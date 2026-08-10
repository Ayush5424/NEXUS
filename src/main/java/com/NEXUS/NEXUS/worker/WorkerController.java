package com.NEXUS.NEXUS.worker;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workers")
public class WorkerController {

    private final WorkerService workerService;
    private final WorkerManager workerManager;

    public WorkerController(
            WorkerService workerService,
            WorkerManager workerManager
    ) {
        this.workerService = workerService;
        this.workerManager = workerManager;
    }

    @GetMapping
    public List<WorkerEntity> getWorkers() {
        return workerManager.getWorkers();
    }

    @GetMapping("/{workerId}")
    public WorkerEntity getWorker(
            @PathVariable String workerId
    ) {
        return workerManager.getWorker(workerId);
    }

    @GetMapping("/status")
    public WorkerStatus getStatus() {
        return workerService.getStatus();
    }

    @PostMapping("/{workerId}/stop")
    public String stopWorker(
            @PathVariable String workerId
    ) {
        workerManager.stopWorker(workerId);
        return "Worker " + workerId + " stopped";
    }

    @PostMapping("/{workerId}/restart")
    public String restartWorker(
            @PathVariable String workerId
    ) {
        workerManager.restartWorker(workerId);
        return "Worker " + workerId + " restart requested";
    }

    @PostMapping("/failure/enable")
    public String enableFailure() {
        workerService.enableFailure();
        return "Worker failure mode enabled";
    }

    @PostMapping("/failure/disable")
    public String disableFailure() {
        workerService.disableFailure();
        return "Worker failure mode disabled";
    }
}