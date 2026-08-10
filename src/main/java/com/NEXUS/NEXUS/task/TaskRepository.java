package com.NEXUS.NEXUS.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    Optional<Task> findByIdempotencyKey(String idempotencyKey);

    List<Task> findByStatus(TaskStatus status);

    List<Task> findByStatusAndNextAttemptAtBefore(
            TaskStatus status,
            LocalDateTime time
    );

    long countByStatus(TaskStatus status);
}