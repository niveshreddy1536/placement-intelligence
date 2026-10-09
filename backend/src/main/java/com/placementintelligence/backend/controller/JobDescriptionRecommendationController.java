package com.placementintelligence.backend.controller;

import com.placementintelligence.backend.service.JobDescriptionRecommendationService;
import com.placementintelligence.backend.service.JobDescriptionRecommendationService.JobRecommendation;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/placement-recommendations")
@CrossOrigin(origins = "http://localhost:5173")
public class JobDescriptionRecommendationController {

    private final JobDescriptionRecommendationService recommendationService;

    public JobDescriptionRecommendationController(
            JobDescriptionRecommendationService recommendationService) {

        this.recommendationService =
                recommendationService;
    }

    @GetMapping("/student/{studentId}")
    public List<JobRecommendation> getRecommendations(
            @PathVariable Long studentId) {

        return recommendationService.recommendJobs(
                studentId
        );
    }
}