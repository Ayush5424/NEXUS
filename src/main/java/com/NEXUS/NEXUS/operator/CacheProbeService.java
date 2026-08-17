package com.NEXUS.NEXUS.operator;

import com.NEXUS.NEXUS.event.EventService;
import com.NEXUS.NEXUS.event.EventType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class CacheProbeService {

    private final FailureSimulator failureSimulator;
    private final EventService eventService;
    private final long maxAgeSeconds;

    private String cachedValue = "catalog-count=42";
    private LocalDateTime cachedAt = LocalDateTime.now();

    public CacheProbeService(
            FailureSimulator failureSimulator,
            EventService eventService,
            @Value("${nexus.cache.max-age-seconds:60}") long maxAgeSeconds
    ) {
        this.failureSimulator = failureSimulator;
        this.eventService = eventService;
        this.maxAgeSeconds = maxAgeSeconds;
    }

    public synchronized Map<String, Object> inspect() {
        String sourceValue = failureSimulator.hasCacheDisagreement()
                ? "catalog-count=43"
                : "catalog-count=42";
        boolean sourceAvailable = !failureSimulator.isDependencyDown();
        long ageSeconds = Duration.between(cachedAt, LocalDateTime.now()).toSeconds();
        boolean stale = ageSeconds > maxAgeSeconds;
        boolean disagreement = sourceAvailable && !cachedValue.equals(sourceValue);

        if (disagreement) {
            eventService.record(
                    EventType.CACHE_DISAGREEMENT,
                    "Cached value " + cachedValue + " disagrees with source value " + sourceValue
            );
        } else {
            eventService.record(
                    EventType.CACHE_VALIDATED,
                    "Cache inspected; ageSeconds=" + ageSeconds +
                            ", sourceAvailable=" + sourceAvailable
            );
        }

        String servedMode;
        if (!sourceAvailable) {
            servedMode = "STALE_MARKED";
        } else if (disagreement) {
            servedMode = "DISAGREEMENT_VISIBLE";
        } else if (stale) {
            servedMode = "STALE_REJECTED";
        } else {
            servedMode = "FRESH";
        }

        return Map.of(
                "cachedValue", cachedValue,
                "sourceValue", sourceAvailable ? sourceValue : "UNAVAILABLE",
                "sourceAvailable", sourceAvailable,
                "cachedAt", cachedAt,
                "ageSeconds", ageSeconds,
                "maxAgeSeconds", maxAgeSeconds,
                "stale", stale,
                "disagreement", disagreement,
                "servedMode", servedMode,
                "whatCouldChangeIt", "POST /operator/cache/refresh or set failure mode NORMAL"
        );
    }

    public synchronized Map<String, Object> refresh() {
        if (failureSimulator.isDependencyDown()) {
            eventService.record(
                    EventType.DEPENDENCY_DEGRADED,
                    "Cache refresh skipped because simulated dependency is unavailable"
            );
            return inspect();
        }

        cachedValue = failureSimulator.hasCacheDisagreement()
                ? "catalog-count=43"
                : "catalog-count=42";
        cachedAt = LocalDateTime.now();
        eventService.record(
                EventType.CACHE_VALIDATED,
                "Cache refreshed from available source"
        );
        return inspect();
    }
}
