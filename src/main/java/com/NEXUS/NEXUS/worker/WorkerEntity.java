package com.NEXUS.NEXUS.worker;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workers")
public class WorkerEntity {

    @Id
    private String workerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkerStatus status;

    @Column(nullable = false)
    private int failureCount;

    @Column(nullable = false)
    private int restartCount;

    @Column(nullable = false)
    private int maxRestarts;

    @Column(nullable = false)
    private LocalDateTime lastHeartbeat;

    protected WorkerEntity() {
    }

    public WorkerEntity(String workerId, int maxRestarts) {
        this.workerId = workerId;
        this.status = WorkerStatus.RUNNING;
        this.failureCount = 0;
        this.restartCount = 0;
        this.maxRestarts = maxRestarts;
        this.lastHeartbeat = LocalDateTime.now();
    }

    public String getWorkerId() {
        return workerId;
    }

    public WorkerStatus getStatus() {
        return status;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public int getRestartCount() {
        return restartCount;
    }

    public int getMaxRestarts() {
        return maxRestarts;
    }

    public LocalDateTime getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void heartbeat() {
        this.lastHeartbeat = LocalDateTime.now();
    }

    public void recordFailure() {
        this.failureCount++;
        this.lastHeartbeat = LocalDateTime.now();
    }

    public void restart() {
        this.restartCount++;
        this.status = WorkerStatus.RUNNING;
        this.lastHeartbeat = LocalDateTime.now();
    }

    public void stop() {
        this.status = WorkerStatus.STOPPED;
    }

    public void markRestarting() {
        this.status = WorkerStatus.RESTARTING;
    }

    public void markOutOfService() {
        this.status = WorkerStatus.OUT_OF_SERVICE;
    }
}
