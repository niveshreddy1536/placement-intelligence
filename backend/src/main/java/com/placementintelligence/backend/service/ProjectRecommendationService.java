package com.placementintelligence.backend.service;

import com.placementintelligence.backend.entity.JobDescription;
import com.placementintelligence.backend.entity.Skill;
import com.placementintelligence.backend.entity.Student;
import com.placementintelligence.backend.repository.JobDescriptionRepository;
import com.placementintelligence.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ProjectRecommendationService {

    private final StudentRepository studentRepository;
    private final JobDescriptionRepository jobDescriptionRepository;

    public ProjectRecommendationService(
            StudentRepository studentRepository,
            JobDescriptionRepository jobDescriptionRepository) {

        this.studentRepository = studentRepository;
        this.jobDescriptionRepository = jobDescriptionRepository;
    }

    public ProjectRecommendationResult recommendProject(
            Long studentId,
            Long jobDescriptionId) {

        Student student =
                studentRepository.findById(studentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Student not found"
                                )
                        );

        JobDescription jobDescription =
                jobDescriptionRepository.findById(jobDescriptionId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Job description not found"
                                )
                        );

        Set<String> studentSkills =
                getStudentSkills(student);

        Set<String> requiredSkills =
                getRequiredSkills(jobDescription);

        List<String> matchedSkills =
                new ArrayList<>();

        List<String> missingSkills =
                new ArrayList<>();

        for (String requiredSkill :
                requiredSkills) {

            if (skillMatches(
                    requiredSkill,
                    studentSkills
            )) {
                matchedSkills.add(requiredSkill);
            } else {
                missingSkills.add(requiredSkill);
            }
        }

        ProjectRecommendation recommendation =
                createRecommendation(
                        jobDescription.getTitle(),
                        jobDescription.getDescription(),
                        missingSkills,
                        studentSkills
                );

        recommendation.setMissingSkills(
                missingSkills
        );

        recommendation.setMatchedSkills(
                matchedSkills
        );

        recommendation.setSkillGapCount(
                missingSkills.size()
        );

        ProjectRecommendationResult result =
                new ProjectRecommendationResult();

        result.setStudentId(studentId);
        result.setJobDescriptionId(jobDescriptionId);
        result.setJobTitle(jobDescription.getTitle());

        result.setStudentSkills(
                new ArrayList<>(studentSkills)
        );

        result.setRequiredSkills(
                new ArrayList<>(requiredSkills)
        );

        result.setMatchedSkills(
                matchedSkills
        );

        result.setMissingSkills(
                missingSkills
        );

        result.setRecommendation(
                recommendation
        );

        return result;
    }

    /*
     * =============================================================
     * STUDENT SKILLS
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
     * REQUIRED SKILLS
     * =============================================================
     */

    private Set<String> getRequiredSkills(
            JobDescription jobDescription) {

        Set<String> skills =
                new LinkedHashSet<>();

        String extracted =
                jobDescription.getExtractedSkills();

        if (extracted != null &&
                !extracted.isBlank()) {

            for (String part :
                    extracted.split(",")) {

                String skill =
                        normalizeDisplay(part);

                if (!skill.isBlank()) {
                    skills.add(skill);
                }
            }
        }

        /*
         * Fallback when the job description has not yet
         * been processed by JobDescriptionAnalyzerService.
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
                    inferSkills(text)
            );
        }

        return skills;
    }

    private Set<String> inferSkills(
            String text) {

        Set<String> skills =
                new LinkedHashSet<>();

        addIfPresent(
                text,
                skills,
                "Java",
                "java"
        );

        addIfPresent(
                text,
                skills,
                "Spring Boot",
                "spring boot"
        );

        addIfPresent(
                text,
                skills,
                "REST API",
                "rest api",
                "restful api",
                "rest services"
        );

        addIfPresent(
                text,
                skills,
                "SQL",
                "sql"
        );

        addIfPresent(
                text,
                skills,
                "PostgreSQL",
                "postgresql"
        );

        addIfPresent(
                text,
                skills,
                "MySQL",
                "mysql"
        );

        addIfPresent(
                text,
                skills,
                "Python",
                "python"
        );

        addIfPresent(
                text,
                skills,
                "React",
                "react"
        );

        addIfPresent(
                text,
                skills,
                "JavaScript",
                "javascript"
        );

        addIfPresent(
                text,
                skills,
                "Git",
                "git"
        );

        addIfPresent(
                text,
                skills,
                "Docker",
                "docker"
        );

        addIfPresent(
                text,
                skills,
                "Kubernetes",
                "kubernetes"
        );

        addIfPresent(
                text,
                skills,
                "Machine Learning",
                "machine learning"
        );

        addIfPresent(
                text,
                skills,
                "NLP",
                "nlp",
                "natural language processing"
        );

        addIfPresent(
                text,
                skills,
                "Computer Vision",
                "computer vision"
        );

        return skills;
    }

    private void addIfPresent(
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
     * PROJECT SELECTION
     * =============================================================
     */

    private ProjectRecommendation createRecommendation(
            String jobTitle,
            String description,
            List<String> missingSkills,
            Set<String> studentSkills) {

        String title =
                safeLower(jobTitle);

        if (containsAny(
                title,
                "java backend",
                "java back-end",
                "backend developer",
                "back-end developer",
                "backend engineer",
                "back-end engineer"
        )) {

            return createJavaBackendProject(
                    missingSkills,
                    studentSkills
            );
        }

        if (containsAny(
                title,
                "full stack",
                "fullstack"
        )) {

            return createFullStackProject(
                    missingSkills
            );
        }

        if (containsAny(
                title,
                "frontend",
                "front-end",
                "ui developer"
        )) {

            return createFrontendProject(
                    missingSkills
            );
        }

        if (containsAny(
                title,
                "machine learning",
                "ml engineer",
                "ml developer",
                "data scientist"
        )) {

            return createMlProject(
                    missingSkills
            );
        }

        if (containsAny(
                title,
                "nlp",
                "natural language"
        )) {

            return createNlpProject(
                    missingSkills
            );
        }

        if (containsAny(
                title,
                "computer vision",
                "vision engineer",
                "vision developer"
        )) {

            return createComputerVisionProject(
                    missingSkills
            );
        }

        if (containsAny(
                title,
                "devops",
                "dev ops",
                "cloud engineer"
        )) {

            return createDevOpsProject(
                    missingSkills
            );
        }

        return createGenericProject(
                jobTitle,
                missingSkills
        );
    }

    /*
     * =============================================================
     * JAVA BACKEND PROJECT
     * =============================================================
     */

    private ProjectRecommendation createJavaBackendProject(
            List<String> missingSkills,
            Set<String> studentSkills) {

        ProjectRecommendation project =
                new ProjectRecommendation();

        project.setTitle(
                "Placement Management REST API"
        );

        project.setDifficulty(
                "Intermediate"
        );

        project.setRoleFit(
                "Excellent"
        );

        project.setPriority(
                "HIGH"
        );

        project.setWhyRecommended(
                "This project directly targets the Java backend skill gaps "
                        + "while allowing you to reuse your existing technical experience."
        );

        project.setResumeValue(
                "High"
        );

        project.setDescription(
                "Build a production-style placement management backend "
                        + "where students, companies, jobs and applications "
                        + "are managed through REST APIs."
        );

        project.setTechnologies(
                Arrays.asList(
                        "Java",
                        "Spring Boot",
                        "REST API",
                        "Spring Data JPA",
                        "PostgreSQL",
                        "Git",
                        "GitHub"
                )
        );

        project.setFeatures(
                Arrays.asList(
                        "Student registration and profile management",
                        "Company and job management",
                        "Job application tracking",
                        "Skill-based job matching",
                        "CRUD REST APIs",
                        "PostgreSQL persistence",
                        "Request validation",
                        "Global exception handling",
                        "Pagination and filtering",
                        "API documentation"
                )
        );

        project.setSkillsDemonstrated(
                Arrays.asList(
                        "Java",
                        "Spring Boot",
                        "REST API",
                        "JPA/Hibernate",
                        "SQL/PostgreSQL",
                        "Backend architecture",
                        "Git/GitHub"
                )
        );

        project.setBuildSteps(
                Arrays.asList(
                        "Create the Spring Boot project",
                        "Design Student, Company, Job and Application entities",
                        "Create JPA repositories and service layers",
                        "Build REST controllers and CRUD endpoints",
                        "Connect PostgreSQL",
                        "Add validation and exception handling",
                        "Add job matching logic",
                        "Write tests",
                        "Document and push the project to GitHub"
                )
        );

        project.setResumeBullet(
                "Developed a Spring Boot placement management REST API with "
                        + "PostgreSQL, JPA/Hibernate, CRUD operations, validation "
                        + "and skill-based job matching."
        );

        if (containsSkill(
                missingSkills,
                "Spring Boot"
        ) || containsSkill(
                missingSkills,
                "REST API"
        )) {

            project.setGapClosure(
                    "Directly closes the highest-priority Spring Boot and REST API gaps."
            );

        } else {

            project.setGapClosure(
                    "Strengthens backend architecture, persistence, testing and deployment skills."
            );
        }

        return project;
    }

    /*
     * =============================================================
     * FULL STACK PROJECT
     * =============================================================
     */

    private ProjectRecommendation createFullStackProject(
            List<String> missingSkills) {

        ProjectRecommendation project =
                createBaseProject(
                        "Placement Intelligence Portal",
                        "Intermediate",
                        "High",
                        "Build a full-stack placement portal connecting a React frontend with a Spring Boot backend and PostgreSQL."
                );

        project.setTechnologies(
                Arrays.asList(
                        "React",
                        "JavaScript",
                        "Spring Boot",
                        "REST API",
                        "PostgreSQL",
                        "Git/GitHub"
                )
        );

        project.setFeatures(
                Arrays.asList(
                        "Student dashboard",
                        "Job search and filtering",
                        "Resume upload",
                        "Skill matching",
                        "Application tracking",
                        "REST API integration",
                        "Authentication"
                )
        );

        project.setSkillsDemonstrated(
                Arrays.asList(
                        "React",
                        "JavaScript",
                        "Spring Boot",
                        "REST API",
                        "PostgreSQL",
                        "Full-stack development"
                )
        );

        project.setBuildSteps(
                Arrays.asList(
                        "Create React frontend",
                        "Create Spring Boot REST backend",
                        "Design PostgreSQL schema",
                        "Connect frontend and backend",
                        "Implement job search and matching",
                        "Add authentication",
                        "Test and document the application"
                )
        );

        project.setResumeBullet(
                "Built a full-stack placement portal using React, Spring Boot REST APIs and PostgreSQL with job matching and application tracking."
        );

        project.setGapClosure(
                "Combines the missing frontend and backend capabilities into one portfolio project."
        );

        return project;
    }

    /*
     * =============================================================
     * FRONTEND PROJECT
     * =============================================================
     */

    private ProjectRecommendation createFrontendProject(
            List<String> missingSkills) {

        ProjectRecommendation project =
                createBaseProject(
                        "Job Search Analytics Dashboard",
                        "Intermediate",
                        "High",
                        "Build a responsive frontend dashboard for discovering jobs, analyzing skill gaps and tracking applications."
                );

        project.setTechnologies(
                Arrays.asList(
                        "React",
                        "JavaScript",
                        "HTML",
                        "CSS",
                        "REST API"
                )
        );

        project.setFeatures(
                Arrays.asList(
                        "Job search",
                        "Skill filters",
                        "Match-score visualization",
                        "Application tracker",
                        "Responsive dashboard",
                        "REST API integration"
                )
        );

        project.setSkillsDemonstrated(
                Arrays.asList(
                        "React",
                        "JavaScript",
                        "Responsive UI",
                        "REST API integration"
                )
        );

        project.setBuildSteps(
                Arrays.asList(
                        "Create React application",
                        "Design reusable components",
                        "Build job listing and filtering",
                        "Connect REST API",
                        "Create analytics dashboard",
                        "Add responsive design",
                        "Deploy and document the project"
                )
        );

        project.setResumeBullet(
                "Developed a responsive React job analytics dashboard with REST API integration, filtering and application tracking."
        );

        project.setGapClosure(
                "Provides practical frontend evidence for the target role."
        );

        return project;
    }

    /*
     * =============================================================
     * MACHINE LEARNING PROJECT
     * =============================================================
     */

    private ProjectRecommendation createMlProject(
            List<String> missingSkills) {

        ProjectRecommendation project =
                createBaseProject(
                        "End-to-End Job Recommendation ML System",
                        "Intermediate",
                        "High",
                        "Build a machine-learning recommendation system that ranks jobs based on student skills and job requirements."
                );

        project.setTechnologies(
                Arrays.asList(
                        "Python",
                        "Pandas",
                        "Scikit-learn",
                        "TF-IDF",
                        "Cosine Similarity"
                )
        );

        project.setFeatures(
                Arrays.asList(
                        "Resume preprocessing",
                        "Skill extraction",
                        "Job feature extraction",
                        "Similarity scoring",
                        "Job ranking",
                        "Evaluation metrics"
                )
        );

        project.setSkillsDemonstrated(
                Arrays.asList(
                        "Python",
                        "Machine Learning",
                        "NLP",
                        "Feature Engineering",
                        "Recommendation Systems"
                )
        );

        project.setBuildSteps(
                Arrays.asList(
                        "Collect or prepare job data",
                        "Preprocess resume and job text",
                        "Extract features",
                        "Build similarity model",
                        "Rank jobs",
                        "Evaluate recommendations",
                        "Document the pipeline"
                )
        );

        project.setResumeBullet(
                "Built an ML-based job recommendation system using text feature extraction and similarity scoring to rank opportunities by candidate fit."
        );

        project.setGapClosure(
                "Builds practical machine-learning and recommendation-system evidence."
        );

        return project;
    }

    /*
     * =============================================================
     * NLP PROJECT
     * =============================================================
     */

    private ProjectRecommendation createNlpProject(
            List<String> missingSkills) {

        ProjectRecommendation project =
                createBaseProject(
                        "Intelligent Resume and Job Analyzer",
                        "Intermediate",
                        "High",
                        "Build an NLP pipeline that extracts skills, sections and job requirements and produces candidate-job insights."
                );

        project.setTechnologies(
                Arrays.asList(
                        "Python",
                        "NLP",
                        "TF-IDF",
                        "Cosine Similarity",
                        "Scikit-learn"
                )
        );

        project.setFeatures(
                Arrays.asList(
                        "Resume text preprocessing",
                        "Skill extraction",
                        "Job requirement extraction",
                        "Similarity scoring",
                        "Skill-gap analysis"
                )
        );

        project.setSkillsDemonstrated(
                Arrays.asList(
                        "NLP",
                        "Python",
                        "Text Processing",
                        "Information Extraction",
                        "Similarity Analysis"
                )
        );

        project.setBuildSteps(
                Arrays.asList(
                        "Preprocess resume text",
                        "Extract structured sections",
                        "Build skill dictionary",
                        "Analyze job descriptions",
                        "Calculate similarity",
                        "Generate skill-gap output",
                        "Evaluate results"
                )
        );

        project.setResumeBullet(
                "Developed an NLP-based resume and job analyzer that extracts skills and requirements and calculates candidate-job similarity."
        );

        project.setGapClosure(
                "Creates direct NLP and text-analysis evidence for the target role."
        );

        return project;
    }

    /*
     * =============================================================
     * COMPUTER VISION PROJECT
     * =============================================================
     */

    private ProjectRecommendation createComputerVisionProject(
            List<String> missingSkills) {

        ProjectRecommendation project =
                createBaseProject(
                        "Real-Time Safety Monitoring System",
                        "Intermediate",
                        "High",
                        "Build a real-time computer-vision application that detects safety events and produces actionable alerts."
                );

        project.setTechnologies(
                Arrays.asList(
                        "Python",
                        "OpenCV",
                        "YOLO",
                        "Computer Vision"
                )
        );

        project.setFeatures(
                Arrays.asList(
                        "Real-time detection",
                        "Camera processing",
                        "Object detection",
                        "Event logging",
                        "Alert generation",
                        "Performance evaluation"
                )
        );

        project.setSkillsDemonstrated(
                Arrays.asList(
                        "Computer Vision",
                        "Python",
                        "YOLO",
                        "OpenCV",
                        "Real-Time Processing"
                )
        );

        project.setBuildSteps(
                Arrays.asList(
                        "Prepare dataset",
                        "Configure detection model",
                        "Build video-processing pipeline",
                        "Implement detection events",
                        "Add alerts and logging",
                        "Evaluate model performance",
                        "Document the system"
                )
        );

        project.setResumeBullet(
                "Developed a real-time computer-vision safety monitoring system using YOLO and OpenCV with event detection and alert generation."
        );

        project.setGapClosure(
                "Provides strong applied computer-vision evidence."
        );

        return project;
    }

    /*
     * =============================================================
     * DEVOPS / CLOUD PROJECT
     * =============================================================
     */

    private ProjectRecommendation createDevOpsProject(
            List<String> missingSkills) {

        ProjectRecommendation project =
                createBaseProject(
                        "Containerized Spring Boot Deployment Pipeline",
                        "Intermediate",
                        "High",
                        "Containerize a Spring Boot application and create a repeatable deployment workflow."
                );

        project.setTechnologies(
                Arrays.asList(
                        "Java",
                        "Spring Boot",
                        "Docker",
                        "Kubernetes",
                        "Git"
                )
        );

        project.setFeatures(
                Arrays.asList(
                        "Docker containerization",
                        "Environment configuration",
                        "Health checks",
                        "Kubernetes deployment",
                        "Scaling configuration",
                        "Deployment documentation"
                )
        );

        project.setSkillsDemonstrated(
                Arrays.asList(
                        "Docker",
                        "Kubernetes",
                        "CI/CD concepts",
                        "Deployment",
                        "Spring Boot"
                )
        );

        project.setBuildSteps(
                Arrays.asList(
                        "Containerize the application",
                        "Configure environment variables",
                        "Add health checks",
                        "Create Kubernetes manifests",
                        "Deploy the application",
                        "Document the deployment process"
                )
        );

        project.setResumeBullet(
                "Containerized and deployed a Spring Boot application using Docker and Kubernetes with health checks and environment-based configuration."
        );

        project.setGapClosure(
                "Provides hands-on deployment and DevOps evidence."
        );

        return project;
    }

    /*
     * =============================================================
     * GENERIC PROJECT
     * =============================================================
     */

    private ProjectRecommendation createGenericProject(
            String jobTitle,
            List<String> missingSkills) {

        String role =
                jobTitle == null ||
                        jobTitle.isBlank()
                        ? "Target Role"
                        : jobTitle;

        ProjectRecommendation project =
                createBaseProject(
                        role + " Portfolio Project",
                        "Intermediate",
                        "High",
                        "Build a practical project combining the most important requirements for the target role."
                );

        List<String> technologies =
                new ArrayList<>();

        technologies.addAll(
                missingSkills
        );

        if (technologies.isEmpty()) {

            technologies.add(
                    "Role-specific technologies"
            );
        }

        project.setTechnologies(
                technologies
        );

        project.setFeatures(
                Arrays.asList(
                        "Role-specific core functionality",
                        "Input validation",
                        "Error handling",
                        "Testing",
                        "Documentation",
                        "Deployment"
                )
        );

        project.setSkillsDemonstrated(
                new ArrayList<>(
                        missingSkills
                )
        );

        project.setBuildSteps(
                Arrays.asList(
                        "Design the project around the target role",
                        "Implement the missing skills",
                        "Add testing and validation",
                        "Document the architecture",
                        "Deploy the project"
                )
        );

        project.setResumeBullet(
                "Built a practical "
                        + role
                        + " project demonstrating role-specific technologies and production-oriented development practices."
        );

        project.setGapClosure(
                "Targets the detected missing skills for the job."
        );

        return project;
    }

    private ProjectRecommendation createBaseProject(
            String title,
            String difficulty,
            String priority,
            String description) {

        ProjectRecommendation project =
                new ProjectRecommendation();

        project.setTitle(title);
        project.setDifficulty(difficulty);
        project.setPriority(priority);
        project.setRoleFit("Excellent");
        project.setDescription(description);
        project.setResumeValue("High");

        return project;
    }

    /*
     * =============================================================
     * SKILL MATCHING
     * =============================================================
     */

    private boolean skillMatches(
            String required,
            Set<String> studentSkills) {

        String target =
                safeLower(required);

        for (String studentSkill :
                studentSkills) {

            String current =
                    safeLower(studentSkill);

            if (target.equals(current)) {
                return true;
            }

            if (target.equals("rest api") &&
                    (current.contains("rest") ||
                            current.equals("api development"))) {

                return true;
            }

            if (target.equals("spring boot") &&
                    current.equals("spring")) {

                return true;
            }

            if ((target.equals("postgresql") ||
                    target.equals("mysql")) &&
                    current.equals("sql")) {

                return true;
            }

            if (target.equals("github") &&
                    current.equals("git")) {

                return true;
            }
        }

        return false;
    }

    private boolean containsSkill(
            List<String> skills,
            String target) {

        for (String skill :
                skills) {

            if (safeLower(skill)
                    .equals(
                            safeLower(target)
                    )) {

                return true;
            }
        }

        return false;
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

        if (value.equalsIgnoreCase("spring boot")) {
            return "Spring Boot";
        }

        if (value.equalsIgnoreCase("rest api")) {
            return "REST API";
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
     * RESULT DTO
     * =============================================================
     */

    public static class ProjectRecommendationResult {

        private Long studentId;
        private Long jobDescriptionId;
        private String jobTitle;

        private List<String> studentSkills;
        private List<String> requiredSkills;
        private List<String> matchedSkills;
        private List<String> missingSkills;

        private ProjectRecommendation recommendation;

        public Long getStudentId() {
            return studentId;
        }

        public void setStudentId(
                Long studentId) {

            this.studentId =
                    studentId;
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

        public void setJobTitle(
                String jobTitle) {

            this.jobTitle =
                    jobTitle;
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

        public ProjectRecommendation getRecommendation() {
            return recommendation;
        }

        public void setRecommendation(
                ProjectRecommendation recommendation) {

            this.recommendation =
                    recommendation;
        }
    }

    /*
     * =============================================================
     * PROJECT DTO
     * =============================================================
     */

    public static class ProjectRecommendation {

        private String title;
        private String difficulty;
        private String roleFit;
        private String priority;
        private String description;
        private String whyRecommended;
        private String gapClosure;
        private String resumeValue;

        private List<String> missingSkills;
        private List<String> matchedSkills;
        private int skillGapCount;

        private List<String> technologies;
        private List<String> features;
        private List<String> skillsDemonstrated;
        private List<String> buildSteps;

        private String resumeBullet;

        public String getTitle() {
            return title;
        }

        public void setTitle(
                String title) {

            this.title = title;
        }

        public String getDifficulty() {
            return difficulty;
        }

        public void setDifficulty(
                String difficulty) {

            this.difficulty = difficulty;
        }

        public String getRoleFit() {
            return roleFit;
        }

        public void setRoleFit(
                String roleFit) {

            this.roleFit = roleFit;
        }

        public String getPriority() {
            return priority;
        }

        public void setPriority(
                String priority) {

            this.priority = priority;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(
                String description) {

            this.description = description;
        }

        public String getWhyRecommended() {
            return whyRecommended;
        }

        public void setWhyRecommended(
                String whyRecommended) {

            this.whyRecommended =
                    whyRecommended;
        }

        public String getGapClosure() {
            return gapClosure;
        }

        public void setGapClosure(
                String gapClosure) {

            this.gapClosure =
                    gapClosure;
        }

        public String getResumeValue() {
            return resumeValue;
        }

        public void setResumeValue(
                String resumeValue) {

            this.resumeValue =
                    resumeValue;
        }

        public List<String> getMissingSkills() {
            return missingSkills;
        }

        public void setMissingSkills(
                List<String> missingSkills) {

            this.missingSkills =
                    missingSkills;
        }

        public List<String> getMatchedSkills() {
            return matchedSkills;
        }

        public void setMatchedSkills(
                List<String> matchedSkills) {

            this.matchedSkills =
                    matchedSkills;
        }

        public int getSkillGapCount() {
            return skillGapCount;
        }

        public void setSkillGapCount(
                int skillGapCount) {

            this.skillGapCount =
                    skillGapCount;
        }

        public List<String> getTechnologies() {
            return technologies;
        }

        public void setTechnologies(
                List<String> technologies) {

            this.technologies =
                    technologies;
        }

        public List<String> getFeatures() {
            return features;
        }

        public void setFeatures(
                List<String> features) {

            this.features =
                    features;
        }

        public List<String> getSkillsDemonstrated() {
            return skillsDemonstrated;
        }

        public void setSkillsDemonstrated(
                List<String> skillsDemonstrated) {

            this.skillsDemonstrated =
                    skillsDemonstrated;
        }

        public List<String> getBuildSteps() {
            return buildSteps;
        }

        public void setBuildSteps(
                List<String> buildSteps) {

            this.buildSteps =
                    buildSteps;
        }

        public String getResumeBullet() {
            return resumeBullet;
        }

        public void setResumeBullet(
                String resumeBullet) {

            this.resumeBullet =
                    resumeBullet;
        }
    }
}