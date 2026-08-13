package com.NEXUS.NEXUS.release;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/operator/releases")
public class ReleaseController {

    private final ReleaseService releaseService;

    public ReleaseController(ReleaseService releaseService) {
        this.releaseService = releaseService;
    }

    @GetMapping
    public List<Release> getReleases() {
        return releaseService.getAllReleases();
    }

    @PostMapping("/deploy")
    public Release deploy(@RequestParam String version) {
        return releaseService.deployRelease(version);
    }

    // R-06: Undo endpoint
    @PostMapping("/undo")
    public Release undo() {
        return releaseService.rollbackRelease();
    }
}