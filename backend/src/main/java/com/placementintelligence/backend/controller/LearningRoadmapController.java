package com.placementintelligence.backend.controller;

import com.placementintelligence.backend.entity.Student;
import com.placementintelligence.backend.service.AuthService;
import com.placementintelligence.backend.service.LearningRoadmapService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/roadmap")
@CrossOrigin(origins = "http://localhost:5173")
public class LearningRoadmapController {

    private final LearningRoadmapService learningRoadmapService;
    private final AuthService authService;

    public LearningRoadmapController(
            LearningRoadmapService learningRoadmapService,
            AuthService authService) {

        this.learningRoadmapService =
                learningRoadmapService;

        this.authService =
                authService;
    }

    /*
     * =========================================================
     * AUTHENTICATE REQUEST
     * =========================================================
     */

    private Student authenticate(
            String authorizationHeader) {

        if (authorizationHeader == null
                || authorizationHeader.isBlank()) {

            throw new RuntimeException(
                    "Authentication required");
        }

        if (!authorizationHeader.startsWith("Bearer ")) {

            throw new RuntimeException(
                    "Invalid authorization header");
        }

        String token =
                authorizationHeader
                        .substring(7)
                        .trim();

        if (token.isBlank()) {

            throw new RuntimeException(
                    "Authentication token is missing");
        }

        return authService.authenticate(token);
    }


    /*
     * =========================================================
     * GENERATE LEARNING ROADMAP
     * =========================================================
     */

    @GetMapping(
            "/student/{studentId}/job/{jobDescriptionId}"
    )
    public LearningRoadmapService.RoadmapResult getRoadmap(
            @PathVariable Long studentId,
            @PathVariable Long jobDescriptionId,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorizationHeader) {

        Student authenticatedStudent =
                authenticate(authorizationHeader);

        /*
         * A student can only generate a roadmap
         * using their own profile.
         */

        if (!authenticatedStudent
                .getId()
                .equals(studentId)) {

            throw new RuntimeException(
                    "You are not authorized to access " +
                            "the learning roadmap for this student");
        }

        return learningRoadmapService.generateRoadmap(
                studentId,
                jobDescriptionId
        );
    }
}