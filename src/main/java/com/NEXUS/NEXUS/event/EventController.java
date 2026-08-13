package com.NEXUS.NEXUS.event;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/operator/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public List<Event> getEvents() {
        return eventService.getRecentEvents();
    }

    @GetMapping("/{taskId}")
    public List<Event> getTimeline(
            @PathVariable UUID taskId
    ) {
        return eventService.getTaskTimeline(taskId);
    }
}