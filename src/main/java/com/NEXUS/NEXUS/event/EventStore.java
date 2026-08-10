package com.NEXUS.NEXUS.event;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EventStore {

    private final EventRepository eventRepository;

    public EventStore(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional
    public Event record(
            UUID taskId,
            EventType type,
            String message
    ) {

        Event event = new Event(
                taskId,
                type,
                message
        );

        return eventRepository.save(event);
    }

    public List<Event> getTaskHistory(UUID taskId) {
        return eventRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
    }
}
