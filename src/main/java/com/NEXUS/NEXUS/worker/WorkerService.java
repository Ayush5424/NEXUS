package com.NEXUS.NEXUS.worker;



import com.NEXUS.NEXUS.retry.RetryManager;
import com.NEXUS.NEXUS.task.Task;
import com.NEXUS.NEXUS.task.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkerService implements Worker {

    private final TaskRepository taskRepository;
    private final RetryManager retryManager;

    private final String workerId = "worker-1";

    private WorkerStatus status = WorkerStatus.RUNNING;

    private boolean failMode = false;

    public WorkerService(
            TaskRepository taskRepository,
            RetryManager retryManager
    ) {
        this.taskRepository = taskRepository;
        this.retryManager = retryManager;
    }

    @Override
    @Transactional
    public void process(Task task) {

        if (status != WorkerStatus.RUNNING) {
            return;
        }

        task.markProcessing();
        taskRepository.save(task);

        System.out.println(
                "Worker " + workerId +
                        " processing task " + task.getId()
        );

        try {

            if (failMode) {
                throw new RuntimeException("Simulated worker failure");
            }

            Thread.sleep(1000);

            task.markCompleted();
            taskRepository.save(task);

            System.out.println(
                    "Worker " + workerId +
                            " completed task " + task.getId()
            );

        } catch (Exception e) {

            System.out.println(
                    "Worker " + workerId +
                            " failed task " + task.getId()
            );

            retryManager.handleFailure(task);
        }
    }

    public void enableFailure() {
        failMode = true;
    }

    public void disableFailure() {
        failMode = false;
    }

    public boolean isFailMode() {
        return failMode;
    }

    @Override
    public String getWorkerId() {
        return workerId;
    }

    @Override
    public WorkerStatus getStatus() {
        return status;
    }
}