package com.NEXUS.NEXUS.retry;

import com.NEXUS.NEXUS.event.EventStore;
import com.NEXUS.NEXUS.event.EventType;

import com.NEXUS.NEXUS.task.Task;
import com.NEXUS.NEXUS.task.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RetryManager {

    private final TaskRepository taskRepository;
    private final EventStore eventStore;

    public RetryManager(TaskRepository taskRepository, EventStore eventStore) {
        this.taskRepository = taskRepository;
        this.eventStore = eventStore;
    }

    @Transactional
    public void handleFailure(Task task) {

        if (task.getAttemptCount() + 1 >= task.getMaxAttempts()) {
            task.markDeadLetter();
            taskRepository.save(task);
            eventStore.record(
                    task.getId(),
                    EventType.TASK_DEAD_LETTERED,
                    "Retry limit exhausted; task moved to dead letter"
            );

            System.out.println(
                    "Task " + task.getId() + " moved to DEAD_LETTER"
            );

            return;
        }

        int nextAttempt = task.getAttemptCount() + 1;

        long delaySeconds = (long) Math.pow(2, nextAttempt - 1);

        LocalDateTime nextAttemptAt =
                LocalDateTime.now().plusSeconds(delaySeconds);

        task.markRetrying(nextAttemptAt);

        taskRepository.save(task);

        task.markRetrying(nextAttemptAt);
        taskRepository.save(task);

        eventStore.record(
                task.getId(),
                EventType.TASK_RETRY_SCHEDULED,
                "Retry scheduled for " + nextAttemptAt
        );

        System.out.println(
                "Task " + task.getId() +
                        " retry scheduled in " +
                        delaySeconds +
                        " seconds"
        );
    }
}