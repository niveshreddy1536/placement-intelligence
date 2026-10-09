package com.placementintelligence.backend.service;

import com.placementintelligence.backend.dto.JobRecommendation;
import com.placementintelligence.backend.entity.Job;
import com.placementintelligence.backend.entity.Skill;
import com.placementintelligence.backend.entity.Student;
import com.placementintelligence.backend.repository.JobRepository;
import com.placementintelligence.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RecommendationService {

    private final StudentRepository studentRepository;
    private final JobRepository jobRepository;

    public RecommendationService(
            StudentRepository studentRepository,
            JobRepository jobRepository) {

        this.studentRepository = studentRepository;
        this.jobRepository = jobRepository;
    }

    public List<JobRecommendation> recommendJobs(Long studentId) {

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        List<Skill> studentSkills = student.getSkills();

        if (studentSkills == null) {
            studentSkills = new ArrayList<>();
        }

        List<Job> jobs = jobRepository.findAll();

        List<JobRecommendation> recommendations = new ArrayList<>();

        for (Job job : jobs) {

            List<Skill> requiredSkills = job.getRequiredSkills();

            if (requiredSkills == null || requiredSkills.isEmpty()) {
                continue;
            }

            List<String> matchedSkills = new ArrayList<>();
            List<String> missingSkills = new ArrayList<>();

            for (Skill requiredSkill : requiredSkills) {

                boolean matched = false;

                for (Skill studentSkill : studentSkills) {

                    if (studentSkill.getName()
                            .equalsIgnoreCase(requiredSkill.getName())) {

                        matched = true;
                        break;
                    }
                }

                if (matched) {
                    matchedSkills.add(requiredSkill.getName());
                } else {
                    missingSkills.add(requiredSkill.getName());
                }
            }

            double matchPercentage =
                    ((double) matchedSkills.size()
                            / requiredSkills.size()) * 100;

            JobRecommendation recommendation =
                    new JobRecommendation();

            recommendation.setJobId(job.getId());
            recommendation.setJobTitle(job.getJobTitle());
            recommendation.setCompanyName(job.getCompanyName());
            recommendation.setLocation(job.getLocation());
            recommendation.setSalary(job.getSalary());
            recommendation.setMatchPercentage(matchPercentage);

            recommendation.setMatchedSkills(matchedSkills);
            recommendation.setMissingSkills(missingSkills);

            recommendations.add(recommendation);
        }

        recommendations.sort(
                (a, b) -> Double.compare(
                        b.getMatchPercentage(),
                        a.getMatchPercentage()
                )
        );

        return recommendations;
    }
}