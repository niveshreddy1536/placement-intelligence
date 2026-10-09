package com.placementintelligence.backend.repository;

import com.placementintelligence.backend.entity.JobDescription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobDescriptionRepository
        extends JpaRepository<JobDescription, Long> {
}