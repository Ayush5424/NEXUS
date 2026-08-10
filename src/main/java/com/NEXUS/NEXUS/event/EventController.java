package com.NEXUS.NEXUS.event;


import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tasks")
public class EventController {

    private final EventStore eventStore;

    public EventController(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    @GetMapping("/{taskId}/events")
    public List<Event> getTaskEvents(
            @PathVariable UUID taskId
    ) {
        return eventStore.getTaskHistory(taskId);
    }
}