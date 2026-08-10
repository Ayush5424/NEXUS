package com.NEXUS.NEXUS.task;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<Task> createTask(
            @RequestBody CreateTaskRequest request
    ) {

        Task task = taskService.acceptTask(
                request.type(),
                request.payload()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(task);
    }

    public record CreateTaskRequest(
            String type,
            String payload
    ) {
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTask(@PathVariable UUID id) {

        return taskService.getTask(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}