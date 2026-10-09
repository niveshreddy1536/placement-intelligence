package com.placementintelligence.backend.repository;

import com.placementintelligence.backend.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository
        extends JpaRepository<Student, Long> {

    Optional<Student> findByEmail(String email);

    Optional<Student> findByEmailIgnoreCase(String email);

    Optional<Student> findByAuthToken(String authToken);
}