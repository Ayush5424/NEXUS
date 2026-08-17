package com.NEXUS.NEXUS.event;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType type;

    @Column(name = "task_id")
    private UUID taskId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(columnDefinition = "TEXT")
    private String details;

    public Event() {
    }

    public Event(UUID taskId, EventType type, String details) {
        this.taskId = taskId;
        this.type = type;
        this.createdAt = LocalDateTime.now();
        this.details = details;
    }

    public UUID getId() {
        return id;
    }

    public EventType getType() {
        return type;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getDetails() {
        return details;
    }
}
