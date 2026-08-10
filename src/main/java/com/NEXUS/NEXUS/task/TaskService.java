package com.NEXUS.NEXUS.task;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional
    public Task acceptTask(String type, String payload) {

        Task task = new Task(
                type,
                payload,
                3
        );

        return taskRepository.save(task);
    }

    public Optional<Task> getTask(UUID id) {
        return taskRepository.findById(id);
    }
}