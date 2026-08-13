package com.NEXUS.NEXUS.config;

import com.NEXUS.NEXUS.event.EventService;
import com.NEXUS.NEXUS.event.EventType;
import com.NEXUS.NEXUS.task.Task;
import com.NEXUS.NEXUS.task.TaskRepository;
import com.NEXUS.NEXUS.task.TaskStatus;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class StartupRecovery implements CommandLineRunner {

    private final TaskRepository taskRepository;
    private final EventService eventService;

    public StartupRecovery(TaskRepository taskRepository, EventService eventService) {
        this.taskRepository = taskRepository;
        this.eventService = eventService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Recover orphan tasks left in PROCESSING when platform crashed/restarted
        List<Task> pendingTasks = taskRepository.findByStatus(TaskStatus.PROCESSING);

        if (!pendingTasks.isEmpty()) {
            for (Task task : pendingTasks) {
                task.setStatus(TaskStatus.ACCEPTED); // Re-queue task
                taskRepository.save(task);
            }

            // Fixed: Replaced eventService.logEvent with eventService.record
            eventService.record(
                    EventType.SYSTEM_ALERT,
                    "STARTUP RECOVERY: Reset " + pendingTasks.size() + " orphaned processing tasks back to ACCEPTED."
            );
        }
    }
}