package com.NEXUS.NEXUS.operator;

import com.NEXUS.NEXUS.task.TaskRepository;
import com.NEXUS.NEXUS.task.TaskStatus;
import com.NEXUS.NEXUS.worker.WorkerManager;
import com.NEXUS.NEXUS.worker.WorkerRepository;
import com.NEXUS.NEXUS.worker.WorkerStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/operator")
public class OperatorController {

    private final TaskRepository taskRepository;
    private final WorkerRepository workerRepository;
    private final WorkerManager workerManager;
    private final FailureSimulator failureSimulator;

    public OperatorController(TaskRepository taskRepository,
                              WorkerRepository workerRepository,
                              WorkerManager workerManager,
                              FailureSimulator failureSimulator) {
        this.taskRepository = taskRepository;
        this.workerRepository = workerRepository;
        this.workerManager = workerManager;
        this.failureSimulator = failureSimulator;
    }

    // R-12: Human-readable diagnostic engine for 90-second assessment[cite: 1]
    @GetMapping("/diagnostics")
    public Map<String, Object> getDiagnostics() {
        long deadLetterCount = taskRepository.countByStatus(TaskStatus.DEAD_LETTER);
        long outOfServiceWorkers = workerRepository.countByStatus(WorkerStatus.OUT_OF_SERVICE);
        long queuedTasks = taskRepository.countByStatus(TaskStatus.ACCEPTED);

        String status = "HEALTHY";
        String summary = "System operating normally. Workers active and queue backlog clear.";

        if (outOfServiceWorkers > 0) {
            status = "CRITICAL";
            summary = String.format("CRITICAL: %d worker(s) hit crash-loop limit and are OUT OF SERVICE.", outOfServiceWorkers);
        } else if (deadLetterCount > 0) {
            status = "WARNING";
            summary = String.format("WARNING: %d task(s) reached Dead Letter Queue (DLQ).", deadLetterCount);
        } else if (queuedTasks > 25) {
            status = "DEGRADED";
            summary = String.format("DEGRADED: High backlog detected (%d items waiting).", queuedTasks);
        }

        return Map.of(
                "status", status,
                "humanReadableSummary", summary,
                "timestamp", LocalDateTime.now()
        );
    }

    // R-15 / Thing 04: Break-It-On-Purpose endpoints[cite: 1]
    @PostMapping("/simulate/kill-worker")
    public Map<String, String> killWorkerMidTask(@RequestParam String workerId) {
        workerManager.stopWorker(workerId);
        return Map.of("message", "Worker " + workerId + " hard-killed mid-execution.");
    }

    @PostMapping("/failures/mode")
    public Map<String, String> setFailureMode(@RequestParam FailureMode mode) {
        failureSimulator.setMode(mode);
        return Map.of("message", "System failure mode set to: " + mode);
    }
}