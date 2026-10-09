package com.placementintelligence.backend.controller;

import com.placementintelligence.backend.entity.Student;
import com.placementintelligence.backend.service.AuthService;
import com.placementintelligence.backend.service.SkillGapService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/skill-gaps")
@CrossOrigin(origins = "http://localhost:5173")
public class SkillGapController {

    private final SkillGapService skillGapService;
    private final AuthService authService;

    public SkillGapController(
            SkillGapService skillGapService,
            AuthService authService) {

        this.skillGapService =
                skillGapService;

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
     * ANALYZE SKILL GAPS
     * =========================================================
     */

    @GetMapping(
            "/student/{studentId}/job/{jobDescriptionId}"
    )
    public SkillGapService.SkillGapResult analyzeSkillGap(
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
         * A student can only analyze skill gaps
         * for their own profile.
         */

        if (!authenticatedStudent
                .getId()
                .equals(studentId)) {

            throw new RuntimeException(
                    "You are not authorized to access " +
                            "skill-gap analysis for this student");
        }

        return skillGapService.analyzeSkillGap(
                studentId,
                jobDescriptionId
        );
    }
}