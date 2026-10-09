package com.placementintelligence.backend.service;

import com.placementintelligence.backend.entity.JobDescription;
import com.placementintelligence.backend.repository.JobDescriptionRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class JobDescriptionRecommendationService {

    private final JobDescriptionRepository jobDescriptionRepository;
    private final PlacementMatchService placementMatchService;

    public JobDescriptionRecommendationService(
            JobDescriptionRepository jobDescriptionRepository,
            PlacementMatchService placementMatchService) {

        this.jobDescriptionRepository =
                jobDescriptionRepository;

        this.placementMatchService =
                placementMatchService;
    }

    public List<JobRecommendation> recommendJobs(
            Long studentId) {

        List<JobDescription> jobDescriptions =
                jobDescriptionRepository.findAll();

        List<JobRecommendation> recommendations =
                new ArrayList<>();

        for (JobDescription jobDescription :
                jobDescriptions) {

            if (jobDescription == null ||
                    jobDescription.getId() == null) {

                continue;
            }

            Long jobDescriptionId =
                    jobDescription.getId();

            try {

                /*
                 * Run the existing intelligent matcher
                 * for this specific JD.
                 */
                PlacementMatchService.MatchResult matchResult =
                        placementMatchService.matchStudentWithJob(
                                studentId,
                                jobDescriptionId
                        );

                if (matchResult == null) {
                    System.err.println(
                            "No match result returned for JD #"
                                    + jobDescriptionId
                    );

                    continue;
                }

                JobRecommendation recommendation =
                        new JobRecommendation();

                /*
                 * -------------------------------------------------
                 * BASIC JOB INFORMATION
                 * -------------------------------------------------
                 */

                recommendation.setJobId(
                        jobDescriptionId
                );

                recommendation.setStudentId(
                        studentId
                );

                recommendation.setJobTitle(
                        safe(jobDescription.getTitle())
                );

                recommendation.setCompanyName(
                        safe(jobDescription.getCompanyName())
                );

                recommendation.setLocation(
                        safe(jobDescription.getLocation())
                );

                recommendation.setJobType(
                        safe(jobDescription.getJobType())
                );

                recommendation.setWorkMode(
                        safe(jobDescription.getWorkMode())
                );

                recommendation.setSalary(
                        jobDescription.getSalary()
                );

                recommendation.setSalaryText(
                        safe(jobDescription.getSalaryText())
                );

                recommendation.setExperience(
                        safe(jobDescription.getExperience())
                );

                recommendation.setEducation(
                        safe(jobDescription.getEducation())
                );

                recommendation.setApplicationUrl(
                        safe(jobDescription.getApplicationUrl())
                );

                recommendation.setPostedDate(
                        safe(jobDescription.getPostedDate())
                );

                recommendation.setApplicationDeadline(
                        safe(
                                jobDescription
                                        .getApplicationDeadline()
                        )
                );

                recommendation.setDescription(
                        safe(jobDescription.getDescription())
                );

                /*
                 * -------------------------------------------------
                 * MATCH SCORES
                 * -------------------------------------------------
                 */

                recommendation.setMatchPercentage(
                        matchResult.getMatchPercentage()
                );

                recommendation.setSkillScore(
                        matchResult.getSkillScore()
                );

                recommendation.setProjectScore(
                        matchResult.getProjectScore()
                );

                recommendation.setExperienceScore(
                        matchResult.getExperienceScore()
                );

                recommendation.setEducationCertificationScore(
                        matchResult
                                .getEducationCertificationScore()
                );

                /*
                 * -------------------------------------------------
                 * SKILLS
                 * -------------------------------------------------
                 */

                recommendation.setRequiredSkills(
                        safeList(
                                matchResult.getRequiredSkills()
                        )
                );

                recommendation.setMatchedSkills(
                        safeList(
                                matchResult.getMatchedSkills()
                        )
                );

                recommendation.setMissingSkills(
                        safeList(
                                matchResult.getMissingSkills()
                        )
                );

                /*
                 * -------------------------------------------------
                 * STRENGTHS
                 * -------------------------------------------------
                 */

                recommendation.setStrengths(
                        safeList(
                                matchResult.getStrengths()
                        )
                );

                /*
                 * -------------------------------------------------
                 * RECOMMENDATIONS
                 * -------------------------------------------------
                 */

                recommendation.setRecommendations(
                        safeList(
                                matchResult.getRecommendations()
                        )
                );

                /*
                 * The actual PlacementMatchService returns
                 * List<ProjectMatch>, so we do not try to convert
                 * those objects into List<String>.
                 *
                 * The project intelligence is already reflected
                 * in projectScore.
                 */

                recommendation.setRelevantProjects(
                        new ArrayList<>()
                );

                recommendation.setProjectRecommendation(
                        ""
                );

                /*
                 * -------------------------------------------------
                 * EXPLANATION
                 * -------------------------------------------------
                 */

                recommendation.setReason(
                        buildReason(
                                recommendation
                        )
                );

                recommendations.add(
                        recommendation
                );

                System.out.println(
                        "Successfully analyzed JD #"
                                + jobDescriptionId
                                + " - "
                                + safe(
                                jobDescription.getTitle()
                        )
                                + " - Score: "
                                + matchResult
                                .getMatchPercentage()
                );

            } catch (Exception exception) {

                /*
                 * IMPORTANT:
                 *
                 * One bad JD should NOT cause the entire
                 * recommendation endpoint to return HTTP 500.
                 */

                System.err.println(
                        "Failed to analyze JD #"
                                + jobDescriptionId
                                + " - "
                                + safe(
                                jobDescription.getTitle()
                        )
                );

                System.err.println(
                        "Reason: "
                                + exception.getMessage()
                );

                exception.printStackTrace();

                /*
                 * Continue with the next JD.
                 */
                continue;
            }
        }

        /*
         * ---------------------------------------------------------
         * SORT HIGHEST MATCH FIRST
         * ---------------------------------------------------------
         */

        recommendations.sort(
                Comparator.comparingDouble(
                        JobRecommendation::getMatchPercentage
                ).reversed()
        );

        /*
         * ---------------------------------------------------------
         * ASSIGN RANK
         * ---------------------------------------------------------
         */

        for (int i = 0;
             i < recommendations.size();
             i++) {

            recommendations
                    .get(i)
                    .setRank(i + 1);
        }

        return recommendations;
    }

    /*
     * =============================================================
     * BUILD HUMAN-READABLE REASON
     * =============================================================
     */

    private String buildReason(
            JobRecommendation recommendation) {

        double score =
                recommendation.getMatchPercentage();

        int matched =
                recommendation
                        .getMatchedSkills()
                        .size();

        int missing =
                recommendation
                        .getMissingSkills()
                        .size();

        StringBuilder reason =
                new StringBuilder();

        if (score >= 80) {

            reason.append(
                    "This is a strong overall match"
            );

        } else if (score >= 60) {

            reason.append(
                    "This is a good overall match"
            );

        } else if (score >= 40) {

            reason.append(
                    "This is a moderate overall match"
            );

        } else {

            reason.append(
                    "This job currently has a lower match"
            );
        }

        if (matched > 0) {

            reason.append(
                    " with "
                            + matched
                            + " matched skill"
                            + (
                            matched == 1
                                    ? ""
                                    : "s"
                    )
            );
        }

        if (missing > 0) {

            reason.append(
                    " and "
                            + missing
                            + " skill"
                            + (
                            missing == 1
                                    ? ""
                                    : "s"
                    )
                            + " to develop"
            );
        }

        if (recommendation.getProjectScore() >= 60) {

            reason.append(
                    ". Your projects provide strong evidence for this role"
            );

        } else if (
                recommendation.getProjectScore() >= 35) {

            reason.append(
                    ". Your projects provide some transferable evidence"
            );
        }

        return reason.toString() + ".";
    }

    /*
     * =============================================================
     * SAFE STRING
     * =============================================================
     */

    private String safe(String value) {

        if (value == null) {
            return "";
        }

        return value.trim();
    }

    /*
     * =============================================================
     * SAFE LIST
     * =============================================================
     */

    private List<String> safeList(
            List<String> values) {

        if (values == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(
                values
        );
    }

    /*
     * =============================================================
     * JOB RECOMMENDATION DTO
     * =============================================================
     */

    public static class JobRecommendation {

        private Long jobId;

        private Long studentId;

        private int rank;

        private String jobTitle;

        private String companyName;

        private String location;

        private String jobType;

        private String workMode;

        private Double salary;

        private String salaryText;

        private String experience;

        private String education;

        private String applicationUrl;

        private String postedDate;

        private String applicationDeadline;

        private String description;

        private double matchPercentage;

        private double skillScore;

        private double projectScore;

        private double experienceScore;

        private double educationCertificationScore;

        private List<String> requiredSkills =
                new ArrayList<>();

        private List<String> matchedSkills =
                new ArrayList<>();

        private List<String> missingSkills =
                new ArrayList<>();

        private List<String> strengths =
                new ArrayList<>();

        private List<String> recommendations =
                new ArrayList<>();

        private List<String> relevantProjects =
                new ArrayList<>();

        private String projectRecommendation;

        private String reason;

        public Long getJobId() {
            return jobId;
        }

        public void setJobId(
                Long jobId) {

            this.jobId =
                    jobId;
        }

        public Long getStudentId() {
            return studentId;
        }

        public void setStudentId(
                Long studentId) {

            this.studentId =
                    studentId;
        }

        public int getRank() {
            return rank;
        }

        public void setRank(
                int rank) {

            this.rank =
                    rank;
        }

        public String getJobTitle() {
            return jobTitle;
        }

        public void setJobTitle(
                String jobTitle) {

            this.jobTitle =
                    jobTitle;
        }

        public String getCompanyName() {
            return companyName;
        }

        public void setCompanyName(
                String companyName) {

            this.companyName =
                    companyName;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(
                String location) {

            this.location =
                    location;
        }

        public String getJobType() {
            return jobType;
        }

        public void setJobType(
                String jobType) {

            this.jobType =
                    jobType;
        }

        public String getWorkMode() {
            return workMode;
        }

        public void setWorkMode(
                String workMode) {

            this.workMode =
                    workMode;
        }

        public Double getSalary() {
            return salary;
        }

        public void setSalary(
                Double salary) {

            this.salary =
                    salary;
        }

        public String getSalaryText() {
            return salaryText;
        }

        public void setSalaryText(
                String salaryText) {

            this.salaryText =
                    salaryText;
        }

        public String getExperience() {
            return experience;
        }

        public void setExperience(
                String experience) {

            this.experience =
                    experience;
        }

        public String getEducation() {
            return education;
        }

        public void setEducation(
                String education) {

            this.education =
                    education;
        }

        public String getApplicationUrl() {
            return applicationUrl;
        }

        public void setApplicationUrl(
                String applicationUrl) {

            this.applicationUrl =
                    applicationUrl;
        }

        public String getPostedDate() {
            return postedDate;
        }

        public void setPostedDate(
                String postedDate) {

            this.postedDate =
                    postedDate;
        }

        public String getApplicationDeadline() {
            return applicationDeadline;
        }

        public void setApplicationDeadline(
                String applicationDeadline) {

            this.applicationDeadline =
                    applicationDeadline;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(
                String description) {

            this.description =
                    description;
        }

        public double getMatchPercentage() {
            return matchPercentage;
        }

        public void setMatchPercentage(
                double matchPercentage) {

            this.matchPercentage =
                    matchPercentage;
        }

        public double getSkillScore() {
            return skillScore;
        }

        public void setSkillScore(
                double skillScore) {

            this.skillScore =
                    skillScore;
        }

        public double getProjectScore() {
            return projectScore;
        }

        public void setProjectScore(
                double projectScore) {

            this.projectScore =
                    projectScore;
        }

        public double getExperienceScore() {
            return experienceScore;
        }

        public void setExperienceScore(
                double experienceScore) {

            this.experienceScore =
                    experienceScore;
        }

        public double getEducationCertificationScore() {
            return educationCertificationScore;
        }

        public void setEducationCertificationScore(
                double educationCertificationScore) {

            this.educationCertificationScore =
                    educationCertificationScore;
        }

        public List<String> getRequiredSkills() {
            return requiredSkills;
        }

        public void setRequiredSkills(
                List<String> requiredSkills) {

            this.requiredSkills =
                    requiredSkills;
        }

        public List<String> getMatchedSkills() {
            return matchedSkills;
        }

        public void setMatchedSkills(
                List<String> matchedSkills) {

            this.matchedSkills =
                    matchedSkills;
        }

        public List<String> getMissingSkills() {
            return missingSkills;
        }

        public void setMissingSkills(
                List<String> missingSkills) {

            this.missingSkills =
                    missingSkills;
        }

        public List<String> getStrengths() {
            return strengths;
        }

        public void setStrengths(
                List<String> strengths) {

            this.strengths =
                    strengths;
        }

        public List<String> getRecommendations() {
            return recommendations;
        }

        public void setRecommendations(
                List<String> recommendations) {

            this.recommendations =
                    recommendations;
        }

        public List<String> getRelevantProjects() {
            return relevantProjects;
        }

        public void setRelevantProjects(
                List<String> relevantProjects) {

            this.relevantProjects =
                    relevantProjects;
        }

        public String getProjectRecommendation() {
            return projectRecommendation;
        }

        public void setProjectRecommendation(
                String projectRecommendation) {

            this.projectRecommendation =
                    projectRecommendation;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(
                String reason) {

            this.reason =
                    reason;
        }
    }
}