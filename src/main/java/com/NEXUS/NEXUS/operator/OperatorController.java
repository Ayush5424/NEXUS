package com.NEXUS.NEXUS.operator;

import com.NEXUS.NEXUS.event.Event;
import com.NEXUS.NEXUS.event.EventRepository;
import com.NEXUS.NEXUS.task.Task;
import com.NEXUS.NEXUS.task.TaskRepository;
import com.NEXUS.NEXUS.task.TaskStatus;
import com.NEXUS.NEXUS.worker.WorkerEntity;
import com.NEXUS.NEXUS.worker.WorkerManager;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/operator")
public class OperatorController {

    private final TaskRepository taskRepository;
    private final EventRepository eventRepository;
    private final WorkerManager workerManager;
    private final FailureSimulator failureSimulator;

    public OperatorController(
            TaskRepository taskRepository,
            EventRepository eventRepository,
            WorkerManager workerManager,
            FailureSimulator failureSimulator
    ) {
        this.taskRepository = taskRepository;
        this.eventRepository = eventRepository;
        this.workerManager = workerManager;
        this.failureSimulator = failureSimulator;
    }

    @GetMapping("/status")
    public OperatorStatus getStatus() {

        return new OperatorStatus(
                taskRepository.count(),
                taskRepository.countByStatus(TaskStatus.ACCEPTED),
                taskRepository.countByStatus(TaskStatus.PROCESSING),
                taskRepository.countByStatus(TaskStatus.COMPLETED),
                taskRepository.countByStatus(TaskStatus.RETRYING),
                taskRepository.countByStatus(TaskStatus.DEAD_LETTER),
                failureSimulator.getMode(),
                workerManager.getWorkers()
        );
    }

    @GetMapping("/tasks")
    public List<Task> getTasks() {
        return taskRepository.findAll();
    }

    @GetMapping("/tasks/{taskId}")
    public Task getTask(@PathVariable UUID taskId) {

        return taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Task not found: " + taskId
                        )
                );
    }

    @GetMapping("/tasks/{taskId}/events")
    public List<Event> getTaskEvents(
            @PathVariable UUID taskId
    ) {

        return eventRepository
                .findByTaskIdOrderByCreatedAtAsc(taskId);
    }

    @GetMapping("/workers")
    public List<WorkerEntity> getWorkers() {
        return workerManager.getWorkers();
    }

    @GetMapping("/workers/{workerId}")
    public WorkerEntity getWorker(
            @PathVariable String workerId
    ) {
        return workerManager.getWorker(workerId);
    }

    @PostMapping("/workers/{workerId}/stop")
    public String stopWorker(
            @PathVariable String workerId
    ) {

        workerManager.stopWorker(workerId);

        return "Worker " + workerId + " stopped";
    }

    @PostMapping("/workers/{workerId}/restart")
    public String restartWorker(
            @PathVariable String workerId
    ) {

        workerManager.restartWorker(workerId);

        return "Worker " + workerId + " restart requested";
    }

    @PostMapping("/failures/mode")
    public FailureMode setFailureMode(
            @RequestParam FailureMode mode
    ) {

        failureSimulator.setMode(mode);

        return failureSimulator.getMode();
    }

    @GetMapping("/failures/mode")
    public FailureMode getFailureMode() {
        return failureSimulator.getMode();
    }

    public record OperatorStatus(
            long totalTasks,
            long acceptedTasks,
            long processingTasks,
            long completedTasks,
            long retryingTasks,
            long deadLetterTasks,
            FailureMode failureMode,
            List<WorkerEntity> workers
    ) {
    }
}