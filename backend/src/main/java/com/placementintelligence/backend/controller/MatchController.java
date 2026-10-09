package com.placementintelligence.backend.controller;

import com.placementintelligence.backend.entity.JobDescription;
import com.placementintelligence.backend.entity.Student;
import com.placementintelligence.backend.repository.JobDescriptionRepository;
import com.placementintelligence.backend.service.AuthService;
import com.placementintelligence.backend.service.PlacementMatchService;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/matching")
@CrossOrigin(origins = "http://localhost:5173")
public class MatchController {

    private final PlacementMatchService placementMatchService;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final AuthService authService;

    public MatchController(
            PlacementMatchService placementMatchService,
            JobDescriptionRepository jobDescriptionRepository,
            AuthService authService) {

        this.placementMatchService =
                placementMatchService;

        this.jobDescriptionRepository =
                jobDescriptionRepository;

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


    /*
     * =========================================================
     * VERIFY STUDENT OWNERSHIP
     * =========================================================
     *
     * Makes sure the studentId in the URL belongs to the
     * currently logged-in user.
     *
     * =========================================================
     */

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
                    "You are not authorized to access this student's matching data"
            );
        }

        return authenticatedStudent;
    }


    /*
     * =========================================================
     * MATCH STUDENT WITH ONE JOB
     * =========================================================
     *
     * Example:
     *
     * GET /api/matching/student/2/job/4
     *
     * Authorization:
     * Bearer <token>
     *
     * =========================================================
     */

    @GetMapping(
            "/student/{studentId}/job/{jobDescriptionId}"
    )
    public PlacementMatchService.MatchResult match(
            @PathVariable Long studentId,
            @PathVariable Long jobDescriptionId,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorizationHeader) {

        /*
         * Verify that the logged-in user owns this student ID.
         */
        authenticateStudent(
                studentId,
                authorizationHeader
        );

        /*
         * Only after authentication succeeds,
         * perform the actual matching.
         */
        return placementMatchService.matchStudentWithJob(
                studentId,
                jobDescriptionId
        );
    }


    /*
     * =========================================================
     * MATCH STUDENT WITH ALL JOBS
     * =========================================================
     *
     * Compares the authenticated student's profile/resume
     * against every JobDescription stored in the database.
     *
     * Results are sorted from highest match percentage
     * to lowest.
     *
     * Example:
     *
     * GET /api/matching/student/2/all
     *
     * Authorization:
     * Bearer <token>
     *
     * =========================================================
     */

    @GetMapping(
            "/student/{studentId}/all"
    )
    public List<PlacementMatchService.MatchResult> matchAllJobs(
            @PathVariable Long studentId,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorizationHeader) {

        /*
         * Verify student ownership before accessing
         * any matching information.
         */
        authenticateStudent(
                studentId,
                authorizationHeader
        );

        List<JobDescription> jobDescriptions =
                jobDescriptionRepository.findAll();

        return jobDescriptions.stream()
                .map(jobDescription ->
                        placementMatchService.matchStudentWithJob(
                                studentId,
                                jobDescription.getId()
                        )
                )
                .sorted(
                        Comparator.comparingDouble(
                                PlacementMatchService.MatchResult
                                        ::getMatchPercentage
                        ).reversed()
                )
                .toList();
    }
}