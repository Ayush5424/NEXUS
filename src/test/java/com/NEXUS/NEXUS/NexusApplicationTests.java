package com.NEXUS.NEXUS;

import com.NEXUS.NEXUS.config.StartupRecovery;
import com.NEXUS.NEXUS.event.EventRepository;
import com.NEXUS.NEXUS.event.EventType;
import com.NEXUS.NEXUS.operator.CacheProbeService;
import com.NEXUS.NEXUS.operator.FailureMode;
import com.NEXUS.NEXUS.operator.FailureSimulator;
import com.NEXUS.NEXUS.operator.OperatorController;
import com.NEXUS.NEXUS.release.ReleaseRepository;
import com.NEXUS.NEXUS.release.ReleaseService;
import com.NEXUS.NEXUS.retry.RetryManager;
import com.NEXUS.NEXUS.task.Task;
import com.NEXUS.NEXUS.task.TaskRepository;
import com.NEXUS.NEXUS.task.TaskService;
import com.NEXUS.NEXUS.task.TaskStatus;
import com.NEXUS.NEXUS.worker.WorkerManager;
import com.NEXUS.NEXUS.worker.WorkerRepository;
import com.NEXUS.NEXUS.worker.WorkerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:nexus-tests;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.task.scheduling.enabled=false"
})
class NexusApplicationTests {

    @Autowired TaskService taskService;
    @Autowired TaskRepository taskRepository;
    @Autowired RetryManager retryManager;
    @Autowired StartupRecovery startupRecovery;
    @Autowired EventRepository eventRepository;
    @Autowired ReleaseService releaseService;
    @Autowired ReleaseRepository releaseRepository;
    @Autowired FailureSimulator failureSimulator;
    @Autowired CacheProbeService cacheProbeService;
    @Autowired OperatorController operatorController;
    @Autowired WorkerManager workerManager;
    @Autowired WorkerRepository workerRepository;

    @BeforeEach
    void cleanDatabase() {
        failureSimulator.setMode(FailureMode.NORMAL);
        eventRepository.deleteAll();
        taskRepository.deleteAll();
        releaseRepository.deleteAll();
        workerRepository.deleteAll();
        workerManager.initialize();
    }

    @Test
    void duplicateWorkReturnsOriginalAcceptedTask() {
        Task first = taskService.acceptTask("EMAIL", "send-a", "same-key");
        Task duplicate = taskService.acceptTask("EMAIL", "send-a-again", "same-key");

        assertThat(duplicate.getId()).isEqualTo(first.getId());
        assertThat(taskRepository.count()).isEqualTo(1);
        assertThat(eventRepository.findByTaskIdOrderByCreatedAtAsc(first.getId()))
                .hasSize(1)
                .first()
                .extracting(event -> event.getType())
                .isEqualTo(EventType.TASK_ACCEPTED);
    }

    @Test
    void retryFailureUsesBackoffAndEventuallyDeadLetters() {
        Task task = taskRepository.save(new Task("EMAIL", "payload", "retry-key", 3));

        retryManager.handleFailure(task);
        Task afterFirstFailure = taskRepository.findById(task.getId()).orElseThrow();
        assertThat(afterFirstFailure.getStatus()).isEqualTo(TaskStatus.RETRYING);
        assertThat(afterFirstFailure.getAttemptCount()).isEqualTo(1);
        assertThat(afterFirstFailure.getNextAttemptAt()).isNotNull();

        retryManager.handleFailure(afterFirstFailure);
        Task afterSecondFailure = taskRepository.findById(task.getId()).orElseThrow();
        assertThat(afterSecondFailure.getStatus()).isEqualTo(TaskStatus.RETRYING);
        assertThat(afterSecondFailure.getAttemptCount()).isEqualTo(2);

        retryManager.handleFailure(afterSecondFailure);
        Task deadLetter = taskRepository.findById(task.getId()).orElseThrow();
        assertThat(deadLetter.getStatus()).isEqualTo(TaskStatus.DEAD_LETTER);
        assertThat(deadLetter.getAttemptCount()).isEqualTo(3);
        assertThat(eventRepository.findByTaskIdOrderByCreatedAtAsc(task.getId()))
                .extracting(event -> event.getType())
                .contains(EventType.TASK_RETRY_SCHEDULED, EventType.TASK_DEAD_LETTERED);
    }

    @Test
    void startupRecoveryRequeuesProcessingWorkAndRecordsIt() throws Exception {
        Task task = taskRepository.save(new Task("EMAIL", "payload", "recover-key", 3));
        task.setStatus(TaskStatus.PROCESSING);
        taskRepository.save(task);

        startupRecovery.run();

        Task recovered = taskRepository.findById(task.getId()).orElseThrow();
        assertThat(recovered.getStatus()).isEqualTo(TaskStatus.ACCEPTED);
        assertThat(eventRepository.findByTaskIdOrderByCreatedAtAsc(task.getId()))
                .extracting(event -> event.getType())
                .contains(EventType.TASK_RECOVERED);
    }

    @Test
    void releaseRollbackRestoresKnownPreviousVersionAndRecordsAction() {
        releaseService.deployRelease("v1.0.0");
        releaseService.deployRelease("v2.0.0-bad");

        assertThat(releaseService.rollbackRelease().getVersion()).isEqualTo("v1.0.0");
        assertThat(eventRepository.findTop100ByOrderByCreatedAtDesc())
                .extracting(event -> event.getType())
                .contains(EventType.RELEASE_DEPLOYED, EventType.RELEASE_ROLLED_BACK);
    }

    @Test
    void cacheDisagreementAndDependencyFailureAreOperatorVisible() {
        failureSimulator.setMode(FailureMode.CACHE_DISAGREE);
        Map<String, Object> disagreement = cacheProbeService.inspect();
        assertThat(disagreement).containsEntry("disagreement", true);
        assertThat(operatorController.getDiagnostics().get("status")).isEqualTo("WARNING");

        failureSimulator.setMode(FailureMode.DEPENDENCY_DOWN);
        Map<String, Object> degraded = cacheProbeService.inspect();
        assertThat(degraded).containsEntry("sourceAvailable", false);
        assertThat(degraded).containsEntry("servedMode", "STALE_MARKED");
        assertThat(operatorController.getDiagnostics().get("status")).isEqualTo("DEGRADED");
    }

    @Test
    void workerRestartBudgetCreatesVisibleOutOfServiceState() {
        workerManager.createWorker("test-worker-budget");

        workerManager.restartWorker("test-worker-budget");
        workerManager.restartWorker("test-worker-budget");
        workerManager.restartWorker("test-worker-budget");
        workerManager.restartWorker("test-worker-budget");

        assertThat(workerRepository.findById("test-worker-budget").orElseThrow().getStatus())
                .isEqualTo(WorkerStatus.OUT_OF_SERVICE);
        assertThat(operatorController.getDiagnostics().get("status")).isEqualTo("CRITICAL");
    }
}
