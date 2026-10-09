package com.placementintelligence.backend.repository;

import com.placementintelligence.backend.entity.Application;
import com.placementintelligence.backend.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository
        extends JpaRepository<Application, Long> {

    List<Application> findByStudent(Student student);
}