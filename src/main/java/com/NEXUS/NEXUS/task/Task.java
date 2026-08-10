package com.NEXUS.NEXUS.task;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Column(nullable = false)
    private int attemptCount;

    @Column(nullable = false)
    private int maxAttempts;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime nextAttemptAt;

    protected Task() {
    }

    public Task(String type, String payload, int maxAttempts) {
        this.type = type;
        this.payload = payload;
        this.maxAttempts = maxAttempts;
        this.status = TaskStatus.ACCEPTED;
        this.attemptCount = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void markProcessing() {
        this.status = TaskStatus.PROCESSING;
        this.updatedAt = LocalDateTime.now();
    }

    public void markCompleted() {
        this.status = TaskStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markRetrying(LocalDateTime nextAttemptAt) {
        this.status = TaskStatus.RETRYING;
        this.attemptCount++;
        this.nextAttemptAt = nextAttemptAt;
        this.updatedAt = LocalDateTime.now();
    }

    public void markDeadLetter() {
        this.status = TaskStatus.DEAD_LETTER;
        this.updatedAt = LocalDateTime.now();
    }
}