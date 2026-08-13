package com.NEXUS.NEXUS.worker;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkerRepository extends JpaRepository<WorkerEntity, String> {

    // Counts workers by their current lifecycle state (e.g., OUT_OF_SERVICE, RUNNING)
    long countByStatus(WorkerStatus status);

    // Finds all workers currently in a given status (e.g., RUNNING)
    List<WorkerEntity> findByStatus(WorkerStatus status);

    // Helps retrieve a worker by its assigned instance ID safely
    Optional<WorkerEntity> findByWorkerId(String workerId);
}