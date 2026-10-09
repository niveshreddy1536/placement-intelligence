package com.placementintelligence.backend.repository;

import com.placementintelligence.backend.entity.Resume;
import com.placementintelligence.backend.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    Optional<Resume> findByStudent(Student student);
}