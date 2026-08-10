package com.NEXUS.NEXUS.worker;



import com.NEXUS.NEXUS.retry.RetryManager;
import com.NEXUS.NEXUS.task.Task;
import com.NEXUS.NEXUS.task.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.NEXUS.NEXUS.event.EventStore;
import com.NEXUS.NEXUS.event.EventType;

@Service
public class WorkerService implements Worker {

    private final TaskRepository taskRepository;
    private final RetryManager retryManager;
    private final EventStore eventStore;
    private final WorkerManager workerManager;

    private final String workerId = "worker-1";

    private boolean failMode = false;

    public WorkerService(
            TaskRepository taskRepository,
            RetryManager retryManager,
            EventStore eventStore,
            WorkerManager workerManager
    ) {
        this.taskRepository = taskRepository;
        this.retryManager = retryManager;
        this.eventStore = eventStore;
        this.workerManager = workerManager;
    }

    @Override
    @Transactional
    public void process(Task task) {

        WorkerEntity worker = workerManager.getWorker(workerId);

        if (worker.getStatus() != WorkerStatus.RUNNING) {
            return;
        }

        task.markProcessing();
        taskRepository.save(task);

        eventStore.record(
                task.getId(),
                EventType.TASK_STARTED,
                "Worker " + workerId + " started processing task"
        );

        try {

            if (failMode) {
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
                    "Worker " + workerId + " completed task"
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
        return workerManager
                .getWorker(workerId)
                .getStatus();
    }
}