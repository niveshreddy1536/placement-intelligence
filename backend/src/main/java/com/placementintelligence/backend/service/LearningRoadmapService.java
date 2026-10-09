package com.placementintelligence.backend.service;

import com.placementintelligence.backend.entity.JobDescription;
import com.placementintelligence.backend.entity.Skill;
import com.placementintelligence.backend.entity.Student;
import com.placementintelligence.backend.repository.JobDescriptionRepository;
import com.placementintelligence.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Builds a personalized, local/free learning roadmap from:
 * 1. The student's existing skills
 * 2. The target job's required skills
 *
 * No external AI/API is required.
 */
@Service
public class LearningRoadmapService {

    private final StudentRepository studentRepository;
    private final JobDescriptionRepository jobDescriptionRepository;

    public LearningRoadmapService(
            StudentRepository studentRepository,
            JobDescriptionRepository jobDescriptionRepository) {

        this.studentRepository = studentRepository;
        this.jobDescriptionRepository = jobDescriptionRepository;
    }

    public RoadmapResult generateRoadmap(
            Long studentId,
            Long jobDescriptionId) {

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() ->
                        new RuntimeException("Student not found"));

        JobDescription jobDescription =
                jobDescriptionRepository.findById(jobDescriptionId)
                        .orElseThrow(() ->
                                new RuntimeException("Job description not found"));

        Set<String> studentSkills =
                extractStudentSkills(student);

        Set<String> requiredSkills =
                parseRequiredSkills(jobDescription);

        List<String> matchedSkills =
                new ArrayList<>();

        List<String> missingSkills =
                new ArrayList<>();

        for (String required : requiredSkills) {

            if (isSkillMatched(required, studentSkills)) {
                matchedSkills.add(required);
            } else {
                missingSkills.add(required);
            }
        }

        List<RoadmapPhase> phases =
                buildPhases(
                        jobDescription.getTitle(),
                        missingSkills,
                        studentSkills
                );

        RoadmapResult result =
                new RoadmapResult();

        result.setStudentId(studentId);
        result.setJobDescriptionId(jobDescriptionId);
        result.setJobTitle(jobDescription.getTitle());

        result.setStudentSkills(
                new ArrayList<>(studentSkills)
        );

        result.setRequiredSkills(
                new ArrayList<>(requiredSkills)
        );

        result.setMatchedSkills(matchedSkills);
        result.setMissingSkills(missingSkills);

        result.setSkillCoverage(
                requiredSkills.isEmpty()
                        ? 0.0
                        : round(
                        ((double) matchedSkills.size()
                         / requiredSkills.size())
                        * 100.0
                )
        );

        result.setTotalPhases(phases.size());
        result.setPhases(phases);

        result.setEstimatedCompletion(
                estimateCompletion(phases)
        );

        result.setRoadmapSummary(
                generateSummary(
                        jobDescription.getTitle(),
                        missingSkills,
                        phases
                )
        );

