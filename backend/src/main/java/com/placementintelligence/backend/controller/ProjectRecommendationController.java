package com.placementintelligence.backend.controller;

import com.placementintelligence.backend.entity.Student;
import com.placementintelligence.backend.service.AuthService;
import com.placementintelligence.backend.service.ProjectRecommendationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/project-recommendations")
@CrossOrigin(origins = "http://localhost:5173")
public class ProjectRecommendationController {

    private final ProjectRecommendationService
            projectRecommendationService;

    private final AuthService authService;

    public ProjectRecommendationController(
            ProjectRecommendationService
                    projectRecommendationService,
            AuthService authService) {

        this.projectRecommendationService =
                projectRecommendationService;

        this.authService =
                authService;
    }

    private Student authenticate(
            String authorizationHeader) {

        if (authorizationHeader == null
                || authorizationHeader.isBlank()) {

            throw new RuntimeException(
                    "Authentication required"
            );
        }

        if (!authorizationHeader.startsWith("Bearer ")) {

            throw new RuntimeException(
                    "Invalid authorization header"
            );
        }

        String token =
                authorizationHeader
                        .substring(7)
                        .trim();

        if (token.isBlank()) {

            throw new RuntimeException(
                    "Authentication token is missing"
            );
        }

        return authService.authenticate(token);
    }

    private Student authenticateStudent(
            Long requestedStudentId,
            String authorizationHeader) {

        Student authenticatedStudent =
                authenticate(
                        authorizationHeader
                );

        if (!authenticatedStudent
                .getId()
                .equals(requestedStudentId)) {

            throw new RuntimeException(
                    "You are not authorized to access this student's project recommendations"
            );
        }

        return authenticatedStudent;
    }

    @GetMapping(
            "/student/{studentId}/job/{jobDescriptionId}"
    )
    public ProjectRecommendationService.ProjectRecommendationResult
    recommendProject(
            @PathVariable Long studentId,
            @PathVariable Long jobDescriptionId,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorizationHeader) {

        authenticateStudent(
                studentId,
                authorizationHeader
        );

        return projectRecommendationService
                .recommendProject(
                        studentId,
                        jobDescriptionId
                );
    }
}