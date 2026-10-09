package com.placementintelligence.backend.controller;

import com.placementintelligence.backend.entity.JobDescription;
import com.placementintelligence.backend.repository.JobDescriptionRepository;
import com.placementintelligence.backend.service.JobDescriptionAnalyzerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/job-descriptions")
public class JobDescriptionController {

    private final JobDescriptionRepository jobDescriptionRepository;
    private final JobDescriptionAnalyzerService analyzerService;

    public JobDescriptionController(
            JobDescriptionRepository jobDescriptionRepository,
            JobDescriptionAnalyzerService analyzerService) {

        this.jobDescriptionRepository =
                jobDescriptionRepository;

        this.analyzerService =
                analyzerService;
    }

    /*
     * =========================================================
     * CREATE JOB DESCRIPTION
     * =========================================================
     */

    @PostMapping
    public JobDescription createJobDescription(
            @RequestBody JobDescription jobDescription) {

        validateJobDescription(jobDescription);

        analyzeJobDescription(jobDescription);

        return jobDescriptionRepository.save(
                jobDescription
        );
    }

    /*
     * =========================================================
     * GET ALL JOB DESCRIPTIONS
     * =========================================================
     */

    @GetMapping
    public List<JobDescription> getAllJobDescriptions() {

        return jobDescriptionRepository.findAll();
    }

    /*
     * =========================================================
     * GET ONE JOB DESCRIPTION
     * =========================================================
     */

    @GetMapping("/{id}")
    public JobDescription getJobDescription(
            @PathVariable Long id) {

        return jobDescriptionRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Job description not found"
                        )
                );
    }

    /*
     * =========================================================
     * UPDATE JOB DESCRIPTION
     * =========================================================
     */

    @PutMapping("/{id}")
    public JobDescription updateJobDescription(
            @PathVariable Long id,
            @RequestBody JobDescription updatedJob) {

        JobDescription existingJob =
                jobDescriptionRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Job description not found"
                                )
                        );

        validateJobDescription(updatedJob);

        /*
         * Preserve the existing database ID.
         */
        existingJob.setTitle(
                updatedJob.getTitle()
        );

        existingJob.setCompanyName(
                updatedJob.getCompanyName()
        );

        existingJob.setLocation(
                updatedJob.getLocation()
        );

        existingJob.setJobType(
                updatedJob.getJobType()
        );

        existingJob.setWorkMode(
                updatedJob.getWorkMode()
        );

        existingJob.setSalary(
                updatedJob.getSalary()
        );

        existingJob.setSalaryText(
                updatedJob.getSalaryText()
        );

        existingJob.setExperience(
                updatedJob.getExperience()
        );

        existingJob.setEducation(
                updatedJob.getEducation()
        );

        existingJob.setApplicationUrl(
                updatedJob.getApplicationUrl()
        );

        existingJob.setPostedDate(
                updatedJob.getPostedDate()
        );

        existingJob.setApplicationDeadline(
                updatedJob.getApplicationDeadline()
        );

        existingJob.setDescription(
                updatedJob.getDescription()
        );

        existingJob.setExtractedResponsibilities(
                updatedJob.getExtractedResponsibilities()
        );

        existingJob.setExtractedRequirements(
                updatedJob.getExtractedRequirements()
        );

        /*
         * IMPORTANT:
         *
         * Re-run skill extraction whenever the title or
         * description is changed.
         */
        analyzeJobDescription(existingJob);

        return jobDescriptionRepository.save(
                existingJob
        );
    }

    /*
     * =========================================================
     * DELETE JOB DESCRIPTION
     * =========================================================
     */

    @DeleteMapping("/{id}")
    public void deleteJobDescription(
            @PathVariable Long id) {

        JobDescription existingJob =
                jobDescriptionRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Job description not found"
                                )
                        );

        jobDescriptionRepository.delete(
                existingJob
        );
    }

    /*
     * =========================================================
     * VALIDATION
     * =========================================================
     */

    private void validateJobDescription(
            JobDescription jobDescription) {

        if (jobDescription == null) {
            throw new RuntimeException(
                    "Job description is required"
            );
        }

        if (jobDescription.getTitle() == null ||
                jobDescription.getTitle().isBlank()) {

            throw new RuntimeException(
                    "Job title is required"
            );
        }

        if (jobDescription.getDescription() == null ||
                jobDescription.getDescription().isBlank()) {

            throw new RuntimeException(
                    "Job description is required"
            );
        }
    }

    /*
     * =========================================================
     * SKILL EXTRACTION
     * =========================================================
     */

    private void analyzeJobDescription(
            JobDescription jobDescription) {

        String title =
                jobDescription.getTitle();

        String description =
                jobDescription.getDescription();

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

            jobDescription.setExtractedResponsibilities(
                    ""
            );
        }

        if (jobDescription
                .getExtractedRequirements() == null) {

            jobDescription.setExtractedRequirements(
                    ""
            );
        }
    }
}