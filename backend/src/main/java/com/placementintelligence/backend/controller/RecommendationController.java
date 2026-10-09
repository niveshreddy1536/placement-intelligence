package com.placementintelligence.backend.controller;

import com.placementintelligence.backend.dto.JobRecommendation;
import com.placementintelligence.backend.service.RecommendationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(
            RecommendationService recommendationService) {

        this.recommendationService = recommendationService;
    }

    @GetMapping("/student/{studentId}")
    public List<JobRecommendation> getRecommendations(
            @PathVariable Long studentId) {

        return recommendationService.recommendJobs(studentId);
    }
}