package com.placementintelligence.backend.repository;

import com.placementintelligence.backend.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {
}