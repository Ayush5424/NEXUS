package com.NEXUS.NEXUS.worker;



import com.NEXUS.NEXUS.task.Task;
import com.NEXUS.NEXUS.task.TaskRepository;
import com.NEXUS.NEXUS.task.TaskStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskDispatcher {

    private final TaskRepository taskRepository;
    private final WorkerService workerService;
    private final int batchSize;

    public TaskDispatcher(
            TaskRepository taskRepository,
            WorkerService workerService,
            @Value("${nexus.dispatch.batch-size:25}") int batchSize
    ) {
        this.taskRepository = taskRepository;
        this.workerService = workerService;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelay = 500)
    public void dispatchTasks() {
        if (workerService.getAvailableWorkers().isEmpty()) {
            return;
        }

        List<Task> acceptedTasks =
                taskRepository.findTop25ByStatusOrderByCreatedAtAsc(
                        TaskStatus.ACCEPTED
                );

        for (Task task : acceptedTasks.stream().limit(batchSize).toList()) {

            int claimed = taskRepository.claimTask(
                    task.getId(),
                    TaskStatus.ACCEPTED,
                    TaskStatus.PROCESSING,
                    LocalDateTime.now()
            );

            if (claimed == 1) {
                workerService.process(task);
            }
        }

        List<Task> retryTasks =
                taskRepository.findTop25ByStatusAndNextAttemptAtBeforeOrderByNextAttemptAtAsc(
                        TaskStatus.RETRYING,
                        LocalDateTime.now()
                );

        for (Task task : retryTasks.stream().limit(batchSize).toList()) {
            LocalDateTime now = LocalDateTime.now();
            int claimed = taskRepository.claimRetryTask(
                    task.getId(),
                    TaskStatus.RETRYING,
                    TaskStatus.PROCESSING,
                    now,
                    now
            );

            if (claimed == 1) {
                workerService.process(task);
            }
        }
    }
}
