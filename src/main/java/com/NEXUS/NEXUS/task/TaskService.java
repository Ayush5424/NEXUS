package com.NEXUS.NEXUS.task;

import com.NEXUS.NEXUS.event.EventStore;
import com.NEXUS.NEXUS.event.EventType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final EventStore eventStore;

    public TaskService(
            TaskRepository taskRepository,
            EventStore eventStore
    ) {
        this.taskRepository = taskRepository;
        this.eventStore = eventStore;
    }

    @Transactional
    public Task acceptTask(String type, String payload) {

        Task task = new Task(
                type,
                payload,
                3
        );

        Task savedTask = taskRepository.save(task);

        eventStore.record(
                savedTask.getId(),
                EventType.TASK_ACCEPTED,
                "Task accepted by NEXUS"
        );

        return savedTask;
    }

    public Optional<Task> getTask(UUID id) {
        return taskRepository.findById(id);
    }
}