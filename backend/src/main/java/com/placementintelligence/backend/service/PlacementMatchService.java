package com.placementintelligence.backend.service;

import com.placementintelligence.backend.entity.JobDescription;
import com.placementintelligence.backend.entity.Resume;
import com.placementintelligence.backend.entity.ResumeAnalysis;
import com.placementintelligence.backend.entity.Skill;
import com.placementintelligence.backend.entity.Student;
import com.placementintelligence.backend.repository.JobDescriptionRepository;
import com.placementintelligence.backend.repository.ResumeAnalysisRepository;
import com.placementintelligence.backend.repository.ResumeRepository;
import com.placementintelligence.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class PlacementMatchService {

    private final StudentRepository studentRepository;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final LocalTextSimilarityService similarityService;

    public PlacementMatchService(
            StudentRepository studentRepository,
            JobDescriptionRepository jobDescriptionRepository,
            ResumeRepository resumeRepository,
            ResumeAnalysisRepository resumeAnalysisRepository,
            LocalTextSimilarityService similarityService) {

        this.studentRepository = studentRepository;
        this.jobDescriptionRepository = jobDescriptionRepository;
        this.resumeRepository = resumeRepository;
        this.resumeAnalysisRepository = resumeAnalysisRepository;
        this.similarityService = similarityService;
    }

    public MatchResult matchStudentWithJob(
            Long studentId,
            Long jobDescriptionId) {

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() ->
                        new RuntimeException("Student not found"));

        JobDescription jobDescription =
                jobDescriptionRepository.findById(jobDescriptionId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Job description not found"
                                ));

        /*
         * =========================================================
         * 1. FIND RESUME
         * =========================================================
         */

        Resume resume = resumeRepository.findAll()
                .stream()
                .filter(r ->
                        r.getStudent() != null
                                && r.getStudent().getId() != null
                                && r.getStudent().getId().equals(studentId))
                .findFirst()
                .orElse(null);

        /*
         * =========================================================
         * 2. FIND RESUME ANALYSIS
         * =========================================================
         */

        ResumeAnalysis resumeAnalysis = null;

        if (resume != null) {

            resumeAnalysis =
                    resumeAnalysisRepository
                            .findByResume(resume)
                            .orElse(null);
        }

        /*
         * =========================================================
         * 3. STUDENT SKILLS
         * =========================================================
         */

        Set<String> studentSkills =
                new LinkedHashSet<>();

        if (student.getSkills() != null) {

            for (Skill skill : student.getSkills()) {

                if (skill != null
                        && skill.getName() != null
                        && !skill.getName().isBlank()) {

                    studentSkills.add(
                            skill.getName().trim()
                    );
                }
            }
        }

        /*
         * =========================================================
         * 4. REQUIRED JOB SKILLS
         * =========================================================
         */

        Set<String> requiredSkills =
                deriveRequiredSkills(
                        jobDescription
                );

        /*
         * =========================================================
         * 5. COMPLETE JOB TEXT
         * =========================================================
         */

        String jobText =
                buildJobText(jobDescription);

        /*
         * =========================================================
         * 6. RESUME TEXT
         * =========================================================
         */

        String resumeText = "";

        if (resume != null) {

            resumeText =
                    safeText(
                            resume.getExtractedText()
                    );
        }

        /*
         * =========================================================
         * 7. MATCH REQUIRED SKILLS
         * =========================================================
         */

        List<String> matchedSkills =
                new ArrayList<>();

        List<String> missingSkills =
                new ArrayList<>();

        for (String requiredSkill :
                requiredSkills) {

            boolean matched = false;

            /*
             * First check structured student skills.
             */

            for (String studentSkill :
                    studentSkills) {

                if (skillsMatch(
                        studentSkill,
                        requiredSkill)) {

                    matched = true;
                    break;
                }
            }

            /*
             * Then check complete resume text.
             */

            if (!matched
                    && containsSkillEvidence(
                    resumeText,
                    requiredSkill)) {

                matched = true;
            }

            if (matched) {

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
         * 8. SKILL SCORE
         * =========================================================
         */

        double skillScore = 0;

        if (!requiredSkills.isEmpty()) {

            skillScore =
                    ((double) matchedSkills.size()
                            / requiredSkills.size())
                            * 100;
        }

        /*
         * =========================================================
         * 9. RESUME SECTIONS
         * =========================================================
         */

        String projects = "";
        String experience = "";
        String education = "";
        String certifications = "";
        String achievements = "";
        String summary = "";

        if (resumeAnalysis != null) {

            projects =
                    safeText(
                            resumeAnalysis.getProjects()
                    );

            experience =
                    safeText(
                            resumeAnalysis.getExperience()
                    );

            education =
                    safeText(
                            resumeAnalysis.getEducation()
                    );

            certifications =
                    safeText(
                            resumeAnalysis.getCertifications()
                    );

            achievements =
                    safeText(
                            resumeAnalysis.getAchievements()
                    );

            summary =
                    safeText(
                            resumeAnalysis.getSummary()
                    );
        }

        /*
         * =========================================================
         * 10. ROLE PROFILE
         * =========================================================
         */

        RoleProfile roleProfile =
                buildRoleProfile(
                        jobDescription.getTitle(),
                        jobText
                );

        /*
         * =========================================================
         * 11. PROJECT SIMILARITY
         * =========================================================
         */

        double projectTextSimilarity =
                similarityService.calculatePercentage(
                        projects,
                        jobText
                );

        /*
         * =========================================================
         * 12. EXPERIENCE SIMILARITY
         * =========================================================
         */

        double experienceTextSimilarity =
                similarityService.calculatePercentage(
                        experience,
                        jobText
                );

        /*
         * =========================================================
         * 13. SUMMARY SIMILARITY
         * =========================================================
         */

        double summarySimilarity =
                similarityService.calculatePercentage(
                        summary,
                        jobText
                );

        /*
         * =========================================================
         * 14. PROJECT EVIDENCE SCORE
         * =========================================================
         */

        double projectEvidenceScore =
                calculateProjectEvidenceScore(
                        projects,
                        requiredSkills,
                        roleProfile
                );

        double projectScore =
                (projectTextSimilarity * 0.35)
                        + (projectEvidenceScore * 0.65);

        projectScore =
                Math.min(
                        round(projectScore),
                        100
                );

        /*
         * =========================================================
         * 15. EXPERIENCE EVIDENCE SCORE
         * =========================================================
         */

        double experienceEvidenceScore =
                calculateExperienceEvidenceScore(
                        experience,
                        summary,
                        requiredSkills,
                        roleProfile
                );

        double experienceScore =
                (experienceTextSimilarity * 0.40)
                        + (experienceEvidenceScore * 0.60);

        /*
         * If structured experience extraction is weak,
         * summary still provides supporting evidence.
         */

        if (experienceScore < 10
                && summarySimilarity > experienceScore) {

            experienceScore =
                    (experienceScore * 0.70)
                            + (summarySimilarity * 0.30);
        }

        experienceScore =
                Math.min(
                        round(experienceScore),
                        100
                );

        /*
         * =========================================================
         * 16. EDUCATION + CERTIFICATION SCORE
         * =========================================================
         */

        double educationCertificationScore =
                calculateEducationCertificationScore(
                        education,
                        certifications,
                        achievements,
                        requiredSkills,
                        roleProfile
                );

        /*
         * =========================================================
         * 17. FIND RELEVANT PROJECTS
         * =========================================================
         */

        List<String> relevantProjects =
                findRelevantProjects(
                        projects,
                        resumeText,
                        jobText,
                        requiredSkills,
                        roleProfile
                );

        /*
         * Project relevance should represent the strongest actual projects,
         * not the similarity of the entire Projects section. This prevents
         * unrelated research work or many weak projects from diluting a
         * clearly relevant project.
         */
        if (!relevantProjects.isEmpty()) {
            projectScore =
                    calculateAggregateProjectScore(
                            relevantProjects
                    );
        }

        /*
         * =========================================================
         * 18. FINAL SCORE
         * =========================================================
         *
         * Skills                  = 50%
         * Projects                = 25%
         * Experience              = 20%
         * Education/Certification = 5%
         *
         * This keeps technical skill coverage as the
         * most important factor while allowing the
         * complete resume to influence the result.
         * =========================================================
         */

        double finalScore =
                (skillScore * 0.50)
                        + (projectScore * 0.25)
                        + (experienceScore * 0.20)
                        + (educationCertificationScore * 0.05);

        finalScore =
                Math.min(
                        round(finalScore),
                        100
                );

        /*
         * =========================================================
         * 19. STRENGTHS
         * =========================================================
         */

        List<String> strengths =
                generateStrengths(
                        matchedSkills,
                        studentSkills,
                        projects,
                        experience,
                        education,
                        relevantProjects,
                        roleProfile
                );

        /*
         * =========================================================
         * 20. RECOMMENDATIONS
         * =========================================================
         */

        List<String> recommendations =
                generateRecommendations(
                        missingSkills,
                        roleProfile
                );

        /*
         * =========================================================
         * 21. PROJECT RECOMMENDATION
         * =========================================================
         */

        String projectRecommendation =
                generateProjectRecommendation(
                        missingSkills,
                        jobDescription.getTitle(),
                        relevantProjects,
                        projects
                );

        /*
         * =========================================================
         * 22. BUILD RESULT
         * =========================================================
         */

        MatchResult result =
                new MatchResult();

        result.setStudentId(
                studentId
        );

        result.setJobDescriptionId(
                jobDescriptionId
        );

        result.setJobTitle(
                jobDescription.getTitle()
        );

        result.setMatchPercentage(
                finalScore
        );

        result.setSkillScore(
                round(skillScore)
        );

        result.setProjectScore(
                round(projectScore)
        );

        result.setExperienceScore(
                round(experienceScore)
        );

        result.setEducationCertificationScore(
                round(educationCertificationScore)
        );

        result.setStudentSkills(
                new ArrayList<>(
                        studentSkills
                )
        );

        result.setRequiredSkills(
                new ArrayList<>(
                        requiredSkills
                )
        );

        result.setMatchedSkills(
                matchedSkills
        );

        result.setMissingSkills(
                missingSkills
        );

        result.setStrengths(
                strengths
        );

        result.setRecommendations(
                recommendations
        );

        result.setRelevantProjects(
                relevantProjects
        );

        result.setProjectRecommendation(
                projectRecommendation
        );

        return result;
    }

    /*
     * =========================================================
     * BUILD JOB TEXT
     * =========================================================
     */

    private String buildCoreSkillText(
            JobDescription jobDescription) {

        StringBuilder text =
                new StringBuilder();

        if (jobDescription.getTitle() != null) {
            text.append(
                    jobDescription.getTitle()
            ).append(" ");
        }

        if (jobDescription.getDescription() != null) {
            text.append(
                    jobDescription.getDescription()
            );
        }

        return safeLower(
                text.toString()
        );
    }

    private String buildJobText(
            JobDescription jobDescription) {

        StringBuilder text =
                new StringBuilder();

        if (jobDescription.getTitle() != null) {

            text.append(
                    jobDescription.getTitle()
            ).append(" ");
        }

        if (jobDescription.getDescription() != null) {

            text.append(
                    jobDescription.getDescription()
            ).append(" ");
        }

        if (jobDescription.getExtractedResponsibilities()
                != null) {

            text.append(
                    jobDescription
                            .getExtractedResponsibilities()
            ).append(" ");
        }

        if (jobDescription.getExtractedRequirements()
                != null) {

            text.append(
                    jobDescription
                            .getExtractedRequirements()
            );
        }

        return safeLower(
                text.toString()
        );
    }

    /*
     * =========================================================
     * PARSE SKILLS
     * =========================================================
     */

    private Set<String> deriveRequiredSkills(
            JobDescription jobDescription) {

        Set<String> skills =
                new LinkedHashSet<>();

        // Core skill extraction must use only the current title and
        // description. Responsibilities/requirements can contain stale
        // values from an older version of the same JD.
        String jobText =
                buildCoreSkillText(jobDescription);

        /*
         * Treat the current title + description as the source of truth.
         * This prevents stale extracted skills from a previous edit
         * from appearing in recommendations.
         */
        String extracted =
                jobDescription.getExtractedSkills();

        if (extracted != null && !extracted.isBlank()) {
            for (String skill : parseSkills(extracted)) {
                if (containsSkillEvidence(jobText, skill)) {
                    skills.add(skill);
                }
            }
        }

        String text = safeLower(jobText);

        // Match Java as a standalone technology.
        // This prevents JavaScript from being incorrectly detected as Java.
        if (containsWholeWord(text, "java"))
            skills.add("Java");

        if (containsAnyText(text, "spring boot", "springboot"))
            skills.add("Spring Boot");

        if (containsAnyText(text, "rest api", "restful api", "rest services", "rest service", "rest endpoints"))
            skills.add("REST API");

        if (containsAnyText(text, "sql", "structured query language"))
            skills.add("SQL");

        if (containsAnyText(text, "postgresql", "postgres"))
            skills.add("PostgreSQL");

        if (containsAnyText(text, "python"))
            skills.add("Python");

        if (containsAnyText(text, "javascript", "java script"))
            skills.add("JavaScript");

        if (containsAnyText(text, "react", "react.js", "reactjs"))
            skills.add("React");

        if (containsAnyText(text, "html"))
            skills.add("HTML");

        if (containsAnyText(text, "css"))
            skills.add("CSS");

        if (containsAnyText(text, "git", "github"))
            skills.add("Git");

        if (containsAnyText(text, "docker"))
            skills.add("Docker");

        if (containsAnyText(text, "kubernetes"))
            skills.add("Kubernetes");

        if (containsAnyText(text, "machine learning", "machine-learning"))
            skills.add("Machine Learning");

        if (containsAnyText(text, "artificial intelligence", "ai engineer", "ai developer"))
            skills.add("Artificial Intelligence");

        if (containsAnyText(text, "nlp", "natural language processing"))
            skills.add("NLP");

        if (containsAnyText(text, "computer vision", "image processing", "image recognition"))
            skills.add("Computer Vision");

        /* Data Analyst / BI skills */
        if (containsAnyText(text, "excel", "microsoft excel"))
            skills.add("Excel");

        if (containsAnyText(text, "power bi", "powerbi"))
            skills.add("Power BI");

        if (containsAnyText(text, "tableau"))
            skills.add("Tableau");

        if (containsAnyText(text, "statistics", "statistical analysis", "statistical"))
            skills.add("Statistics");

        if (containsAnyText(text, "data visualization", "data visualisation", "visualization", "visualisation"))
            skills.add("Data Visualization");

        if (containsAnyText(text, "data analysis", "data analyst", "analyze datasets", "analyse datasets"))
            skills.add("Data Analysis");

        if (containsAnyText(text, "dashboard", "dashboards"))
            skills.add("Dashboarding");

        /*
         * Role-aware baseline skills.
         *
         * A very short job description can otherwise make a role look
         * artificially easy. For example, a "Java Backend Developer"
         * description containing only "Java" would give a candidate a
         * 100% skill score even when the role clearly involves backend
         * API/database work. These baseline skills are added only from
         * the job title, not from stale extracted JD fields.
         */
        String title = safeLower(jobDescription.getTitle());

        if (containsAnyText(
                title,
                "java backend",
                "java back-end",
                "backend developer",
                "back-end developer",
                "backend engineer",
                "back-end engineer")) {

            skills.add("Java");
            skills.add("Spring Boot");
            skills.add("REST API");
            skills.add("SQL");
        }

        if (containsAnyText(
                title,
                "frontend",
                "front-end")) {

            skills.add("JavaScript");
            skills.add("HTML");
            skills.add("CSS");
            skills.add("React");
        }

        return skills;
    }

    private Set<String> parseSkills(
            String skillsText) {

        Set<String> skills =
                new LinkedHashSet<>();

        if (skillsText == null
                || skillsText.isBlank()) {

            return skills;
        }

        String[] parts =
                skillsText.split(",");

        for (String part :
                parts) {

            String skill =
                    part.trim();

            if (!skill.isBlank()) {

                skills.add(skill);
            }
        }

        return skills;
    }

    /*
     * =========================================================
     * ROLE PROFILE
     * =========================================================
     */

    private RoleProfile buildRoleProfile(
            String jobTitle,
            String jobText) {

        RoleProfile profile =
                new RoleProfile();

        String title =
                safeLower(jobTitle);

        if (containsAnyText(
                title,
                "java backend",
                "java back-end",
                "backend developer",
                "back-end developer",
                "backend engineer",
                "back-end engineer")) {

            profile.role =
                    "JAVA_BACKEND";

            addKeywords(
                    profile,
                    "java",
                    "backend",
                    "api",
                    "rest",
                    "database",
                    "sql",
                    "server",
                    "web",
                    "crud",
                    "jpa",
                    "hibernate",
                    "application",
                    "system",
                    "integration",
                    "development"
            );
        }

        else if (containsAnyText(
                title,
                "backend",
                "back-end")) {

            profile.role =
                    "BACKEND";

            addKeywords(
                    profile,
                    "backend",
                    "api",
                    "rest",
                    "database",
                    "sql",
                    "server",
                    "web",
                    "system",
                    "integration",
                    "development"
            );
        }

        else if (containsAnyText(
                title,
                "frontend",
                "front-end")) {

            profile.role =
                    "FRONTEND";

            addKeywords(
                    profile,
                    "frontend",
                    "web",
                    "html",
                    "css",
                    "javascript",
                    "react",
                    "ui",
                    "interface",
                    "development"
            );
        }

        else if (containsAnyText(
                title,
                "full stack",
                "fullstack")) {

            profile.role =
                    "FULL_STACK";

            addKeywords(
                    profile,
                    "frontend",
                    "backend",
                    "web",
                    "api",
                    "database",
                    "javascript",
                    "react",
                    "sql",
                    "development"
            );
        }

        else if (containsAnyText(
                title,
                "data analyst",
                "data analytics",
                "business analyst",
                "bi analyst",
                "business intelligence")) {

            profile.role =
                    "DATA_ANALYST";

            addKeywords(
                    profile,
                    "data",
                    "analysis",
                    "analytics",
                    "sql",
                    "python",
                    "excel",
                    "power bi",
                    "tableau",
                    "statistics",
                    "visualization",
                    "dashboard",
                    "reporting",
                    "insights",
                    "business"
            );
        }

        else if (containsAnyText(
                title,
                "machine learning",
                "ml engineer",
                "ml developer")) {

            profile.role =
                    "MACHINE_LEARNING";

            addKeywords(
                    profile,
                    "python",
                    "machine learning",
                    "ml",
                    "model",
                    "data",
                    "training",
                    "prediction",
                    "classification",
                    "evaluation"
            );
        }

        else if (containsAnyText(
                title,
                "nlp",
                "natural language")) {

            profile.role =
                    "NLP";

            addKeywords(
                    profile,
                    "python",
                    "nlp",
                    "natural language",
                    "text",
                    "classification",
                    "sentiment",
                    "language",
                    "machine learning",
                    "model"
            );
        }

        else if (containsAnyText(
                title,
                "computer vision",
                "vision engineer",
                "vision developer")) {

            profile.role =
                    "COMPUTER_VISION";

            addKeywords(
                    profile,
                    "python",
                    "computer vision",
                    "image",
                    "object detection",
                    "yolo",
                    "opencv",
                    "model",
                    "machine learning"
            );
        }

        else if (containsAnyText(
                title,
                "artificial intelligence",
                "ai engineer",
                "ai developer")) {

            profile.role =
                    "AI";

            addKeywords(
                    profile,
                    "python",
                    "artificial intelligence",
                    "ai",
                    "machine learning",
                    "model",
                    "data",
                    "automation"
            );
        }

        else if (containsAnyText(
                title,
                "devops",
                "dev ops")) {

            profile.role =
                    "DEVOPS";

            addKeywords(
                    profile,
                    "docker",
                    "kubernetes",
                    "cloud",
                    "deployment",
                    "automation",
                    "linux",
                    "ci",
                    "cd"
            );
        }

        else {

            profile.role =
                    "SOFTWARE";

            addKeywords(
                    profile,
                    "software",
                    "development",
                    "programming",
                    "system",
                    "application",
                    "database",
                    "api"
            );
        }

        /*
         * Add concepts detected directly from the job.
         */

        if (jobText.contains("microservices")) {

            profile.keywords.add(
                    "microservices"
            );
        }

        if (jobText.contains("cloud")) {

            profile.keywords.add(
                    "cloud"
            );
        }

        if (jobText.contains("deployment")) {

            profile.keywords.add(
                    "deployment"
            );
        }

        if (jobText.contains("testing")
                || jobText.contains("unit test")) {

            profile.keywords.add(
                    "testing"
            );
        }

        if (jobText.contains("authentication")
                || jobText.contains("authorization")) {

            profile.keywords.add(
                    "authentication"
            );
        }

        return profile;
    }

    /*
     * =========================================================
     * ADD ROLE KEYWORDS
     * =========================================================
     */

    private void addKeywords(
            RoleProfile profile,
            String... keywords) {

        for (String keyword :
                keywords) {

            profile.keywords.add(
                    keyword
            );
        }
    }

    /*
     * =========================================================
     * PROJECT EVIDENCE SCORE
     * =========================================================
     */

    private double calculateProjectEvidenceScore(
            String projects,
            Set<String> requiredSkills,
            RoleProfile roleProfile) {

        if (projects == null
                || projects.isBlank()) {

            return 0;
        }

        int directMatches = 0;

        /*
         * Direct technical evidence.
         */
        for (String skill : requiredSkills) {

            if (containsSkillEvidence(
                    projects,
                    skill)) {

                directMatches++;
            }
        }

        double directScore = 0;

        if (!requiredSkills.isEmpty()) {

            directScore =
                    ((double) directMatches
                            / requiredSkills.size())
                            * 100;
        }

        /*
         * Role-specific project evidence.
         *
         * Generic keywords such as "web", "application" or
         * "development" are too broad to prove that a project
         * is actually relevant to the target role. These concept
         * groups therefore focus on the kind of implementation
         * expected for the role.
         */
        double roleEvidenceScore =
                calculateRoleProjectEvidenceScore(
                        projects,
                        roleProfile
                );

        double implementationBonus = 0;

        if (containsAnyText(
                projects,
                "developed",
                "implemented",
                "engineered",
                "built",
                "designed",
                "integrated",
                "deployed",
                "created")) {

            implementationBonus = 10;
        }

        /*
         * Direct skill coverage and role-specific project evidence
         * are equally important. The implementation bonus rewards
         * evidence that the student actually built something.
         */
        double score =
                (directScore * 0.45)
                        + (roleEvidenceScore * 0.45)
                        + implementationBonus;

        return Math.min(
                round(score),
                100
        );
    }

    /*
     * =========================================================
     * ROLE-SPECIFIC PROJECT EVIDENCE
     * =========================================================
     */

    private double calculateRoleProjectEvidenceScore(
            String projects,
            RoleProfile roleProfile) {

        if (projects == null || projects.isBlank() || roleProfile == null) {
            return 0;
        }

        String text = safeLower(projects);

        /*
         * Use weighted implementation evidence rather than requiring exact
         * phrases such as "react component". Real resumes often say
         * "built with React" or "responsive web interface" instead.
         */
        double score = 0;

        switch (roleProfile.role) {

            case "FRONTEND":
                score += containsAnyText(text,
                        "react", "react.js", "reactjs") ? 22 : 0;
                score += containsAnyText(text,
                        "javascript", "js functionality", "js") ? 14 : 0;
                score += containsAnyText(text,
                        "html", "html5") ? 8 : 0;
                score += containsAnyText(text,
                        "css", "css3", "stylesheet") ? 8 : 0;
                score += containsAnyText(text,
                        "rest api", "restful api", "api", "api integration", "backend api") ? 14 : 0;
                score += containsAnyText(text,
                        "responsive", "user interface", "ui", "frontend", "front end", "web interface", "web application") ? 10 : 0;
                score += containsAnyText(text,
                        "state management", "application state") ? 6 : 0;
                score += containsAnyText(text,
                        "git", "github", "vercel", "deployment", "deployed") ? 5 : 0;
                score += containsAnyText(text,
                        "testing", "test cases", "debugging") ? 5 : 0;
                score += containsAnyText(text,
                        "authentication", "jwt", "websocket", "websockets") ? 3 : 0;
                break;

            case "JAVA_BACKEND":
            case "BACKEND":
                score += containsAnyText(text,
                        "java", "spring boot") ? 22 : 0;
                score += containsAnyText(text,
                        "rest api", "restful api", "api development", "api") ? 16 : 0;
                score += containsAnyText(text,
                        "sql", "postgresql", "mysql", "database", "database operations") ? 16 : 0;
                score += containsAnyText(text,
                        "crud", "jpa", "hibernate") ? 12 : 0;
                score += containsAnyText(text,
                        "authentication", "authorization", "jwt") ? 10 : 0;
                score += containsAnyText(text,
                        "backend", "server side", "server-side") ? 9 : 0;
                score += containsAnyText(text,
                        "testing", "deployed", "deployment") ? 5 : 0;
                break;

            case "FULL_STACK":
                score += containsAnyText(text,
                        "react", "react.js", "javascript") ? 18 : 0;
                score += containsAnyText(text,
                        "rest api", "restful api", "api") ? 15 : 0;
                score += containsAnyText(text,
                        "backend", "node.js", "nodejs", "server side") ? 15 : 0;
                score += containsAnyText(text,
                        "database", "postgresql", "mysql", "supabase", "firebase") ? 12 : 0;
                score += containsAnyText(text,
                        "authentication", "authorization", "jwt") ? 10 : 0;
                score += containsAnyText(text,
                        "responsive", "user interface", "frontend") ? 10 : 0;
                score += containsAnyText(text,
                        "git", "github", "deployment", "deployed") ? 5 : 0;
                break;

            case "DATA_ANALYST":
                score += containsAnyText(text,
                        "data analysis", "data analytics", "analyzed data", "analysis") ? 20 : 0;
                score += containsAnyText(text,
                        "sql", "sql queries", "postgresql", "mysql") ? 16 : 0;
                score += containsAnyText(text,
                        "power bi", "tableau", "excel", "data visualization") ? 18 : 0;
                score += containsAnyText(text,
                        "dashboard", "reporting", "business insights") ? 14 : 0;
                score += containsAnyText(text,
                        "statistics", "statistical", "data cleaning", "data preprocessing") ? 10 : 0;
                score += containsAnyText(text,
                        "python", "pandas", "numpy") ? 7 : 0;
                score += containsAnyText(text,
                        "automated", "automation", "optimized") ? 5 : 0;
                break;

            case "MACHINE_LEARNING":
                score += containsAnyText(text,
                        "machine learning", "ml model", "model training") ? 22 : 0;
                score += containsAnyText(text,
                        "classification", "regression", "prediction") ? 15 : 0;
                score += containsAnyText(text,
                        "feature engineering", "data preprocessing") ? 15 : 0;
                score += containsAnyText(text,
                        "tensorflow", "pytorch", "scikit-learn", "sklearn") ? 14 : 0;
                score += containsAnyText(text,
                        "model evaluation", "accuracy", "evaluation") ? 12 : 0;
                score += containsAnyText(text,
                        "python", "pandas", "numpy") ? 8 : 0;
                break;

            case "NLP":
                score += containsAnyText(text,
                        "natural language processing", "nlp", "text classification") ? 25 : 0;
                score += containsAnyText(text,
                        "sentiment analysis", "text preprocessing", "tokenization") ? 18 : 0;
                score += containsAnyText(text,
                        "transformer", "language model", "llm", "rag") ? 18 : 0;
                score += containsAnyText(text,
                        "python", "tensorflow", "pytorch") ? 10 : 0;
                break;

            case "COMPUTER_VISION":
                score += containsAnyText(text,
                        "computer vision", "object detection", "image classification") ? 25 : 0;
                score += containsAnyText(text,
                        "opencv", "yolo", "mediapipe") ? 18 : 0;
                score += containsAnyText(text,
                        "face detection", "image processing", "image recognition") ? 18 : 0;
                score += containsAnyText(text,
                        "tensorflow", "pytorch", "cnn") ? 14 : 0;
                score += containsAnyText(text,
                        "python", "model training") ? 8 : 0;
                break;

            case "AI":
                score += containsAnyText(text,
                        "artificial intelligence", "generative ai", "ai-powered", "ai powered") ? 22 : 0;
                score += containsAnyText(text,
                        "machine learning", "model training", "classification", "prediction") ? 18 : 0;
                score += containsAnyText(text,
                        "llm", "rag", "language model", "prompt engineering") ? 18 : 0;
                score += containsAnyText(text,
                        "python", "tensorflow", "pytorch") ? 10 : 0;
                break;

            default:
                for (String keyword : roleProfile.keywords) {
                    if (containsConcept(text, keyword)) {
                        score += 100.0 / Math.max(roleProfile.keywords.size(), 1);
                    }
                }
                break;
        }

        return Math.min(round(score), 100);
    }

    private void addConcepts(
            List<String> concepts,
            String... values) {

        for (String value : values) {

            if (value != null
                    && !value.isBlank()) {

                concepts.add(value);
            }
        }
    }

    /*
     * =========================================================
     * EXPERIENCE EVIDENCE SCORE
     * =========================================================
     */

    private double calculateExperienceEvidenceScore(
            String experience,
            String summary,
            Set<String> requiredSkills,
            RoleProfile roleProfile) {

        String combined =
                (
                        safeLower(experience)
                                + " "
                                + safeLower(summary)
                ).trim();

        if (combined.isBlank()) {

            return 0;
        }

        int directMatches = 0;
        int conceptMatches = 0;

        for (String skill :
                requiredSkills) {

            if (containsSkillEvidence(
                    combined,
                    skill)) {

                directMatches++;
            }
        }

        for (String keyword :
                roleProfile.keywords) {

            if (containsConcept(
                    combined,
                    keyword)) {

                conceptMatches++;
            }
        }

        double directScore = 0;

        if (!requiredSkills.isEmpty()) {

            directScore =
                    ((double) directMatches
                            / requiredSkills.size())
                            * 100;
        }

        double conceptScore = 0;

        if (!roleProfile.keywords.isEmpty()) {

            conceptScore =
                    ((double) conceptMatches
                            / roleProfile.keywords.size())
                            * 100;
        }

        /*
         * Professional action evidence.
         */

        int actions = 0;

        String[] actionWords = {

                "developed",
                "implemented",
                "built",
                "designed",
                "engineered",
                "deployed",
                "integrated",
                "tested",
                "optimized",
                "automated",
                "fine-tuned",
                "improved"
        };

        for (String action :
                actionWords) {

            if (combined.contains(action)) {

                actions++;
            }
        }

        double actionBonus =
                Math.min(
                        actions * 4,
                        20
                );

        /*
         * Internship/work evidence.
         */

        double professionalBonus = 0;

        if (containsAnyText(
                combined,
                "internship",
                "intern",
                "professional experience",
                "work experience")) {

            professionalBonus = 10;
        }

        double score =
                (directScore * 0.50)
                        + (conceptScore * 0.30)
                        + actionBonus
                        + professionalBonus;

        return Math.min(
                round(score),
                100
        );
    }

    /*
     * =========================================================
     * EDUCATION + CERTIFICATION
     * =========================================================
     */

    private double calculateEducationCertificationScore(
            String education,
            String certifications,
            String achievements,
            Set<String> requiredSkills,
            RoleProfile roleProfile) {

        String educationText =
                safeLower(education);

        String certificationText =
                safeLower(certifications);

        String achievementText =
                safeLower(achievements);

        if (educationText.isBlank()
                && certificationText.isBlank()
                && achievementText.isBlank()) {

            return 0;
        }

        double educationScore = 0;

        /*
         * Relevant degree.
         */

        if (containsAnyText(
                educationText,
                "computer science",
                "computer engineering",
                "information technology",
                "software engineering",
                "data science",
                "artificial intelligence",
                "computer applications")) {

            educationScore = 80;
        }

        /*
         * General technical degree.
         */

        else if (containsAnyText(
                educationText,
                "bachelor of technology",
                "b.tech",
                "computer")) {

            educationScore = 60;
        }

        /*
         * Certification evidence.
         */

        double certificationScore = 0;

        if (!certificationText.isBlank()) {

            int matched = 0;

            for (String skill :
                    requiredSkills) {

                if (containsSkillEvidence(
                        certificationText,
                        skill)) {

                    matched++;
                }
            }

            if (!requiredSkills.isEmpty()) {

                certificationScore =
                        ((double) matched
                                / requiredSkills.size())
                                * 100;
            } else {

                certificationScore = 50;
            }
        }

        /*
         * Achievements provide a small supporting signal.
         */

        double achievementScore = 0;

        if (!achievementText.isBlank()) {

            achievementScore = 50;
        }

        /*
         * Use the strongest available evidence.
         */

        double score =
                educationScore;

        if (certificationScore > score) {

            score =
                    (score * 0.70)
                            + (certificationScore * 0.30);
        }

        if (achievementScore > 0) {

            score =
                    (score * 0.90)
                            + (achievementScore * 0.10);
        }

        return Math.min(
                round(score),
                100
        );
    }

    /*
     * =========================================================
     * FIND RELEVANT PROJECTS
     * =========================================================
     */

    private List<String> findRelevantProjects(
            String projects,
            String resumeText,
            String jobText,
            Set<String> requiredSkills,
            RoleProfile roleProfile) {

        List<String> relevantProjects =
                new ArrayList<>();

        /*
         * Always prefer the original extracted resume text when it is
         * available. The structured ResumeAnalysis.projects value can be
         * incomplete or reduced to a generic summary by older analysis
         * records. The original resume preserves the actual project names
         * and their descriptions.
         */
        String source =
                resumeText == null
                        ? ""
                        : resumeText.trim();

        String projectsSection =
                extractProjectsSectionFromResumeText(source);

        if (!projectsSection.isBlank()) {
            source = projectsSection;
        } else if (projects != null && !projects.isBlank()) {
            source = projects.trim();
        }

        if (source.isBlank()) {
            return relevantProjects;
        }

        /*
         * The resume parser used by the application may put the project
         * title and date on the same line, for example:
         *
         * WearYourStyle — AI-Powered Fashion Marketplace Feb 2026 – Apr 2026
         *
         * It may also put research project titles on their own line with a
         * year. We therefore split projects primarily from date/title
         * boundaries and only use conservative title detection as a
         * fallback.
         */
        String[] lines =
                source.split("\\r?\\n");

        String currentProject = "";
        StringBuilder currentContent =
                new StringBuilder();

        for (String rawLine : lines) {

            if (rawLine == null || rawLine.isBlank()) {
                continue;
            }

            boolean bulletLine =
                    rawLine.trim().matches("^[•▪●◦\\-].*");

            String line =
                    cleanResumeLine(rawLine);

            if (line.isBlank()) {
                continue;
            }

            if (isProjectTitleLine(line, bulletLine)) {

                if (!currentProject.isBlank()) {
                    addRelevantProject(
                            relevantProjects,
                            currentProject,
                            currentContent.toString(),
                            jobText,
                            requiredSkills,
                            roleProfile
                    );
                }

                currentProject =
                        extractProjectTitleFromLine(line);

                currentContent.setLength(0);

                String remainder =
                        extractProjectDescriptionFromTitleLine(line);

                if (!remainder.isBlank()) {
                    currentContent.append(remainder);
                }

                continue;
            }

            /*
             * Lines such as "Research Work" are section labels, not
             * projects. Ignore them rather than creating a fake project.
             */
            if (isProjectSectionLabel(line)) {
                continue;
            }

            if (!currentProject.isBlank()) {
                if (currentContent.length() > 0) {
                    currentContent.append(' ');
                }
                currentContent.append(line);
            }
        }

        if (!currentProject.isBlank()) {
            addRelevantProject(
                    relevantProjects,
                    currentProject,
                    currentContent.toString(),
                    jobText,
                    requiredSkills,
                    roleProfile
            );
        }

        return relevantProjects;
    }

    private double calculateAggregateProjectScore(
            List<String> relevantProjects) {

        if (relevantProjects == null || relevantProjects.isEmpty()) {
            return 0;
        }

        List<Double> scores = new ArrayList<>();

        for (String project : relevantProjects) {
            if (project == null) {
                continue;
            }

            java.util.regex.Matcher matcher =
                    java.util.regex.Pattern
                            .compile("\\(([0-9]+(?:\\.[0-9]+)?)% relevance\\)")
                            .matcher(project);

            if (matcher.find()) {
                try {
                    scores.add(Double.parseDouble(matcher.group(1)));
                } catch (NumberFormatException ignored) {
                    // Ignore malformed project scores.
                }
            }
        }

        if (scores.isEmpty()) {
            return 0;
        }

        scores.sort(java.util.Collections.reverseOrder());

        double top = scores.get(0);
        double second = scores.size() > 1 ? scores.get(1) : top;
        double third = scores.size() > 2 ? scores.get(2) : second;

        return Math.min(
                round((top * 0.60) + (second * 0.25) + (third * 0.15)),
                100
        );
    }

    private void addRelevantProject(
            List<String> relevantProjects,
            String projectTitle,
            String projectContent,
            String jobText,
            Set<String> requiredSkills,
            RoleProfile roleProfile) {

        if (projectTitle == null || projectTitle.isBlank()) {
            return;
        }

        String content =
                projectContent == null
                        ? ""
                        : projectContent.trim();

        String combined =
                (projectTitle + " " + content).trim();

        if (combined.isBlank()) {
            return;
        }

        double textSimilarity =
                similarityService.calculatePercentage(
                        combined,
                        jobText == null ? "" : jobText
                );

        double evidenceScore =
                calculateProjectEvidenceScore(
                        combined,
                        requiredSkills,
                        roleProfile
                );

        double relevance =
                (textSimilarity * 0.35)
                        + (evidenceScore * 0.65);

        relevance =
                Math.min(
                        round(relevance),
                        100
                );

        /*
         * Keep genuinely relevant projects. The title is always shown
         * exactly as extracted from the resume, while the description is
         * included only as supporting context.
         */
        if (relevance >= 5) {

            String displayTitle =
                    projectTitle.trim();

            if (!content.isBlank()) {

                String cleanDescription =
                        content
                                .replaceAll("\\s+", " ")
                                .trim();

                if (cleanDescription.length() > 220) {
                    cleanDescription =
                            cleanDescription.substring(0, 220)
                                    + "...";
                }

                displayTitle +=
                        " (" + relevance + "% relevance)";
            } else {
                displayTitle +=
                        " (" + relevance + "% relevance)";
            }

            relevantProjects.add(displayTitle);
        }
    }

    private String extractProjectsSectionFromResumeText(
            String resumeText) {

        if (resumeText == null || resumeText.isBlank()) {
            return "";
        }

        String normalized =
                resumeText
                        .replace("\r", "")
                        .replace("\u00A0", " ");

        String[] lines =
                normalized.split("\\n");

        boolean insideProjects = false;
        StringBuilder section =
                new StringBuilder();

        for (String rawLine : lines) {

            String line =
                    rawLine.trim();

            if (line.isBlank()) {
                if (insideProjects) {
                    section.append('\n');
                }
                continue;
            }

            String heading =
                    line
                            .replaceAll(
                                    "^[•\\-–—]+\\s*",
                                    ""
                            )
                            .replaceAll(
                                    "[:|]+$",
                                    ""
                            )
                            .trim()
                            .toUpperCase(Locale.ROOT);

            if (!insideProjects) {
                if (heading.equals("PROJECTS")
                        || heading.equals("TECHNICAL PROJECTS")
                        || heading.equals("ACADEMIC PROJECTS")
                        || heading.equals("PERSONAL PROJECTS")) {

                    insideProjects = true;
                }

                continue;
            }

            /* Research Work is a separate resume section, not a project. */
            if (heading.equals("RESEARCH WORK")) {
                break;
            }

            /* PROJECTS ends at the next major resume section. */
            if (heading.equals("EDUCATION")
                    || heading.equals("EXPERIENCE")
                    || heading.equals("WORK EXPERIENCE")
                    || heading.equals("PROFESSIONAL EXPERIENCE")
                    || heading.equals("CERTIFICATIONS")
                    || heading.equals("ACHIEVEMENTS")
                    || heading.equals("SKILLS")
                    || heading.equals("TECHNICAL SKILLS")
                    || heading.equals("PUBLICATIONS")
                    || heading.equals("INTERNSHIP")
                    || heading.equals("KEY HIGHLIGHTS")
                    || heading.equals("HIGHLIGHTS")
                    || heading.equals("PROFILE")
                    || heading.equals("PROFESSIONAL SUMMARY")
                    || heading.equals("SUMMARY")
                    || heading.equals("OBJECTIVE")
                    || heading.equals("CAREER OBJECTIVE")
                    || heading.equals("LANGUAGES")
                    || heading.equals("CONTACT")
                    || heading.equals("ACHIEVEMENTS AND ACTIVITIES")) {

                break;
            }

            section.append(line).append('\n');
        }

        return section.toString().trim();
    }

    private String cleanResumeLine(String rawLine) {

        if (rawLine == null) {
            return "";
        }

        return rawLine
                .replace("\u00A0", " ")
                .trim();
    }

    private boolean isProjectTitleLine(
            String line,
            boolean bulletLine) {

        if (line == null || line.isBlank()) {
            return false;
        }

        // Never allow a bullet point to become a separate project.
        if (bulletLine || line.trim().matches("^[•▪●◦\\-].*")) {
            return false;
        }

        if (isProjectSectionLabel(line)) {
            return false;
        }

        String lower = safeLower(line).trim();

        // Metadata/reference lines are not project titles.
        if (lower.equals("github")
                || lower.equals("live")
                || lower.equals("demo")
                || lower.startsWith("github:")
                || lower.startsWith("live:")
                || lower.startsWith("certificate")
                || lower.startsWith("certificates")
                || lower.startsWith("repo:")) {
            return false;
        }

        // A real project title may have a date/year on the same line.
        if (containsProjectDate(line)) {
            return true;
        }

        /*
         * Some resumes use project titles without dates.
         *
         * Examples:
         *   College Feedback Classifier
         *   Raspberry Pi-Based Helmet Detection and Ignition Control System
         *
         * Wrapped PDF lines from a project's bullet points can sometimes
         * lose their bullet marker. Therefore we must NOT treat every
         * non-bullet line as a project title.
         *
         * For undated titles, require title-like formatting:
         *   - reasonably short
         *   - does not end like a sentence
         *   - starts with an uppercase letter
         *   - most words look like title words
         *
         * This keeps lines such as:
         *   "such as Academics, Facilities, and Administration."
         *   "review and categorization."
         *   "cloud-based AI workflows."
         * from becoming fake projects.
         */
        if (line.length() > 120) {
            return false;
        }

        if (line.matches(".*[.!?;,]$")) {
            return false;
        }

        if (!Character.isUpperCase(line.charAt(0))) {
            return false;
        }

        String[] words = line.split("\\s+");

        if (words.length == 0 || words.length > 14) {
            return false;
        }

        int titleLikeWords = 0;
        int meaningfulWords = 0;

        for (String word : words) {
            String cleaned =
                    word.replaceAll(
                            "^[^A-Za-z0-9]+|[^A-Za-z0-9]+$",
                            ""
                    );

            if (cleaned.isBlank()) {
                continue;
            }

            meaningfulWords++;

            if (Character.isUpperCase(cleaned.charAt(0))
                    || cleaned.equals(cleaned.toUpperCase(Locale.ROOT))
                    || cleaned.matches("\\d+")) {
                titleLikeWords++;
            }
        }

        if (meaningfulWords == 0) {
            return false;
        }

        double titleRatio =
                (double) titleLikeWords / meaningfulWords;

        return titleRatio >= 0.45;
    }

    private boolean containsProjectDate(String line) {

        if (line == null) {
            return false;
        }

        return line.matches(
                ".*(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{4}.*"
        ) || line.matches(
                ".*\\b20\\d{2}\\b.*"
        );
    }

    private String extractProjectTitleFromLine(String line) {

        if (line == null) {
            return "";
        }

        String title =
                line
                        .replaceAll(
                                "\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{4}.*$",
                                ""
                        )
                        .replaceAll(
                                "\\s+20\\d{2}\\s*$",
                                ""
                        )
                        .trim();

        return title.isBlank()
                ? line.trim()
                : title;
    }

    private String extractProjectDescriptionFromTitleLine(
            String line) {

        if (line == null || line.isBlank()) {
            return "";
        }

        String remainder =
                line
                        .replaceFirst(
                                "^.*?(?=(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{4})",
                                ""
                        )
                        .replaceFirst(
                                "^.*?(?=20\\d{2})",
                                ""
                        )
                        .trim();

        /* Dates themselves are not project descriptions. */
        if (remainder.matches(
                "^(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{4}.*$"
        ) || remainder.matches("^20\\d{2}.*$")) {
            return "";
        }

        return "";
    }

    private boolean isProjectSectionLabel(String line) {

        if (line == null) {
            return true;
        }

        String value =
                line
                        .replaceAll(
                                "[:|]+$",
                                ""
                        )
                        .trim()
                        .toUpperCase(Locale.ROOT);

        return value.equals("PROJECTS")
                || value.equals("TECHNICAL PROJECTS")
                || value.equals("ACADEMIC PROJECTS")
                || value.equals("PERSONAL PROJECTS")
                || value.equals("RESEARCH WORK");
    }

    /*
     * =========================================================
     * CONCEPT MATCHING
     * =========================================================
     */

    private boolean containsConcept(
            String text,
            String concept) {

        if (text == null
                || concept == null) {

            return false;
        }

        String lower =
                safeLower(text);

        String target =
                safeLower(concept);

        if (lower.contains(target)) {

            return true;
        }

        if (target.equals("api")) {

            return containsAnyText(
                    lower,
                    "rest",
                    "endpoint",
                    "service"
            );
        }

        if (target.equals("backend")) {

            return containsAnyText(
                    lower,
                    "server",
                    "api",
                    "database",
                    "backend"
            );
        }

        if (target.equals("database")) {

            return containsAnyText(
                    lower,
                    "sql",
                    "postgres",
                    "mysql",
                    "storage",
                    "database"
            );
        }

        if (target.equals("development")) {

            return containsAnyText(
                    lower,
                    "developed",
                    "implemented",
                    "built",
                    "engineered",
                    "designed"
            );
        }

        if (target.equals("software")) {

            return containsAnyText(
                    lower,
                    "application",
                    "system",
                    "platform",
                    "software"
            );
        }

        if (target.equals("model")) {

            return containsAnyText(
                    lower,
                    "model",
                    "classification",
                    "prediction",
                    "training",
                    "fine-tuned"
            );
        }

        if (target.equals("data")) {

            return containsAnyText(
                    lower,
                    "data",
                    "dataset",
                    "csv",
                    "storage",
                    "analysis"
            );
        }

        if (target.equals("cloud")) {

            return containsAnyText(
                    lower,
                    "cloud",
                    "ibm cloud",
                    "watson studio",
                    "watsonx"
            );
        }

        if (target.equals("deployment")) {

            return containsAnyText(
                    lower,
                    "deployed",
                    "deployment",
                    "cloud"
            );
        }

        if (target.equals("integration")) {

            return containsAnyText(
                    lower,
                    "integrated",
                    "integration",
                    "connected"
            );
        }

        if (target.equals("system")) {

            return containsAnyText(
                    lower,
                    "system",
                    "application",
                    "platform",
                    "solution"
            );
        }

        if (target.equals("testing")) {

            return containsAnyText(
                    lower,
                    "testing",
                    "tested",
                    "junit",
                    "unit test"
            );
        }

        if (target.equals("authentication")) {

            return containsAnyText(
                    lower,
                    "authentication",
                    "authorization",
                    "login",
                    "role-based"
            );
        }

        return false;
    }

    /*
     * =========================================================
     * SKILL MATCHING
     * =========================================================
     */

    private boolean skillsMatch(
            String studentSkill,
            String requiredSkill) {

        if (studentSkill == null
                || requiredSkill == null) {

            return false;
        }

        String student =
                normalizeSkill(
                        studentSkill
                );

        String required =
                normalizeSkill(
                        requiredSkill
                );

        if (student.equals(required)) {

            return true;
        }

        /*
         * PostgreSQL
         */

        if ((student.equals("postgres")
                || student.equals("postgresql"))
                &&
                (required.equals("postgres")
                        || required.equals("postgresql"))) {

            return true;
        }

        /*
         * Git / GitHub
         */

        if ((student.equals("git")
                || student.equals("github"))
                &&
                (required.equals("git")
                        || required.equals("github"))) {

            return true;
        }

        /*
         * Spring Boot
         */

        if ((student.equals("springboot")
                || student.equals("spring boot"))
                &&
                (required.equals("springboot")
                        || required.equals("spring boot"))) {

            return true;
        }

        /*
         * REST API
         */

        if ((student.equals("rest api")
                || student.equals("restful api")
                || student.equals("rest"))
                &&
                (required.equals("rest api")
                        || required.equals("restful api")
                        || required.equals("rest"))) {

            return true;
        }

        /*
         * JavaScript
         */

        if ((student.equals("js")
                || student.equals("javascript"))
                &&
                (required.equals("js")
                        || required.equals("javascript"))) {

            return true;
        }

        /*
         * Machine Learning
         */

        if ((student.equals("ml")
                || student.equals("machine learning"))
                &&
                (required.equals("ml")
                        || required.equals("machine learning"))) {

            return true;
        }

        /*
         * Artificial Intelligence
         */

        if ((student.equals("ai")
                || student.equals("artificial intelligence"))
                &&
                (required.equals("ai")
                        || required.equals("artificial intelligence"))) {

            return true;
        }

        return false;
    }

    /*
     * =========================================================
     * RESUME SKILL EVIDENCE
     * =========================================================
     */

    private boolean containsSkillEvidence(
            String resumeText,
            String requiredSkill) {

        if (resumeText == null
                || requiredSkill == null) {

            return false;
        }

        String text =
                safeLower(resumeText);

        String skill =
                normalizeSkill(
                        requiredSkill
                );

        /*
         * Use normalized comparison.
         */

        // Java must be matched as a standalone word so that
        // "JavaScript" does not count as "Java".
        if (skill.equals("java")) {
            return containsWholeWord(text, "java");
        }

        if (text.contains(skill)) {

            return true;
        }

        if (skill.equals("spring boot")) {

            return text.contains(
                    "springboot"
            );
        }

        if (skill.equals("rest api")
                || skill.equals("restful api")
                || skill.equals("rest")) {

            return text.contains(
                    "rest api"
            ) || text.contains(
                    "restful api"
            ) || text.contains(
                    "rest endpoint"
            );
        }

        if (skill.equals("postgresql")) {

            return text.contains(
                    "postgres"
            ) || text.contains(
                    "postgresql"
            );
        }

        if (skill.equals("git")) {

            return text.contains(
                    "git"
            ) || text.contains(
                    "github"
            );
        }

        if (skill.equals("github")) {

            return text.contains(
                    "github"
            );
        }

        if (skill.equals("machine learning")) {

            return text.contains(
                    "machine learning"
            ) || text.contains(
                    "ml"
            );
        }

        if (skill.equals("artificial intelligence")) {

            return text.contains(
                    "artificial intelligence"
            ) || text.contains(
                    " ai "
            );
        }

        if (skill.equals("computer vision")) {

            return text.contains(
                    "computer vision"
            ) || text.contains(
                    "yolo"
            ) || text.contains(
                    "opencv"
            );
        }

        return false;
    }

    /*
     * =========================================================
     * NORMALIZE SKILL
     * =========================================================
     */

    private String normalizeSkill(
            String skill) {

        return safeLower(skill)
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    /*
     * =========================================================
     * STRENGTH GENERATION
     * =========================================================
     */

    private List<String> generateStrengths(
            List<String> matchedSkills,
            Set<String> studentSkills,
            String projects,
            String experience,
            String education,
            List<String> relevantProjects,
            RoleProfile roleProfile) {

        List<String> strengths =
                new ArrayList<>();

        /*
         * Start with the strongest evidence: skills that are
         * explicitly required by this particular job.
         */
        if (!matchedSkills.isEmpty()) {

            strengths.add(
                    "You already match "
                            + matchedSkills.size()
                            + " required job skill(s)."
            );
        }

        /*
         * Role-specific skill evidence.
         * Do not list unrelated skills simply because they exist
         * in the student's profile.
         */
        if (roleProfile.role.equals("FRONTEND")) {

            if (studentSkills.contains("JavaScript")
                    || studentSkills.contains("React")
                    || studentSkills.contains("HTML")
                    || studentSkills.contains("CSS")) {

                strengths.add(
                        "Your JavaScript, React, HTML and CSS foundation aligns well with frontend development."
                );
            }

            if (studentSkills.contains("REST API")) {

                strengths.add(
                        "Your REST API knowledge supports frontend-to-backend integration."
                );
            }

            if (studentSkills.contains("Git")) {

                strengths.add(
                        "Git is already present in your skill set, matching the role's version-control requirement."
                );
            }
        }

        else if (roleProfile.role.equals("JAVA_BACKEND")
                || roleProfile.role.equals("BACKEND")) {

            if (studentSkills.contains("Java")) {

                strengths.add(
                        "Java is already present in your skill set and directly supports backend development."
                );
            }

            if (studentSkills.contains("SQL")) {

                strengths.add(
                        "Your SQL/database knowledge supports backend data access and application development."
                );
            }

            if (studentSkills.contains("REST API")) {

                strengths.add(
                        "Your REST API knowledge is relevant to building and integrating backend services."
                );
            }
        }

        else if (roleProfile.role.equals("DATA_ANALYST")) {

            if (studentSkills.contains("SQL")) {

                strengths.add(
                        "Your SQL knowledge is directly relevant to querying and analyzing business data."
                );
            }

            if (studentSkills.contains("Python")) {

                strengths.add(
                        "Your Python foundation can support data analysis and automation workflows."
                );
            }

            if (containsAnyText(
                    projects,
                    "data",
                    "analysis",
                    "analytics",
                    "dashboard",
                    "visualization",
                    "machine learning")) {

                strengths.add(
                        "Your projects contain evidence of working with data or analytical problem solving."
                );
            }
        }

        else if (roleProfile.role.equals("MACHINE_LEARNING")) {

            if (studentSkills.contains("Python")) {

                strengths.add(
                        "Your Python foundation is directly relevant to machine-learning development."
                );
            }

            if (studentSkills.contains("Machine Learning")) {

                strengths.add(
                        "Machine Learning is already present in your skill set."
                );
            }
        }

        else if (roleProfile.role.equals("NLP")) {

            if (studentSkills.contains("Python")) {

                strengths.add(
                        "Your Python foundation supports NLP development and experimentation."
                );
            }

            if (studentSkills.contains("NLP")) {

                strengths.add(
                        "NLP is already present in your skill set and directly matches this role."
                );
            }
        }

        else if (roleProfile.role.equals("COMPUTER_VISION")) {

            if (studentSkills.contains("Python")) {

                strengths.add(
                        "Your Python foundation supports computer-vision development."
                );
            }

            if (studentSkills.contains("Computer Vision")) {

                strengths.add(
                        "Computer Vision is already present in your skill set and directly matches this role."
                );
            }
        }

        else if (roleProfile.role.equals("AI")) {

            if (studentSkills.contains("Python")) {

                strengths.add(
                        "Your Python foundation supports AI development and experimentation."
                );
            }

            if (studentSkills.contains("Machine Learning")
                    || studentSkills.contains("Artificial Intelligence")) {

                strengths.add(
                        "Your AI/machine-learning knowledge provides relevant technical foundation for this role."
                );
            }
        }

        /*
         * Project and experience evidence is useful for every role,
         * but the wording remains neutral instead of claiming that
         * unrelated technologies are relevant.
         */
        if (projects != null
                && !projects.isBlank()) {

            strengths.add(
                    "Your resume contains hands-on project experience."
            );
        }

        if (!relevantProjects.isEmpty()) {

            strengths.add(
                    "Your existing projects contain transferable evidence for this role."
            );
        }

        if (experience != null
                && !experience.isBlank()) {

            strengths.add(
                    "Your resume contains professional experience that can support this role."
            );
        }

        /*
         * Education should be described in a role-neutral way.
         * Avoid claiming that every technical role is specifically
         * a software-engineering role.
         */
        if (education != null
                && !education.isBlank()
                && containsAnyText(
                education,
                "computer science",
                "computer engineering",
                "information technology")) {

            if (roleProfile.role.equals("DATA_ANALYST")) {

                strengths.add(
                        "Your computer-science/IT education provides a relevant technical foundation for data analysis."
                );

            } else if (roleProfile.role.equals("FRONTEND")) {

                strengths.add(
                        "Your computer-science/IT education provides a relevant technical foundation for web development."
                );

            } else if (roleProfile.role.equals("JAVA_BACKEND")
                    || roleProfile.role.equals("BACKEND")) {

                strengths.add(
                        "Your computer-science/IT education provides a relevant technical foundation for backend development."
                );

            } else {

                strengths.add(
                        "Your computer-science/IT education provides a relevant technical foundation for this role."
                );
            }
        }

        if (strengths.isEmpty()) {

            strengths.add(
                    "Your resume has been successfully analyzed."
            );
        }

        return strengths;
    }

    /*
     * =========================================================
     * RECOMMENDATIONS
     * =========================================================
     */

    private List<String> generateRecommendations(
            List<String> missingSkills,
            RoleProfile roleProfile) {

        List<String> recommendations =
                new ArrayList<>();

        for (String missingSkill :
                missingSkills) {

            String skill =
                    safeLower(
                            missingSkill
                    );

            if (skill.equals("spring boot")) {

                recommendations.add(
                        "Learn Spring Boot and build a REST API project."
                );

            } else if (skill.equals("rest api")
                    || skill.equals("restful api")
                    || skill.equals("rest")) {

                recommendations.add(
                        "Build a REST API using Spring Boot with CRUD operations."
                );

            } else if (skill.equals("postgresql")) {

                recommendations.add(
                        "Build a Java + PostgreSQL project using JPA/Hibernate."
                );

            } else if (skill.equals("docker")) {

                recommendations.add(
                        "Containerize one of your existing projects using Docker."
                );

            } else if (skill.equals("junit")) {

                recommendations.add(
                        "Add JUnit unit testing to a Java/Spring Boot project."
                );

            } else if (skill.equals("kubernetes")) {

                recommendations.add(
                        "Learn Kubernetes by deploying a containerized application."
                );

            } else if (skill.equals("python")) {

                recommendations.add(
                        "Build a practical Python project demonstrating real-world problem solving."
                );

            } else if (skill.equals("machine learning")) {

                recommendations.add(
                        "Build an end-to-end machine learning project with preprocessing, training and evaluation."
                );

            } else if (skill.equals("nlp")) {

                recommendations.add(
                        "Build an NLP project such as sentiment analysis or text classification."
                );

            } else if (skill.equals("computer vision")) {

                recommendations.add(
                        "Build a computer-vision project using OpenCV or YOLO."
                );

            } else if (skill.equals("react")) {

                recommendations.add(
                        "Build a React frontend connected to a REST API."
                );

            } else if (skill.equals("javascript")) {

                recommendations.add(
                        "Build an interactive JavaScript web application."
                );

            } else if (skill.equals("aws")
                    || skill.equals("azure")
                    || skill.equals("ibm cloud")) {

                recommendations.add(
                        "Deploy one of your projects to a cloud platform."
                );

            } else {

                recommendations.add(
                        "Create a practical project demonstrating "
                                + missingSkill
                                + "."
                );
            }
        }

        /*
         * Role-specific recommendation when skill gaps
         * exist but no individual recommendation was created.
         */

        if (recommendations.isEmpty()) {

            if (roleProfile.role.equals(
                    "JAVA_BACKEND")) {

                recommendations.add(
                        "Your detected skills cover the listed requirements. Focus next on Spring Boot architecture, testing, authentication, deployment and production-level API design."
                );

            } else {

                recommendations.add(
                        "Your current skills cover the detected job requirements. Focus on testing, deployment, documentation and production-level features."
                );
            }
        }

        return recommendations;
    }

    /*
     * =========================================================
     * PROJECT RECOMMENDATION
     * =========================================================
     */

    private String generateProjectRecommendation(
            List<String> missingSkills,
            String jobTitle,
            List<String> relevantProjects,
            String existingProjects) {

        String title =
                safeLower(jobTitle);

        if (containsAnyText(
                title,
                "java backend",
                "java back-end",
                "backend developer",
                "back-end developer",
                "backend engineer",
                "back-end engineer")) {

            if (containsAny(
                    missingSkills,
                    "Spring Boot",
                    "REST API",
                    "PostgreSQL")) {

                if (existingProjects != null
                        && !existingProjects.isBlank()) {

                    return "Best upgrade path: extend your existing technical project into a Java Spring Boot Placement Management System with REST APIs, PostgreSQL, authentication, role-based access and JPA/Hibernate.";
                }

                return "Build a Java Spring Boot Placement Management System with REST APIs, PostgreSQL, authentication, role-based access and JPA/Hibernate.";
            }

            return "Build a production-style Java Spring Boot REST API connected to PostgreSQL with testing, authentication and deployment.";
        }

        if (containsAnyText(
                title,
                "full stack",
                "fullstack")) {

            return "Build a full-stack placement portal using React, Spring Boot REST APIs and PostgreSQL.";
        }

        if (containsAnyText(
                title,
                "frontend",
                "front-end")) {

            return "Build a responsive React job-search dashboard with search, filtering, authentication and REST API integration.";
        }

        if (containsAnyText(
                title,
                "machine learning",
                "ml engineer",
                "ml developer")) {

            return "Build an end-to-end machine learning system with data preprocessing, model training, evaluation and deployment.";
        }

        if (containsAnyText(
                title,
                "nlp",
                "natural language")) {

            return "Build an NLP-based text classification system with preprocessing, feature extraction and model evaluation.";
        }

        if (containsAnyText(
                title,
                "computer vision",
                "vision engineer",
                "vision developer")) {

            return "Build a real-time computer-vision application using OpenCV or YOLO with measurable model evaluation.";
        }

        if (containsAnyText(
                title,
                "artificial intelligence",
                "ai engineer",
                "ai developer")) {

            return "Build an AI application solving a practical problem using a local machine-learning or NLP pipeline.";
        }

        if (containsAnyText(
                title,
                "devops",
                "dev ops")) {

            return "Containerize and deploy a Spring Boot application using Docker and Kubernetes.";
        }

        if (!relevantProjects.isEmpty()) {

            return "Upgrade your most relevant existing project to demonstrate the missing skills, then add testing, documentation and deployment.";
        }

        return "Build a practical project combining the most important missing skills for this job.";
    }

    /*
     * =========================================================
     * MULTIPLE SKILL CHECK
     * =========================================================
     */

    private boolean containsAny(
            List<String> skills,
            String... targets) {

        for (String skill :
                skills) {

            for (String target :
                    targets) {

                if (skillsMatch(
                        skill,
                        target)) {

                    return true;
                }
            }
        }

        return false;
    }

    /*
     * =========================================================
     * WHOLE-WORD TEXT CHECK
     * =========================================================
     */

    private boolean containsWholeWord(
            String text,
            String value) {

        if (text == null || value == null || value.isBlank()) {
            return false;
        }

        return java.util.regex.Pattern
                .compile(
                        "(?<![a-z0-9])"
                                + java.util.regex.Pattern.quote(
                                value.toLowerCase(Locale.ROOT)
                        )
                                + "(?![a-z0-9])"
                )
                .matcher(text.toLowerCase(Locale.ROOT))
                .find();
    }

    /*
     * =========================================================
     * MULTIPLE TEXT CHECK
     * =========================================================
     */

    private boolean containsAnyText(
            String text,
            String... values) {

        if (text == null) {

            return false;
        }

        String lower =
                text.toLowerCase(
                        Locale.ROOT
                );

        for (String value :
                values) {

            if (lower.contains(
                    value.toLowerCase(
                            Locale.ROOT
                    ))) {

                return true;
            }
        }

        return false;
    }

    /*
     * =========================================================
     * SAFE TEXT
     * =========================================================
     */

    private String safeText(
            String text) {

        if (text == null) {

            return "";
        }

        return text;
    }

    /*
     * =========================================================
     * SAFE LOWERCASE
     * =========================================================
     */

    private String safeLower(
            String text) {

        if (text == null) {

            return "";
        }

        return text.toLowerCase(
                Locale.ROOT
        );
    }

    /*
     * =========================================================
     * ROUND
     * =========================================================
     */

    private double round(
            double value) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }

    /*
     * =========================================================
     * ROLE PROFILE
     * =========================================================
     */

    private static class RoleProfile {

        private String role;

        private final Set<String> keywords =
                new LinkedHashSet<>();
    }

    /*
     * =========================================================
     * MATCH RESULT
     * =========================================================
     */

    public static class MatchResult {

        private Long studentId;

        private Long jobDescriptionId;

        private String jobTitle;

        private double matchPercentage;

        private double skillScore;

        private double projectScore;

        private double experienceScore;

        private double educationCertificationScore;

        private List<String> studentSkills;

        private List<String> requiredSkills;

        private List<String> matchedSkills;

        private List<String> missingSkills;

        private List<String> strengths;

        private List<String> recommendations;

        private List<String> relevantProjects;

        private String projectRecommendation;

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
    }
}
