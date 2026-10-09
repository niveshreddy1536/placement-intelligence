package com.placementintelligence.backend.controller;

import com.placementintelligence.backend.entity.JobDescription;
import com.placementintelligence.backend.repository.JobDescriptionRepository;
import com.placementintelligence.backend.service.JobDescriptionAnalyzerService;
import com.placementintelligence.backend.service.PlacementMatchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matching")
public class ExternalJobMatchController {

    private final PlacementMatchService placementMatchService;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final JobDescriptionAnalyzerService analyzerService;

    public ExternalJobMatchController(
            PlacementMatchService placementMatchService,
            JobDescriptionRepository jobDescriptionRepository,
            JobDescriptionAnalyzerService analyzerService) {

        this.placementMatchService =
                placementMatchService;

        this.jobDescriptionRepository =
                jobDescriptionRepository;

        this.analyzerService =
                analyzerService;
    }

    /*
     * =========================================================
     * ANALYZE AN EXTERNAL JOB DESCRIPTION
     * =========================================================
     *
     * This endpoint is for a job description that the student
     * gets from somewhere else.
     *
     * The temporary JobDescription is saved only while the
     * existing matching engine processes it.
     *
     * It is deleted immediately after the match is calculated.
     *
     * Therefore, external JDs do NOT become part of the
     * permanent Job Pool.
     */
    @PostMapping("/student/{studentId}/external")
    public PlacementMatchService.MatchResult matchExternalJob(
            @PathVariable Long studentId,
            @RequestBody JobDescription jobDescription) {

        String title =
                jobDescription.getTitle();

        String description =
                jobDescription.getDescription();

        if (title == null ||
                title.isBlank()) {

            throw new RuntimeException(
                    "Job title is required"
            );
        }

        if (description == null ||
                description.isBlank()) {

            throw new RuntimeException(
                    "Job description is required"
            );
        }

        /*
         * Let the existing analyzer extract the skills
         * from the external job description.
         */
        List<String> extractedSkills =
                analyzerService.extractSkills(
                        title,
                        description
                );

        jobDescription.setExtractedSkills(
                String.join(
                        ", ",
                        extractedSkills
                )
        );

        if (jobDescription
                .getExtractedResponsibilities() == null) {

            jobDescription
                    .setExtractedResponsibilities("");
        }

        if (jobDescription
                .getExtractedRequirements() == null) {

            jobDescription
                    .setExtractedRequirements("");
        }

        /*
         * Save temporarily so the existing matching engine
         * can use its normal JobDescription-based workflow.
         */
        JobDescription temporaryJob =
                jobDescriptionRepository.save(
                        jobDescription
                );

        try {

            return placementMatchService
                    .matchStudentWithJob(
                            studentId,
                            temporaryJob.getId()
                    );

        } finally {

            /*
             * External JDs should NOT remain in the
             * permanent Job Pool.
             */
            jobDescriptionRepository.delete(
                    temporaryJob
            );
        }
    }
}