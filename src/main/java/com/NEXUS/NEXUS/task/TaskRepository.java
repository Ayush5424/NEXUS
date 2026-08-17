package com.NEXUS.NEXUS.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    Optional<Task> findByIdempotencyKey(String idempotencyKey);

    List<Task> findByStatus(TaskStatus status);

    List<Task> findTop25ByStatusOrderByCreatedAtAsc(TaskStatus status);

    List<Task> findTop25ByStatusAndNextAttemptAtBeforeOrderByNextAttemptAtAsc(
            TaskStatus status,
            LocalDateTime time
    );

    List<Task> findByStatusAndNextAttemptAtBefore(
            TaskStatus status,
            LocalDateTime time
    );

    long countByStatus(TaskStatus status);

    @Transactional
    @Modifying
    @Query("""
            UPDATE Task t
            SET t.status = :processing,
                t.updatedAt = :updatedAt
            WHERE t.id = :taskId
              AND t.status = :accepted
            """)
    int claimTask(
            @Param("taskId") UUID taskId,
            @Param("accepted") TaskStatus accepted,
            @Param("processing") TaskStatus processing,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Transactional
    @Modifying
    @Query("""
            UPDATE Task t
            SET t.status = :processing,
                t.updatedAt = :updatedAt
            WHERE t.id = :taskId
              AND t.status = :retrying
              AND t.nextAttemptAt <= :now
            """)
    int claimRetryTask(
            @Param("taskId") UUID taskId,
            @Param("retrying") TaskStatus retrying,
            @Param("processing") TaskStatus processing,
            @Param("now") LocalDateTime now,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    Optional<Task> findFirstByStatusOrderByCreatedAtAsc(TaskStatus status);
}
