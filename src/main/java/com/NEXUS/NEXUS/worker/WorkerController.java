package com.NEXUS.NEXUS.worker;

import com.NEXUS.NEXUS.event.EventStore;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/workers")
public class WorkerController {

    private final WorkerService workerService;



    public WorkerController(WorkerService workerService) {
        this.workerService = workerService;
    }

    @GetMapping("/status")
    public WorkerStatus getStatus() {
        return workerService.getStatus();
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