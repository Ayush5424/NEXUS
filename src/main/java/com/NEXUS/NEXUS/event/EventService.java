package com.NEXUS.NEXUS.event;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public Event record(
            UUID taskId,
            EventType type,
            String details
    ) {
        return eventRepository.save(
                new Event(taskId, type, details)
        );
    }

    // --- ADD THIS OVERLOADED METHOD FOR SYSTEM/RELEASE EVENTS ---
    public Event record(
            EventType type,
            String details
    ) {
        return eventRepository.save(
                new Event(null, type, details)
        );
    }

    public List<Event> getRecentEvents() {
        return eventRepository.findTop100ByOrderByCreatedAtDesc();
    }

    public List<Event> getTaskTimeline(UUID taskId) {
        return eventRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
    }
}