        return result;
    }

    /*
     * =============================================================
     * STUDENT SKILLS
     * =============================================================
     */

    private Set<String> extractStudentSkills(
            Student student) {

        Set<String> skills =
                new LinkedHashSet<>();

        if (student.getSkills() == null) {
            return skills;
        }

        for (Skill skill : student.getSkills()) {

            if (skill == null ||
                    skill.getName() == null ||
                    skill.getName().isBlank()) {
                continue;
            }

            skills.add(
                    normalizeDisplay(skill.getName())
            );
        }

        return skills;
    }

    /*
     * =============================================================
     * REQUIRED SKILLS
     * =============================================================
     */

    private Set<String> parseRequiredSkills(
            JobDescription jobDescription) {

        Set<String> skills =
                new LinkedHashSet<>();

        String extracted =
                jobDescription.getExtractedSkills();

        if (extracted != null &&
                !extracted.isBlank()) {

            skills.addAll(
                    parseCommaSeparated(extracted)
            );
        }

        /*
         * Fallback for a job description that has not yet
         * been analyzed by JobDescriptionAnalyzerService.
         */
        if (skills.isEmpty()) {

            String text =
                    safeLower(
                            (jobDescription.getTitle() == null
                                    ? ""
                                    : jobDescription.getTitle())
                                    + " "
                                    + (jobDescription.getDescription() == null
                                    ? ""
                                    : jobDescription.getDescription())
                    );

            skills.addAll(
                    inferSkillsFromText(text)
            );
        }

        return skills;
    }

    private Set<String> parseCommaSeparated(
            String text) {

        Set<String> skills =
                new LinkedHashSet<>();

        for (String part :
                text.split(",")) {

            String skill =
                    normalizeDisplay(part);

            if (!skill.isBlank()) {
                skills.add(skill);
            }
        }

        return skills;
    }

    /*
     * =============================================================
     * SKILL MATCHING
     * =============================================================
     */

    private boolean isSkillMatched(
            String requiredSkill,
            Set<String> studentSkills) {

        String required =
                safeLower(requiredSkill);

        for (String studentSkill :
                studentSkills) {

            String student =
                    safeLower(studentSkill);

            if (required.equals(student)) {
                return true;
            }

            if (required.equals("rest api") &&
                    (student.equals("api development") ||
                            student.contains("rest"))) {
                return true;
            }

            if (required.equals("spring boot") &&
                    student.equals("spring")) {
                return true;
            }

            if (required.equals("postgresql") &&
                    student.equals("sql")) {
                return true;
            }

            if (required.equals("mysql") &&
                    student.equals("sql")) {
                return true;
            }

            if (required.equals("github") &&
                    student.equals("git")) {
                return true;
            }
        }

        return false;
    }

    /*
     * =============================================================
     * ROADMAP BUILDING
     * =============================================================
     */

    private List<RoadmapPhase> buildPhases(
            String jobTitle,
            List<String> missingSkills,
            Set<String> studentSkills) {

        List<RoadmapPhase> phases =
                new ArrayList<>();

        String title =
                safeLower(jobTitle);

        boolean backendRole =
                containsAny(
                        title,
                        "backend",
                        "back-end",
                        "java developer",
                        "software engineer"
                );

        /*
         * Java Backend roadmap.
         */
        if (backendRole) {

            addIfNeeded(
                    phases,
                    missingSkills,
                    "Spring Boot",
                    1,
                    "Spring Boot Fundamentals",
                    "Learn dependency injection, configuration, controllers, services and application structure.",
                    Arrays.asList(
                            "Spring Boot project structure",
                            "Dependency Injection",
                            "Controllers and Services",
                            "Configuration and application properties"
                    ),
                    5
            );

            addIfNeeded(
                    phases,
                    missingSkills,
                    "REST API",
                    2,
                    "REST API Development",
                    "Learn how backend applications expose HTTP endpoints and exchange JSON data.",
                    Arrays.asList(
                            "HTTP methods and status codes",
                            "GET, POST, PUT and DELETE",
                            "Request and response handling",
                            "Validation and exception handling"
                    ),
                    5
            );

            addIfNeeded(
                    phases,
                    missingSkills,
                    "JPA",
                    3,
                    "Database Access with JPA",
                    "Learn how Spring applications persist Java objects using JPA and Hibernate.",
                    Arrays.asList(
                            "Entities and relationships",
                            "Repositories",
                            "CRUD operations",
                            "JPQL and query methods"
                    ),
                    5
            );

            if (containsAnySkill(
                    missingSkills,
                    "PostgreSQL",
                    "MySQL",
                    "SQL"
            )) {

                addIfNeeded(
                        phases,
                        missingSkills,
                        "SQL",
                        4,
                        "Backend Database Integration",
                        "Strengthen relational database skills and connect the backend application to a real database.",
                        Arrays.asList(
                                "Tables and relationships",
                                "Joins and indexes",
                                "CRUD queries",
                                "Spring Boot database configuration"
                        ),
                        4
                );
            }

            if (containsAnySkill(
                    missingSkills,
                    "Git",
                    "GitHub"
            )) {

                addIfNeeded(
                        phases,
                        missingSkills,
                        "Git",
                        5,
                        "Git and GitHub Workflow",
                        "Learn a professional version-control workflow for backend development.",
                        Arrays.asList(
                                "Branches",
                                "Commits",
                                "Pull requests",
                                "Repository documentation"
                        ),
                        3
                );
            }
        }

        /*
         * Generic missing-skill phases.
         */
        int nextOrder =
                phases.size() + 1;

        for (String skill :
                missingSkills) {

            if (isAlreadyCoveredByRoadmap(
                    skill,
                    phases)) {
                continue;
            }

            RoadmapPhase phase =
                    createGenericPhase(
                            skill,
                            nextOrder++
                    );

            phases.add(phase);
        }

        /*
         * Even if there are no missing skills, give the user
         * an advancement roadmap rather than an empty response.
         */
        if (phases.isEmpty()) {

            phases.add(
                    createAdvancedPhase(
                            jobTitle
                    )
            );
        }

        /*
         * Always finish with a practical project phase.
         */
        phases.add(
                createProjectPhase(
                        jobTitle,
                        missingSkills,
                        phases.size() + 1
                )
        );

        return phases;
    }

    private void addIfNeeded(
            List<RoadmapPhase> phases,
            List<String> missingSkills,
            String skillName,
            int order,
            String title,
            String objective,
            List<String> topics,
            int estimatedWeeks) {

        if (!containsSkill(
                missingSkills,
                skillName)) {
            return;
        }

        RoadmapPhase phase =
                new RoadmapPhase();

        phase.setPhase(order);
        phase.setTitle(title);
        phase.setSkill(skillName);
        phase.setPriority(
                priorityForSkill(skillName)
        );
        phase.setObjective(objective);
        phase.setTopics(topics);
        phase.setEstimatedWeeks(
                estimatedWeeks
        );
        phase.setCompletionCriteria(
                "Build a small working example and demonstrate the skill in a project."
        );

        phases.add(phase);
    }

    private RoadmapPhase createGenericPhase(
            String skill,
            int order) {

        RoadmapPhase phase =
                new RoadmapPhase();

        phase.setPhase(order);
        phase.setTitle(
                "Learn " + skill
        );
        phase.setSkill(skill);
        phase.setPriority(
                priorityForSkill(skill)
        );
        phase.setObjective(
                "Build practical knowledge of "
                        + skill
                        + " and apply it in a small implementation."
        );
        phase.setTopics(
                Arrays.asList(
                        skill + " fundamentals",
                        "Common development patterns",
                        "Hands-on implementation",
                        "Best practices"
                )
        );
        phase.setEstimatedWeeks(4);
        phase.setCompletionCriteria(
                "Complete a small working implementation using "
                        + skill
                        + " and document what you learned."
        );

        return phase;
    }

    private RoadmapPhase createAdvancedPhase(
            String jobTitle) {

        RoadmapPhase phase =
                new RoadmapPhase();

        phase.setPhase(1);
        phase.setTitle(
                "Strengthen Advanced "
                        + (jobTitle == null
                        ? "Role Skills"
                        : jobTitle)
        );
        phase.setSkill("Advanced Role Skills");
        phase.setPriority("MEDIUM");
        phase.setObjective(
                "Your current skills cover the detected requirements. "
                        + "Focus on deeper implementation, testing, architecture and deployment."
        );
        phase.setTopics(
                Arrays.asList(
                        "Advanced implementation",
                        "Testing",
                        "System design",
                        "Performance and deployment"
                )
        );
        phase.setEstimatedWeeks(4);
        phase.setCompletionCriteria(
                "Build and explain a production-style project for the target role."
        );

        return phase;
    }

    private RoadmapPhase createProjectPhase(
            String jobTitle,
            List<String> missingSkills,
            int order) {

        RoadmapPhase phase =
                new RoadmapPhase();

        String title =
                jobTitle == null ||
                        jobTitle.isBlank()
                        ? "Target Role Project"
                        : jobTitle + " Project";

        phase.setPhase(order);
        phase.setTitle(
                "Build a " + title
        );
        phase.setSkill(
                missingSkills.isEmpty()
                        ? "Role-specific project"
                        : String.join(
                        ", ",
                        missingSkills
                )
        );
        phase.setPriority("HIGH");
        phase.setObjective(
                "Turn the learned skills into strong, role-specific evidence that can be added to the resume."
        );

        phase.setTopics(
                buildProjectTopics(
                        jobTitle,
                        missingSkills
                )
        );

        phase.setEstimatedWeeks(3);

        phase.setCompletionCriteria(
                "Finish the project, push it to GitHub, document the architecture and add it to the resume."
        );

        return phase;
    }

    private List<String> buildProjectTopics(
            String jobTitle,
            List<String> missingSkills) {

        List<String> topics =
                new ArrayList<>();

        String title =
                safeLower(jobTitle);

        if (containsAny(
                title,
                "java",
                "backend"
        )) {

            topics.add(
                    "Spring Boot backend architecture"
            );
            topics.add(
                    "REST API endpoints"
            );
            topics.add(
                    "Database integration"
            );
            topics.add(
                    "Validation and exception handling"
            );
            topics.add(
                    "Testing and GitHub documentation"
            );

        } else {

            topics.add(
                    "Role-specific application design"
            );
            topics.add(
                    "Implementation of missing skills"
            );
            topics.add(
                    "Testing and validation"
            );
            topics.add(
                    "GitHub documentation"
            );
        }

        if (!missingSkills.isEmpty()) {
            topics.add(
                    "Demonstrate: "
                            + String.join(
                            ", ",
                            missingSkills
                    )
            );
        }

        return topics;
    }

    /*
     * =============================================================
     * ESTIMATION
     * =============================================================
     */

    private String estimateCompletion(
            List<RoadmapPhase> phases) {

        int weeks = 0;

        for (RoadmapPhase phase :
                phases) {
            weeks +=
                    phase.getEstimatedWeeks();
        }

        if (weeks <= 0) {
            return "Flexible";
        }

        if (weeks == 1) {
            return "Approximately 1 week";
        }

        return "Approximately "
                + weeks
                + " weeks";
    }

    private String generateSummary(
            String jobTitle,
            List<String> missingSkills,
            List<RoadmapPhase> phases) {

        String role =
                jobTitle == null ||
                        jobTitle.isBlank()
                        ? "the target role"
                        : jobTitle;

        if (missingSkills.isEmpty()) {

            return "Your current skills cover the detected requirements for "
                    + role
                    + ". The roadmap focuses on advanced practice and building stronger role-specific project evidence.";
        }

        return "Your roadmap prioritizes "
                + String.join(
                ", ",
                missingSkills
        )
                + " before applying those skills in a "
                + role
                + " project.";
    }

    /*
     * =============================================================
     * FALLBACK SKILL INFERENCE
     * =============================================================
     */

    private Set<String> inferSkillsFromText(
            String text) {

        Set<String> skills =
                new LinkedHashSet<>();

        addSkillIfPresent(
                text,
                skills,
                "Java",
                "java"
        );

        addSkillIfPresent(
                text,
                skills,
                "Spring Boot",
                "spring boot"
        );

        addSkillIfPresent(
                text,
                skills,
                "REST API",
                "rest api",
                "restful api",
                "rest services"
        );

        addSkillIfPresent(
                text,
                skills,
                "SQL",
                "sql"
        );

        addSkillIfPresent(
                text,
                skills,
                "PostgreSQL",
                "postgresql"
        );

        addSkillIfPresent(
                text,
                skills,
                "MySQL",
                "mysql"
        );

        addSkillIfPresent(
                text,
                skills,
                "Python",
                "python"
        );

        addSkillIfPresent(
                text,
                skills,
                "React",
                "react"
        );

        addSkillIfPresent(
                text,
                skills,
                "JavaScript",
                "javascript"
        );

        addSkillIfPresent(
                text,
                skills,
                "HTML",
                "html"
        );

        addSkillIfPresent(
                text,
                skills,
                "CSS",
                "css"
        );

        addSkillIfPresent(
                text,
                skills,
                "Git",
                "git"
        );

        addSkillIfPresent(
                text,
                skills,
                "GitHub",
                "github"
        );

        addSkillIfPresent(
                text,
                skills,
                "Docker",
                "docker"
        );

        addSkillIfPresent(
                text,
                skills,
                "Kubernetes",
                "kubernetes"
        );

        addSkillIfPresent(
                text,
                skills,
                "Machine Learning",
                "machine learning"
        );

        addSkillIfPresent(
                text,
                skills,
                "NLP",
                "nlp",
                "natural language processing"
        );

        addSkillIfPresent(
                text,
                skills,
                "Computer Vision",
                "computer vision"
        );

        return skills;
    }

    private void addSkillIfPresent(
            String text,
            Set<String> skills,
            String displayName,
            String... variants) {

        for (String variant :
                variants) {

            if (text.contains(
                    safeLower(variant)
            )) {

                skills.add(displayName);
                return;
            }
        }
    }

    /*
     * =============================================================
     * HELPERS
     * =============================================================
     */

    private boolean containsSkill(
            List<String> skills,
            String target) {

        for (String skill : skills) {

            if (safeLower(skill)
                    .equals(
                            safeLower(target)
                    )) {
                return true;
            }
        }

        return false;
    }

    private boolean containsAnySkill(
            List<String> skills,
            String... targets) {

        for (String target :
                targets) {

            if (containsSkill(
                    skills,
                    target
            )) {
                return true;
            }
        }

        return false;
    }

    private boolean isAlreadyCoveredByRoadmap(
            String skill,
            List<RoadmapPhase> phases) {

        for (RoadmapPhase phase :
                phases) {

            if (phase.getSkill() != null &&
                    phase.getSkill().equalsIgnoreCase(skill)) {
                return true;
            }
        }

        return false;
    }

    private String priorityForSkill(
            String skill) {

        String lower =
                safeLower(skill);

        if (containsAny(
                lower,
                "spring boot",
                "rest api",
                "java",
                "python",
                "react",
                "javascript",
                "sql",
                "machine learning",
                "nlp"
        )) {
            return "HIGH";
        }

        if (containsAny(
                lower,
                "docker",
                "kubernetes",
                "github",
                "git",
                "postgresql",
                "mysql"
        )) {
            return "MEDIUM";
        }

        return "LOW";
    }

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

    private String safeLower(
            String text) {

        if (text == null) {
            return "";
        }

        return text
                .toLowerCase(Locale.ROOT)
                .trim();
    }

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

        if (value.equalsIgnoreCase("sql")) {
            return "SQL";
        }

        if (value.equalsIgnoreCase("rest api")) {
            return "REST API";
        }

        if (value.equalsIgnoreCase("spring boot")) {
            return "Spring Boot";
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

        return value;
    }

    private double round(
            double value) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }

    /*
     * =============================================================
     * DTOs
     * =============================================================
     */

    public static class RoadmapResult {

        private Long studentId;
        private Long jobDescriptionId;
        private String jobTitle;

        private List<String> studentSkills;
        private List<String> requiredSkills;
        private List<String> matchedSkills;
        private List<String> missingSkills;

        private double skillCoverage;

        private int totalPhases;
        private String estimatedCompletion;
        private String roadmapSummary;

        private List<RoadmapPhase> phases;

        public Long getStudentId() {
            return studentId;
        }

        public void setStudentId(Long studentId) {
            this.studentId = studentId;
        }

        public Long getJobDescriptionId() {
            return jobDescriptionId;
        }

        public void setJobDescriptionId(
                Long jobDescriptionId) {
            this.jobDescriptionId =
                    jobDescriptionId;
        }

        public String getJobTitle() {
            return jobTitle;
        }

        public void setJobTitle(String jobTitle) {
            this.jobTitle = jobTitle;
        }

        public List<String> getStudentSkills() {
            return studentSkills;
        }

        public void setStudentSkills(
                List<String> studentSkills) {
            this.studentSkills =
                    studentSkills;
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

        public double getSkillCoverage() {
            return skillCoverage;
        }

        public void setSkillCoverage(
                double skillCoverage) {
            this.skillCoverage =
                    skillCoverage;
        }

        public int getTotalPhases() {
            return totalPhases;
        }

        public void setTotalPhases(int totalPhases) {
            this.totalPhases =
                    totalPhases;
        }

        public String getEstimatedCompletion() {
            return estimatedCompletion;
        }

        public void setEstimatedCompletion(
                String estimatedCompletion) {
            this.estimatedCompletion =
                    estimatedCompletion;
        }

        public String getRoadmapSummary() {
            return roadmapSummary;
        }

        public void setRoadmapSummary(
                String roadmapSummary) {
            this.roadmapSummary =
                    roadmapSummary;
        }

        public List<RoadmapPhase> getPhases() {
            return phases;
        }

        public void setPhases(
                List<RoadmapPhase> phases) {
            this.phases = phases;
        }
    }

    public static class RoadmapPhase {

        private int phase;
        private String title;
        private String skill;
        private String priority;
        private String objective;
        private List<String> topics;
        private int estimatedWeeks;
        private String completionCriteria;

        public int getPhase() {
            return phase;
        }

        public void setPhase(int phase) {
            this.phase = phase;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSkill() {
            return skill;
        }

        public void setSkill(String skill) {
            this.skill = skill;
        }

        public String getPriority() {
            return priority;
        }

        public void setPriority(String priority) {
            this.priority = priority;
        }

        public String getObjective() {
            return objective;
        }

        public void setObjective(String objective) {
            this.objective = objective;
        }

        public List<String> getTopics() {
            return topics;
        }

        public void setTopics(List<String> topics) {
            this.topics = topics;
        }

        public int getEstimatedWeeks() {
            return estimatedWeeks;
        }

        public void setEstimatedWeeks(
                int estimatedWeeks) {
            this.estimatedWeeks =
                    estimatedWeeks;
        }

        public String getCompletionCriteria() {
            return completionCriteria;
        }

        public void setCompletionCriteria(
                String completionCriteria) {
            this.completionCriteria =
                    completionCriteria;
        }
    }
}
