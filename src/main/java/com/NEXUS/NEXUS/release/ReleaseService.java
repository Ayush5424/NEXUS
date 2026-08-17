package com.NEXUS.NEXUS.release;

import com.NEXUS.NEXUS.event.EventService;
import com.NEXUS.NEXUS.event.EventType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReleaseService {

    private final ReleaseRepository releaseRepository;
    private final EventService eventService;

    public ReleaseService(ReleaseRepository releaseRepository, EventService eventService) {
        this.releaseRepository = releaseRepository;
        this.eventService = eventService;
    }

    @Transactional
    public Release deployRelease(String version) {
        String rollbackVersion = releaseRepository.findFirstByStatusOrderByIdDesc("ACTIVE")
                .map(Release::getVersion)
                .orElse("v1.0.0-INITIAL");

        releaseRepository.findFirstByStatusOrderByIdDesc("ACTIVE")
                .ifPresent(active -> {
                    active.setStatus("SUPERSEDED");
                    releaseRepository.save(active);
                });

        Release newRelease = releaseRepository.save(new Release(version, "ACTIVE", rollbackVersion));

        eventService.record(
                EventType.RELEASE_DEPLOYED,
                "Deployed release version " + version +
                        " with rollback target " + rollbackVersion
        );

        return newRelease;
    }

    @Transactional
    public Release rollbackRelease() {
        Release active = releaseRepository.findFirstByStatusOrderByIdDesc("ACTIVE")
                .orElseThrow(() -> new IllegalStateException("No active release to roll back."));

        active.setStatus("ROLLED_BACK");
        releaseRepository.save(active);

        Release targetRelease = releaseRepository.findFirstByStatusOrderByIdDesc("SUPERSEDED")
                .orElseGet(() -> new Release(active.getRollbackVersion(), "ACTIVE", "v1.0.0-INITIAL"));

        targetRelease.setStatus("ACTIVE");
        Release restored = releaseRepository.save(targetRelease);

        eventService.record(
                EventType.RELEASE_ROLLED_BACK,
                "RELEASE ROLLED BACK: Reverted from " + active.getVersion() + " to " + restored.getVersion()
        );

        return restored;
    }

    public List<Release> getAllReleases() {
        return releaseRepository.findAll();
    }
}
