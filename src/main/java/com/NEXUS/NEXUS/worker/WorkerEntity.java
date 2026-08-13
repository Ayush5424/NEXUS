package com.NEXUS.NEXUS.worker;

import jakarta.persistence.*;

import java.time.LocalDateTime;

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
        this.maxRestarts = maxRestarts;
        this.status = WorkerStatus.RUNNING;
        this.failureCount = 0;
        this.restartCount = 0;
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
        lastHeartbeat = LocalDateTime.now();
    }

    public void recordFailure() {
        failureCount++;
        lastHeartbeat = LocalDateTime.now();
    }

    public void restart() {
        restartCount++;
        status = WorkerStatus.RUNNING;
        lastHeartbeat = LocalDateTime.now();
    }

    public void stop() {
        status = WorkerStatus.STOPPED;
    }

    public void markRestarting() {
        status = WorkerStatus.RESTARTING;
    }

    public void markOutOfService() {
        status = WorkerStatus.OUT_OF_SERVICE;
    }
}