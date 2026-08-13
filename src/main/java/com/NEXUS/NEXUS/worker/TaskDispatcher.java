package com.NEXUS.NEXUS.worker;



import com.NEXUS.NEXUS.task.Task;
import com.NEXUS.NEXUS.task.TaskRepository;
import com.NEXUS.NEXUS.task.TaskStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskDispatcher {

    private final TaskRepository taskRepository;
    private final WorkerService workerService;

    public TaskDispatcher(
            TaskRepository taskRepository,
            WorkerService workerService
    ) {
        this.taskRepository = taskRepository;
        this.workerService = workerService;
    }

    @Scheduled(fixedDelay = 500)
    public void dispatchTasks() {
        System.out.println("NEXUS DISPATCHER RUNNING");

        if (workerService.getAvailableWorkers().isEmpty()) {
            return;
        }

        List<Task> acceptedTasks =
                taskRepository.findByStatus(
                        TaskStatus.ACCEPTED
                );

        for (Task task : acceptedTasks) {

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
                taskRepository.findByStatusAndNextAttemptAtBefore(
                        TaskStatus.RETRYING,
                        LocalDateTime.now()
                );

        for (Task task : retryTasks) {
            workerService.process(task);
        }
    }
}