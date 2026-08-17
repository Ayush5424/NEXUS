package com.NEXUS.NEXUS.operator;

import com.NEXUS.NEXUS.task.TaskRepository;
import com.NEXUS.NEXUS.task.TaskStatus;
import com.NEXUS.NEXUS.event.EventService;
import com.NEXUS.NEXUS.event.EventType;
import com.NEXUS.NEXUS.release.ReleaseRepository;
import com.NEXUS.NEXUS.worker.WorkerManager;
import com.NEXUS.NEXUS.worker.WorkerRepository;
import com.NEXUS.NEXUS.worker.WorkerStatus;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/operator")
public class OperatorController {

    private final TaskRepository taskRepository;
    private final WorkerRepository workerRepository;
    private final WorkerManager workerManager;
    private final FailureSimulator failureSimulator;
    private final ReleaseRepository releaseRepository;
    private final EventService eventService;
    private final CacheProbeService cacheProbeService;

    public OperatorController(TaskRepository taskRepository,
                              WorkerRepository workerRepository,
                              WorkerManager workerManager,
                              FailureSimulator failureSimulator,
                              ReleaseRepository releaseRepository,
                              EventService eventService,
                              CacheProbeService cacheProbeService) {
        this.taskRepository = taskRepository;
        this.workerRepository = workerRepository;
        this.workerManager = workerManager;
        this.failureSimulator = failureSimulator;
        this.releaseRepository = releaseRepository;
        this.eventService = eventService;
        this.cacheProbeService = cacheProbeService;
    }

    @GetMapping("/diagnostics")
    public Map<String, Object> getDiagnostics() {
        long deadLetterCount = taskRepository.countByStatus(TaskStatus.DEAD_LETTER);
        long outOfServiceWorkers = workerRepository.countByStatus(WorkerStatus.OUT_OF_SERVICE);
        long queuedTasks = taskRepository.countByStatus(TaskStatus.ACCEPTED);
        long retryingTasks = taskRepository.countByStatus(TaskStatus.RETRYING);
        long processingTasks = taskRepository.countByStatus(TaskStatus.PROCESSING);
        Map<String, Object> cache = cacheProbeService.inspect();

        String status = "HEALTHY";
        String summary = "System operating normally. Workers active and queue backlog clear.";
        String abnormal = "No abnormal platform belief currently recorded.";

        if (outOfServiceWorkers > 0) {
            status = "CRITICAL";
            summary = String.format("CRITICAL: %d worker(s) hit crash-loop limit and are OUT OF SERVICE.", outOfServiceWorkers);
            abnormal = "Worker restart budget exhausted.";
        } else if (deadLetterCount > 0) {
            status = "WARNING";
            summary = String.format("WARNING: %d task(s) reached Dead Letter Queue (DLQ).", deadLetterCount);
            abnormal = "Accepted work reached visible terminal failure.";
        } else if ((boolean) cache.get("disagreement")) {
            status = "WARNING";
            summary = "WARNING: cached platform belief disagrees with source value.";
            abnormal = "Cache disagreement detected and exposed.";
        } else if (!(boolean) cache.get("sourceAvailable")) {
            status = "DEGRADED";
            summary = "DEGRADED: dependency unavailable; stale cached value is explicitly marked.";
            abnormal = "Dependency unavailable.";
        } else if (queuedTasks > 25) {
            status = "DEGRADED";
            summary = String.format("DEGRADED: High backlog detected (%d items waiting).", queuedTasks);
            abnormal = "Backlog exceeds dispatch batch; recovery is being throttled.";
        }

        LocalDateTime oldestQueuedAt = taskRepository.findFirstByStatusOrderByCreatedAtAsc(TaskStatus.ACCEPTED)
                .map(task -> task.getCreatedAt())
                .orElse(null);
        Long oldestQueuedAgeSeconds = oldestQueuedAt == null
                ? null
                : Duration.between(oldestQueuedAt, LocalDateTime.now()).toSeconds();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", status);
        response.put("humanReadableSummary", summary);
        response.put("abnormalBelief", abnormal);
        response.put("queuedTasks", queuedTasks);
        response.put("processingTasks", processingTasks);
        response.put("retryingTasks", retryingTasks);
        response.put("deadLetterTasks", deadLetterCount);
        response.put("outOfServiceWorkers", outOfServiceWorkers);
        response.put("oldestQueuedAt", oldestQueuedAt == null ? "NONE" : oldestQueuedAt);
        response.put("oldestQueuedAgeSeconds", oldestQueuedAgeSeconds == null ? "NONE" : oldestQueuedAgeSeconds);
        response.put("failureMode", failureSimulator.getMode());
        response.put("activeRelease", releaseRepository.findFirstByStatusOrderByIdDesc("ACTIVE")
                .map(release -> release.getVersion())
                .orElse("v1.0.0-INITIAL"));
        response.put("cache", cache);
        response.put("timestamp", LocalDateTime.now());
        return response;
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        return Map.of(
                "totalTasks", taskRepository.count(),
                "acceptedTasks", taskRepository.countByStatus(TaskStatus.ACCEPTED),
                "processingTasks", taskRepository.countByStatus(TaskStatus.PROCESSING),
                "completedTasks", taskRepository.countByStatus(TaskStatus.COMPLETED),
                "retryingTasks", taskRepository.countByStatus(TaskStatus.RETRYING),
                "deadLetterTasks", taskRepository.countByStatus(TaskStatus.DEAD_LETTER),
                "failureMode", failureSimulator.getMode(),
                "workers", workerManager.getWorkers(),
                "activeRelease", releaseRepository.findFirstByStatusOrderByIdDesc("ACTIVE")
                        .map(release -> release.getVersion())
                        .orElse("v1.0.0-INITIAL")
        );
    }

    @GetMapping("/tasks")
    public Object getTasks() {
        return taskRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @GetMapping("/workers")
    public Object getWorkers() {
        return workerManager.getWorkers();
    }

    @PostMapping("/workers")
    public Object createWorker(@RequestParam String workerId) {
        eventService.record(EventType.OPERATOR_ACTION, "Operator created worker " + workerId);
        return workerManager.createWorker(workerId);
    }

    @DeleteMapping("/workers/{workerId}")
    public Map<String, String> deleteWorker(@PathVariable String workerId) {
        workerManager.deleteWorker(workerId);
        eventService.record(EventType.OPERATOR_ACTION, "Operator deleted worker " + workerId);
        return Map.of("message", "Worker " + workerId + " deleted");
    }

    @PostMapping("/simulate/kill-worker")
    public Map<String, String> killWorkerMidTask(@RequestParam String workerId) {
        workerManager.stopWorker(workerId);
        return Map.of("message", "Worker " + workerId + " hard-killed mid-execution.");
    }

    @PostMapping("/failures/mode")
    public Map<String, String> setFailureMode(@RequestParam FailureMode mode) {
        failureSimulator.setMode(mode);
        eventService.record(EventType.OPERATOR_ACTION, "Operator set failure mode to " + mode);
        return Map.of("message", "System failure mode set to: " + mode);
    }

    @GetMapping("/cache")
    public Map<String, Object> inspectCache() {
        return cacheProbeService.inspect();
    }

    @PostMapping("/cache/refresh")
    public Map<String, Object> refreshCache() {
        eventService.record(EventType.OPERATOR_ACTION, "Operator requested cache refresh");
        return cacheProbeService.refresh();
    }
}
