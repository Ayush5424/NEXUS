package com.NEXUS.NEXUS.worker;


import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkerRepository extends JpaRepository<WorkerEntity, String> {
}
