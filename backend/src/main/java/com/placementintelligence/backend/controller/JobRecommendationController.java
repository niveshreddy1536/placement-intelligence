package com.placementintelligence.backend.controller;

import com.placementintelligence.backend.entity.Student;
import com.placementintelligence.backend.service.AuthService;
import com.placementintelligence.backend.service.JobRecommendationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/job-recommendations")
@CrossOrigin(origins = "http://localhost:5173")
public class JobRecommendationController {

    private final JobRecommendationService jobRecommendationService;
    private final AuthService authService;

    public JobRecommendationController(
            JobRecommendationService jobRecommendationService,
            AuthService authService) {

        this.jobRecommendationService =
                jobRecommendationService;

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
     * GET JOB RECOMMENDATIONS FOR LOGGED-IN STUDENT
     * =========================================================
     *
     * Example:
     *
     * GET
     * /api/job-recommendations/student/2
     *
     * Header:
     *
     * Authorization: Bearer <token>
     *
     * The student ID in the URL MUST belong to
     * the authenticated student.
     * =========================================================
     */

    @GetMapping("/student/{studentId}")
    public Object recommendJobs(
            @PathVariable Long studentId,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorizationHeader) {

        Student authenticatedStudent =
                authenticate(authorizationHeader);

        /*
         * Prevent one student from requesting
         * another student's recommendations.
         */

        if (!authenticatedStudent
                .getId()
                .equals(studentId)) {

            throw new RuntimeException(
                    "You are not authorized to access " +
                            "recommendations for this student");
        }

        /*
         * Generate recommendations only for
         * the authenticated student.
         */

        return jobRecommendationService
                .recommendJobs(studentId);
    }
}