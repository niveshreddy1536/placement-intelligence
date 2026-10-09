package com.placementintelligence.backend.repository;

import com.placementintelligence.backend.entity.Resume;
import com.placementintelligence.backend.entity.ResumeAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResumeAnalysisRepository extends JpaRepository<ResumeAnalysis, Long> {

    Optional<ResumeAnalysis> findByResume(Resume resume);
}