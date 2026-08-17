package com.NEXUS.NEXUS.release;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "releases")
public class Release {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String version;

    @Column(nullable = false)
    private String status; // "ACTIVE", "ROLLED_BACK", "SUPERSEDED"

    @Column
    private String rollbackVersion;

    private LocalDateTime deployedAt;

    public Release() {
        this.deployedAt = LocalDateTime.now();
    }

    public Release(String version, String status, String rollbackVersion) {
        this();
        this.version = version;
        this.status = status;
        this.rollbackVersion = rollbackVersion;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRollbackVersion() { return rollbackVersion; }
    public void setRollbackVersion(String rollbackVersion) { this.rollbackVersion = rollbackVersion; }
    public LocalDateTime getDeployedAt() { return deployedAt; }
    public void setDeployedAt(LocalDateTime deployedAt) { this.deployedAt = deployedAt; }
}
