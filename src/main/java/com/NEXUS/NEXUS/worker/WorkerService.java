package com.NEXUS.NEXUS.worker;

import com.NEXUS.NEXUS.event.EventStore;
import com.NEXUS.NEXUS.event.EventType;
import com.NEXUS.NEXUS.operator.FailureSimulator;
import com.NEXUS.NEXUS.retry.RetryManager;
import com.NEXUS.NEXUS.task.Task;
import com.NEXUS.NEXUS.task.TaskRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkerService implements Worker {

    private final TaskRepository taskRepository;
    private final RetryManager retryManager;
    private final EventStore eventStore;
    private final WorkerManager workerManager;
    private final FailureSimulator failureSimulator;

    public WorkerService(
            TaskRepository taskRepository,
            RetryManager retryManager,
            EventStore eventStore,
            WorkerManager workerManager,
            FailureSimulator failureSimulator
    ) {
        this.taskRepository = taskRepository;
        this.retryManager = retryManager;
        this.eventStore = eventStore;
        this.workerManager = workerManager;
        this.failureSimulator = failureSimulator;
    }

    @Async("workerExecutor")
    @Override
    public void process(Task task) {

        WorkerEntity worker = selectWorker();

        if (worker == null) {
            return;
        }

        String workerId = worker.getWorkerId();

        try {

            eventStore.record(
                    task.getId(),
                    EventType.TASK_STARTED,
                    "Worker " + workerId +
                            " started processing task"
            );

            if (failureSimulator.shouldFail()) {
                throw new RuntimeException(
                        "Simulated worker failure"
                );
            }

            Thread.sleep(1000);

            task.markCompleted();
            taskRepository.save(task);

            workerManager.heartbeat(workerId);

            eventStore.record(
                    task.getId(),
                    EventType.TASK_COMPLETED,
                    "Worker " + workerId +
                            " completed task"
            );

            System.out.println(
                    workerId +
                            " completed task " +
                            task.getId()
            );

        } catch (Exception e) {

            workerManager.recordFailure(workerId);

            eventStore.record(
                    task.getId(),
                    EventType.TASK_FAILED,
                    "Worker " + workerId +
                            " failed task: " +
                            e.getMessage()
            );

            retryManager.handleFailure(task);
        }
    }

    private WorkerEntity selectWorker() {

        return workerManager.getWorkers()
                .stream()
                .filter(worker ->
                        worker.getStatus() ==
                                WorkerStatus.RUNNING)
                .findFirst()
                .orElse(null);
    }

    public List<WorkerEntity> getAvailableWorkers() {

        return workerManager.getWorkers()
                .stream()
                .filter(worker ->
                        worker.getStatus() ==
                                WorkerStatus.RUNNING)
                .toList();
    }

    @Scheduled(fixedRate = 3000)
    public void sendHeartbeats() {

        workerManager.getWorkers()
                .stream()
                .filter(worker ->
                        worker.getStatus() ==
                                WorkerStatus.RUNNING)
                .forEach(worker ->
                        workerManager.heartbeat(
                                worker.getWorkerId()
                        )
                );
    }

    // Kept for compatibility with WorkerController
    public void enableFailure() {
        failureSimulator.setMode(
                com.NEXUS.NEXUS.operator.FailureMode.FAIL
        );
    }

    // Kept for compatibility with WorkerController
    public void disableFailure() {
        failureSimulator.setMode(
                com.NEXUS.NEXUS.operator.FailureMode.NORMAL
        );
    }

    @Override
    public String getWorkerId() {
        return "worker-pool";
    }

    @Override
    public WorkerStatus getStatus() {

        return getAvailableWorkers().isEmpty()
                ? WorkerStatus.OUT_OF_SERVICE
                : WorkerStatus.RUNNING;
    }
}