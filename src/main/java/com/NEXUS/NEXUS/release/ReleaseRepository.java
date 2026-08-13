package com.NEXUS.NEXUS.release;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReleaseRepository extends JpaRepository<Release, Long> {
    Optional<Release> findFirstByStatusOrderByIdDesc(String status);
}