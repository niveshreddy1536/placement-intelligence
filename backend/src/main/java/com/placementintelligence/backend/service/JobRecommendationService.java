package com.placementintelligence.backend.service;

import com.placementintelligence.backend.entity.JobDescription;
import com.placementintelligence.backend.entity.Skill;
import com.placementintelligence.backend.entity.Student;
import com.placementintelligence.backend.repository.JobDescriptionRepository;
import com.placementintelligence.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class JobRecommendationService {

    private final StudentRepository studentRepository;
    private final JobDescriptionRepository jobDescriptionRepository;

    public JobRecommendationService(
            StudentRepository studentRepository,
            JobDescriptionRepository jobDescriptionRepository) {

        this.studentRepository = studentRepository;
        this.jobDescriptionRepository = jobDescriptionRepository;
    }

    /*
     * =============================================================
     * RECOMMEND JOBS
     * =============================================================
     */

    public List<JobRecommendation> recommendJobs(
            Long studentId) {

        Student student =
                studentRepository.findById(studentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Student not found"
                                )
                        );

        Set<String> studentSkills =
                getStudentSkills(student);

        /*
         * IMPORTANT:
         *
         * Recommendations now use the SAME JobDescription
         * records used by the Job Pool.
         *
         * This prevents old Job records such as Google from
         * appearing here.
         */
        List<JobDescription> jobs =
                jobDescriptionRepository.findAll();

        List<JobRecommendation> recommendations =
                new ArrayList<>();

        for (JobDescription job : jobs) {

            if (job == null) {
                continue;
            }

            /*
             * Skip jobs that do not have a title.
             */
            if (job.getTitle() == null ||
                    job.getTitle().isBlank()) {

                continue;
            }

            /*
             * Get required skills from the JobDescription.
             */
            List<String> requiredSkills =
                    extractRequiredSkills(job);

            /*
             * We need at least one skill to calculate
             * a useful recommendation.
             */
            if (requiredSkills.isEmpty()) {
                continue;
            }

            JobRecommendation recommendation =
                    calculateRecommendation(
                            student,
                            studentSkills,
                            job,
                            requiredSkills
                    );

            recommendations.add(recommendation);
        }

        /*
         * Highest match first.
         */
        recommendations.sort(
                Comparator.comparingDouble(
                        JobRecommendation::getMatchPercentage
                ).reversed()
        );

        /*
         * Assign ranking after sorting.
         */
        for (int i = 0;
             i < recommendations.size();
             i++) {

            recommendations.get(i)
                    .setRank(i + 1);
        }

        return recommendations;
    }

    /*
     * =============================================================
     * CALCULATE JOB RECOMMENDATION
     * =============================================================
     */

    private JobRecommendation calculateRecommendation(
            Student student,
            Set<String> studentSkills,
            JobDescription job,
            List<String> requiredSkills) {

        List<String> matchedSkills =
                new ArrayList<>();

        List<String> missingSkills =
                new ArrayList<>();

        /*
         * =========================================================
         * SKILL MATCH
         * =========================================================
         */

        for (String requiredSkill :
                requiredSkills) {

            if (skillMatches(
                    requiredSkill,
                    studentSkills)) {

                matchedSkills.add(
                        requiredSkill
                );

            } else {

                missingSkills.add(
                        requiredSkill
                );
            }
        }

        /*
         * =========================================================
         * SKILL SCORE
         * =========================================================
         */

        double skillScore =
                calculateSkillScore(
                        matchedSkills.size(),
                        requiredSkills.size()
                );

        /*
         * =========================================================
         * ROLE SCORE
         * =========================================================
         */

        double roleScore =
                calculateRoleScore(
                        studentSkills,
                        job
                );

        /*
         * =========================================================
         * SKILL BREADTH
         * =========================================================
         */

        double breadthScore =
                calculateBreadthScore(
                        studentSkills
                );

        /*
         * =========================================================
         * FINAL SCORE
         *
         * Skills             = 70%
         * Role compatibility = 20%
         * Skill breadth      = 10%
         * =========================================================
         */

        double finalScore =
                (skillScore * 0.70)
                        + (roleScore * 0.20)
                        + (breadthScore * 0.10);

        finalScore =
                Math.min(
                        100.0,
                        Math.max(
                                0.0,
                                finalScore
                        )
                );

        /*
         * =========================================================
         * BUILD RESULT
         * =========================================================
         */

        JobRecommendation recommendation =
                new JobRecommendation();

        recommendation.setJobId(
                job.getId()
        );

        recommendation.setCompanyName(
                job.getCompanyName()
        );

        recommendation.setJobTitle(
                job.getTitle()
        );

        recommendation.setLocation(
                job.getLocation()
        );

        recommendation.setJobType(
                job.getJobType()
        );

        recommendation.setWorkMode(
                job.getWorkMode()
        );

        recommendation.setSalary(
                job.getSalary()
        );

        recommendation.setSalaryText(
                job.getSalaryText()
        );

        recommendation.setExperience(
                job.getExperience()
        );

        recommendation.setEducation(
                job.getEducation()
        );

        recommendation.setApplicationUrl(
                job.getApplicationUrl()
        );

        recommendation.setPostedDate(
                job.getPostedDate()
        );

        recommendation.setApplicationDeadline(
                job.getApplicationDeadline()
        );

        recommendation.setDescription(
                job.getDescription()
        );

        recommendation.setMatchPercentage(
                round(finalScore)
        );

        recommendation.setSkillScore(
                round(skillScore)
        );

        recommendation.setRoleScore(
                round(roleScore)
        );

        recommendation.setBreadthScore(
                round(breadthScore)
        );

        recommendation.setRequiredSkills(
                requiredSkills
        );

        recommendation.setMatchedSkills(
                matchedSkills
        );

        recommendation.setMissingSkills(
                missingSkills
        );

        recommendation.setMatchLevel(
                determineMatchLevel(
                        finalScore
                )
        );

        recommendation.setReason(
                generateReason(
                        matchedSkills,
                        missingSkills,
                        finalScore
                )
        );

        return recommendation;
    }

    /*
     * =============================================================
     * EXTRACT REQUIRED SKILLS
     * =============================================================
     */

    private List<String> extractRequiredSkills(
            JobDescription job) {

        Set<String> skills =
                new LinkedHashSet<>();

        /*
         * First use extractedSkills stored in the
         * JobDescription database record.
         *
         * Example:
         *
         * Java, Spring Boot, REST API, SQL, Git
         */
        if (job.getExtractedSkills() != null &&
                !job.getExtractedSkills().isBlank()) {

            String[] parts =
                    job.getExtractedSkills()
                            .split(",");

            for (String part : parts) {

                String skill =
                        part.trim();

                if (!skill.isBlank()) {

                    skills.add(
                            normalizeDisplay(skill)
                    );
                }
            }
        }

        /*
         * If extractedSkills is empty, infer skills
         * from the complete job information.
         */
        if (skills.isEmpty()) {

            String jobText =
                    buildJobText(job);

            addSkillIfPresent(
                    skills,
                    jobText,
                    "Java"
            );

            addSkillIfPresent(
                    skills,
                    jobText,
                    "Python"
            );

            addSkillIfPresent(
                    skills,
                    jobText,
                    "Spring Boot"
            );

            addSkillIfPresent(
                    skills,
                    jobText,
                    "REST API"
            );

            addSkillIfPresent(
                    skills,
                    jobText,
                    "SQL"
            );

            addSkillIfPresent(
                    skills,
                    jobText,
                    "Git"
            );

            addSkillIfPresent(
                    skills,
                    jobText,
                    "HTML"
            );

            addSkillIfPresent(
                    skills,
                    jobText,
                    "CSS"
            );

            addSkillIfPresent(
                    skills,
                    jobText,
                    "JavaScript"
            );

            addSkillIfPresent(
                    skills,
                    jobText,
                    "React"
            );

            addSkillIfPresent(
                    skills,
                    jobText,
                    "Machine Learning"
            );

            addSkillIfPresent(
                    skills,
                    jobText,
                    "NLP"
            );

            addSkillIfPresent(
                    skills,
                    jobText,
                    "Computer Vision"
            );

            addSkillIfPresent(
                    skills,
                    jobText,
                    "YOLO"
            );

            addSkillIfPresent(
                    skills,
                    jobText,
                    "Excel"
            );

            addSkillIfPresent(
                    skills,
                    jobText,
                    "Power BI"
            );

            addSkillIfPresent(
                    skills,
                    jobText,
                    "Statistics"
            );
        }

        return new ArrayList<>(skills);
    }

    /*
     * =============================================================
     * BUILD COMPLETE JOB TEXT
     * =============================================================
     */

    private String buildJobText(
            JobDescription job) {

        StringBuilder text =
                new StringBuilder();

        appendText(
                text,
                job.getTitle()
        );

        appendText(
                text,
                job.getDescription()
        );

        appendText(
                text,
                job.getExtractedResponsibilities()
        );

        appendText(
                text,
                job.getExtractedRequirements()
        );

        appendText(
                text,
                job.getExtractedSkills()
        );

        appendText(
                text,
                job.getEducation()
        );

        appendText(
                text,
                job.getExperience()
        );

        return safeLower(
                text.toString()
        );
    }

    private void appendText(
            StringBuilder text,
            String value) {

        if (value != null &&
                !value.isBlank()) {

            text.append(value)
                    .append(" ");
        }
    }

    /*
     * =============================================================
     * ADD INFERRED SKILL
     * =============================================================
     */

    private void addSkillIfPresent(
            Set<String> skills,
            String jobText,
            String skill) {

        if (jobText.contains(
                safeLower(skill)
        )) {

            skills.add(
                    normalizeDisplay(skill)
            );
        }
    }

    /*
     * =============================================================
     * SKILL SCORE
     * =============================================================
     */

    private double calculateSkillScore(
            int matched,
            int required) {

        if (required == 0) {
            return 0.0;
        }

        return Math.min(
                100.0,
                ((double) matched / required)
                        * 100.0
        );
    }

    /*
     * =============================================================
     * ROLE SCORE
     * =============================================================
     */

    private double calculateRoleScore(
            Set<String> studentSkills,
            JobDescription job) {

        String title =
                safeLower(
                        job.getTitle()
                );

        double score = 0.0;

        /*
         * =========================================================
         * BACKEND
         * =========================================================
         */

        if (containsAny(
                title,
                "backend",
                "back-end"
        )) {

            if (hasSkill(
                    studentSkills,
                    "Java"
            )) {
                score += 35.0;
            }

            if (hasSkill(
                    studentSkills,
                    "Spring Boot"
            )) {
                score += 25.0;
            }

            if (hasSkill(
                    studentSkills,
                    "SQL"
            )) {
                score += 15.0;
            }

            if (hasSkill(
                    studentSkills,
                    "REST API"
            ) || hasSkill(
                    studentSkills,
                    "API Development"
            )) {
                score += 15.0;
            }

            if (hasSkill(
                    studentSkills,
                    "Git"
            )) {
                score += 10.0;
            }

            return Math.min(
                    100.0,
                    score
            );
        }

        /*
         * =========================================================
         * FRONTEND
         * =========================================================
         */

        if (containsAny(
                title,
                "frontend",
                "front-end",
                "ui developer"
        )) {

            if (hasSkill(
                    studentSkills,
                    "HTML"
            )) {
                score += 25.0;
            }

            if (hasSkill(
                    studentSkills,
                    "CSS"
            )) {
                score += 20.0;
            }

            if (hasSkill(
                    studentSkills,
                    "JavaScript"
            )) {
                score += 30.0;
            }

            if (hasSkill(
                    studentSkills,
                    "React"
            )) {
                score += 25.0;
            }

            return Math.min(
                    100.0,
                    score
            );
        }

        /*
         * =========================================================
         * FULL STACK
         * =========================================================
         */

        if (containsAny(
                title,
                "full stack",
                "fullstack"
        )) {

            if (hasSkill(
                    studentSkills,
                    "HTML"
            )) {
                score += 15.0;
            }

            if (hasSkill(
                    studentSkills,
                    "CSS"
            )) {
                score += 15.0;
            }

            if (hasSkill(
                    studentSkills,
                    "JavaScript"
            )) {
                score += 20.0;
            }

            if (hasSkill(
                    studentSkills,
                    "React"
            )) {
                score += 20.0;
            }

            if (hasSkill(
                    studentSkills,
                    "Java"
            )) {
                score += 10.0;
            }

            if (hasSkill(
                    studentSkills,
                    "SQL"
            )) {
                score += 10.0;
            }

            if (hasSkill(
                    studentSkills,
                    "Git"
            )) {
                score += 10.0;
            }

            return Math.min(
                    100.0,
                    score
            );
        }

        /*
         * =========================================================
         * DATA ANALYST
         * =========================================================
         */

        if (containsAny(
                title,
                "data analyst",
                "data analytics",
                "business analyst"
        )) {

            if (hasSkill(
                    studentSkills,
                    "SQL"
            )) {
                score += 30.0;
            }

            if (hasSkill(
                    studentSkills,
                    "Python"
            )) {
                score += 25.0;
            }

            if (hasSkill(
                    studentSkills,
                    "Excel"
            )) {
                score += 15.0;
            }

            if (hasSkill(
                    studentSkills,
                    "Power BI"
            )) {
                score += 20.0;
            }

            if (hasSkill(
                    studentSkills,
                    "Statistics"
            )) {
                score += 10.0;
            }

            return Math.min(
                    100.0,
                    score
            );
        }

        /*
         * =========================================================
         * MACHINE LEARNING / DATA SCIENCE
         * =========================================================
         */

        if (containsAny(
                title,
                "machine learning",
                "ml engineer",
                "ml developer",
                "data scientist"
        )) {

            if (hasSkill(
                    studentSkills,
                    "Python"
            )) {
                score += 30.0;
            }

            if (hasSkill(
                    studentSkills,
                    "Machine Learning"
            )) {
                score += 30.0;
            }

            if (hasSkill(
                    studentSkills,
                    "NLP"
            )) {
                score += 15.0;
            }

            if (hasSkill(
                    studentSkills,
                    "Computer Vision"
            )) {
                score += 15.0;
            }

            if (hasSkill(
                    studentSkills,
                    "SQL"
            )) {
                score += 10.0;
            }

            return Math.min(
                    100.0,
                    score
            );
        }

        /*
         * =========================================================
         * NLP
         * =========================================================
         */

        if (containsAny(
                title,
                "nlp",
                "natural language"
        )) {

            if (hasSkill(
                    studentSkills,
                    "Python"
            )) {
                score += 30.0;
            }

            if (hasSkill(
                    studentSkills,
                    "NLP"
            )) {
                score += 40.0;
            }

            if (hasSkill(
                    studentSkills,
                    "Machine Learning"
            )) {
                score += 20.0;
            }

            if (hasSkill(
                    studentSkills,
                    "SQL"
            )) {
                score += 10.0;
            }

            return Math.min(
                    100.0,
                    score
            );
        }

        /*
         * =========================================================
         * COMPUTER VISION
         * =========================================================
         */

        if (containsAny(
                title,
                "computer vision",
                "vision engineer",
                "vision developer"
        )) {

            if (hasSkill(
                    studentSkills,
                    "Python"
            )) {
                score += 25.0;
            }

            if (hasSkill(
                    studentSkills,
                    "Computer Vision"
            )) {
                score += 40.0;
            }

            if (hasSkill(
                    studentSkills,
                    "YOLO"
            )) {
                score += 20.0;
            }

            if (hasSkill(
                    studentSkills,
                    "Machine Learning"
            )) {
                score += 15.0;
            }

            return Math.min(
                    100.0,
                    score
            );
        }

        /*
         * =========================================================
         * SOFTWARE ENGINEERING
         * =========================================================
         */

        if (containsAny(
                title,
                "software engineer",
                "software developer"
        )) {

            if (hasSkill(
                    studentSkills,
                    "Java"
            )) {
                score += 20.0;
            }

            if (hasSkill(
                    studentSkills,
                    "Python"
            )) {
                score += 20.0;
            }

            if (hasSkill(
                    studentSkills,
                    "SQL"
            )) {
                score += 15.0;
            }

            if (hasSkill(
                    studentSkills,
                    "Git"
            )) {
                score += 15.0;
            }

            if (hasSkill(
                    studentSkills,
                    "JavaScript"
            )) {
                score += 10.0;
            }

            if (hasSkill(
                    studentSkills,
                    "HTML"
            )) {
                score += 5.0;
            }

            if (hasSkill(
                    studentSkills,
                    "CSS"
            )) {
                score += 5.0;
            }

            if (hasSkill(
                    studentSkills,
                    "REST API"
            )) {
                score += 10.0;
            }

            return Math.min(
                    100.0,
                    score
            );
        }

        /*
         * =========================================================
         * UNKNOWN ROLE
         * =========================================================
         */

        return 50.0;
    }

    /*
     * =============================================================
     * SKILL BREADTH
     * =============================================================
     */

    private double calculateBreadthScore(
            Set<String> studentSkills) {

        if (studentSkills.isEmpty()) {
            return 0.0;
        }

        int skillCount =
                studentSkills.size();

        if (skillCount >= 12) {
            return 100.0;
        }

        if (skillCount >= 9) {
            return 85.0;
        }

        if (skillCount >= 7) {
            return 70.0;
        }

        if (skillCount >= 5) {
            return 55.0;
        }

        if (skillCount >= 3) {
            return 40.0;
        }

        return 25.0;
    }

    /*
     * =============================================================
     * MATCH LEVEL
     * =============================================================
     */

    private String determineMatchLevel(
            double score) {

        if (score >= 80.0) {
            return "EXCELLENT";
        }

        if (score >= 65.0) {
            return "STRONG";
        }

        if (score >= 50.0) {
            return "MODERATE";
        }

        if (score >= 35.0) {
            return "LOW";
        }

        return "VERY LOW";
    }

    /*
     * =============================================================
     * REASON
     * =============================================================
     */

    private String generateReason(
            List<String> matchedSkills,
            List<String> missingSkills,
            double score) {

        StringBuilder reason =
                new StringBuilder();

        if (!matchedSkills.isEmpty()) {

            reason.append(
                    "Matches "
            );

            reason.append(
                    matchedSkills.size()
            );

            reason.append(
                    " of "
            );

            reason.append(
                    matchedSkills.size()
                            + missingSkills.size()
            );

            reason.append(
                    " required skill(s)"
            );
        }

        if (!missingSkills.isEmpty()) {

            if (reason.length() > 0) {
                reason.append(". ");
            }

            reason.append(
                    "Main skill gaps: "
            );

            reason.append(
                    String.join(
                            ", ",
                            missingSkills
                    )
            );
        }

        if (score >= 80.0) {

            reason.append(
                    ". This job is a strong fit for your current profile."
            );

        } else if (score >= 65.0) {

            reason.append(
                    ". This job is a good target with some skill development."
            );

        } else if (score >= 50.0) {

            reason.append(
                    ". This job is a possible target but requires additional preparation."
            );

        } else {

            reason.append(
                    ". Consider building the missing skills before prioritizing this role."
            );
        }

        return reason.toString();
    }

    /*
     * =============================================================
     * GET STUDENT SKILLS
     * =============================================================
     */

    private Set<String> getStudentSkills(
            Student student) {

        Set<String> skills =
                new LinkedHashSet<>();

        if (student.getSkills() == null) {
            return skills;
        }

        for (Skill skill :
                student.getSkills()) {

            if (skill == null ||
                    skill.getName() == null ||
                    skill.getName().isBlank()) {

                continue;
            }

            skills.add(
                    normalizeDisplay(
                            skill.getName()
                    )
            );
        }

        return skills;
    }

    /*
     * =============================================================
     * SKILL MATCHING
     * =============================================================
     */

    private boolean skillMatches(
            String requiredSkill,
            Set<String> studentSkills) {

        String required =
                safeLower(requiredSkill);

        for (String studentSkill :
                studentSkills) {

            String current =
                    safeLower(studentSkill);

            /*
             * Exact match.
             */
            if (required.equals(current)) {
                return true;
            }

            /*
             * REST API / API Development.
             */
            if (required.equals("rest api") &&
                    (
                            current.contains("rest") ||
                                    current.equals("api development")
                    )) {

                return true;
            }

            /*
             * Spring can satisfy Spring Boot.
             */
            if (required.equals("spring boot") &&
                    current.equals("spring")) {

                return true;
            }

            /*
             * SQL can represent PostgreSQL/MySQL.
             */
            if (required.equals("postgresql") &&
                    (
                            current.equals("sql") ||
                                    current.equals("postgresql")
                    )) {

                return true;
            }

            if (required.equals("mysql") &&
                    (
                            current.equals("sql") ||
                                    current.equals("mysql")
                    )) {

                return true;
            }

            /*
             * Git can satisfy GitHub requirement.
             */
            if (required.equals("github") &&
                    current.equals("git")) {

                return true;
            }
        }

        return false;
    }

    /*
     * =============================================================
     * HAS SKILL
     * =============================================================
     */

    private boolean hasSkill(
            Set<String> skills,
            String target) {

        String wanted =
                safeLower(target);

        for (String skill :
                skills) {

            String current =
                    safeLower(skill);

            if (current.equals(wanted)) {
                return true;
            }

            if (wanted.equals("rest api") &&
                    (
                            current.contains("rest") ||
                                    current.equals("api development")
                    )) {

                return true;
            }

            if (wanted.equals("sql") &&
                    (
                            current.equals("postgresql") ||
                                    current.equals("mysql")
                    )) {

                return true;
            }
        }

        return false;
    }

    /*
     * =============================================================
     * CONTAINS ANY
     * =============================================================
     */

    private boolean containsAny(
            String text,
            String... values) {

        String lower =
                safeLower(text);

        for (String value :
                values) {

            if (lower.contains(
                    safeLower(value)
            )) {

                return true;
            }
        }

        return false;
    }

    /*
     * =============================================================
     * NORMALIZE DISPLAY
     * =============================================================
     */

    private String normalizeDisplay(
            String text) {

        if (text == null) {
            return "";
        }

        String value =
                text.trim();

        if (value.equalsIgnoreCase("java")) {
            return "Java";
        }

        if (value.equalsIgnoreCase("python")) {
            return "Python";
        }

        if (value.equalsIgnoreCase("sql")) {
            return "SQL";
        }

        if (value.equalsIgnoreCase("spring boot")) {
            return "Spring Boot";
        }

        if (value.equalsIgnoreCase("spring")) {
            return "Spring";
        }

        if (value.equalsIgnoreCase("rest api")) {
            return "REST API";
        }

        if (value.equalsIgnoreCase("api development")) {
            return "API Development";
        }

        if (value.equalsIgnoreCase("postgresql")) {
            return "PostgreSQL";
        }

        if (value.equalsIgnoreCase("mysql")) {
            return "MySQL";
        }

        if (value.equalsIgnoreCase("git")) {
            return "Git";
        }

        if (value.equalsIgnoreCase("github")) {
            return "GitHub";
        }

        if (value.equalsIgnoreCase("javascript")) {
            return "JavaScript";
        }

        if (value.equalsIgnoreCase("html")) {
            return "HTML";
        }

        if (value.equalsIgnoreCase("css")) {
            return "CSS";
        }

        if (value.equalsIgnoreCase("react")) {
            return "React";
        }

        if (value.equalsIgnoreCase("machine learning")) {
            return "Machine Learning";
        }

        if (value.equalsIgnoreCase("nlp")) {
            return "NLP";
        }

        if (value.equalsIgnoreCase("computer vision")) {
            return "Computer Vision";
        }

        if (value.equalsIgnoreCase("yolo")) {
            return "YOLO";
        }

        if (value.equalsIgnoreCase("excel")) {
            return "Excel";
        }

        if (value.equalsIgnoreCase("power bi")) {
            return "Power BI";
        }

        if (value.equalsIgnoreCase("statistics")) {
            return "Statistics";
        }

        return value;
    }

    /*
     * =============================================================
     * SAFE LOWERCASE
     * =============================================================
     */

    private String safeLower(
            String text) {

        if (text == null) {
            return "";
        }

        return text
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    /*
     * =============================================================
     * ROUND
     * =============================================================
     */

    private double round(
            double value) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }

    /*
     * =============================================================
     * DTO
     * =============================================================
     */

    public static class JobRecommendation {

        private int rank;

        private Long jobId;

        private String companyName;
        private String jobTitle;
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
        private double roleScore;
        private double breadthScore;

        private String matchLevel;
        private String reason;

        private List<String> requiredSkills;
        private List<String> matchedSkills;
        private List<String> missingSkills;

        public int getRank() {
            return rank;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }

        public Long getJobId() {
            return jobId;
        }

        public void setJobId(Long jobId) {
            this.jobId = jobId;
        }

        public String getCompanyName() {
            return companyName;
        }

        public void setCompanyName(
                String companyName) {

            this.companyName =
                    companyName;
        }

        public String getJobTitle() {
            return jobTitle;
        }

        public void setJobTitle(
                String jobTitle) {

            this.jobTitle =
                    jobTitle;
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

        public double getRoleScore() {
            return roleScore;
        }

        public void setRoleScore(
                double roleScore) {

            this.roleScore =
                    roleScore;
        }

        public double getBreadthScore() {
            return breadthScore;
        }

        public void setBreadthScore(
                double breadthScore) {

            this.breadthScore =
                    breadthScore;
        }

        public String getMatchLevel() {
            return matchLevel;
        }

        public void setMatchLevel(
                String matchLevel) {

            this.matchLevel =
                    matchLevel;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(
                String reason) {

            this.reason =
                    reason;
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
    }
}