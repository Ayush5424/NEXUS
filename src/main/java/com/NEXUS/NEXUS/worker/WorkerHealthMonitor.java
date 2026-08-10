package com.NEXUS.NEXUS.worker;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class WorkerHealthMonitor {

    private final WorkerManager workerManager;

    public WorkerHealthMonitor(WorkerManager workerManager) {
        this.workerManager = workerManager;
    }

    @Scheduled(fixedDelay = 5000)
    public void monitorWorkers() {

        workerManager.getWorkers()
                .forEach(worker ->
                        workerManager.checkWorkerHealth(
                                worker.getWorkerId()
                        )
                );
    }
}
