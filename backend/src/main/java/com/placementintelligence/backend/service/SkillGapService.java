package com.placementintelligence.backend.service;

import com.placementintelligence.backend.entity.JobDescription;
import com.placementintelligence.backend.entity.Skill;
import com.placementintelligence.backend.entity.Student;
import com.placementintelligence.backend.repository.JobDescriptionRepository;
import com.placementintelligence.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SkillGapService {

    private final StudentRepository studentRepository;
    private final JobDescriptionRepository jobDescriptionRepository;

    public SkillGapService(
            StudentRepository studentRepository,
            JobDescriptionRepository jobDescriptionRepository) {
        this.studentRepository = studentRepository;
        this.jobDescriptionRepository = jobDescriptionRepository;
    }

    public SkillGapResult analyzeSkillGap(Long studentId, Long jobDescriptionId) {

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        JobDescription jobDescription = jobDescriptionRepository.findById(jobDescriptionId)
                .orElseThrow(() -> new RuntimeException("Job description not found"));

        List<String> studentSkills = getStudentSkills(student);
        List<String> requiredSkills = getRequiredSkills(jobDescription);

        List<String> matchedSkills = new ArrayList<>();
        List<SkillGap> skillGaps = new ArrayList<>();

        for (String requiredSkill : requiredSkills) {
            String matchedStudentSkill = findMatchingStudentSkill(requiredSkill, studentSkills);

            if (matchedStudentSkill != null) {
                matchedSkills.add(requiredSkill);
            } else {
                skillGaps.add(buildSkillGap(requiredSkill, jobDescription.getTitle()));
            }
        }

        double coverage = requiredSkills.isEmpty()
                ? 0.0
                : ((double) matchedSkills.size() / requiredSkills.size()) * 100.0;

        skillGaps.sort((a, b) -> Integer.compare(
                priorityRank(b.getPriority()),
                priorityRank(a.getPriority())
        ));

        SkillGapResult result = new SkillGapResult();
        result.setStudentId(studentId);
        result.setJobDescriptionId(jobDescriptionId);
        result.setJobTitle(jobDescription.getTitle());
        result.setStudentSkills(studentSkills);
        result.setRequiredSkills(requiredSkills);
        result.setMatchedSkills(matchedSkills);
        result.setMissingSkills(extractNames(skillGaps));
        result.setSkillCoverage(round(coverage));
        result.setTotalRequiredSkills(requiredSkills.size());
        result.setMatchedSkillCount(matchedSkills.size());
        result.setMissingSkillCount(skillGaps.size());
        result.setSkillGaps(skillGaps);
        result.setOverallAdvice(generateOverallAdvice(skillGaps, coverage));

        return result;
    }

    private List<String> getStudentSkills(Student student) {
        Set<String> skills = new LinkedHashSet<>();

        if (student.getSkills() != null) {
            for (Skill skill : student.getSkills()) {
                if (skill != null && skill.getName() != null && !skill.getName().isBlank()) {
                    skills.add(skill.getName().trim());
                }
            }
        }

        return new ArrayList<>(skills);
    }

    private List<String> getRequiredSkills(JobDescription jobDescription) {
        Set<String> skills = new LinkedHashSet<>();

        if (jobDescription.getExtractedSkills() != null
                && !jobDescription.getExtractedSkills().isBlank()) {
            String[] parts = jobDescription.getExtractedSkills().split(",");

            for (String part : parts) {
                String skill = part.trim();
                if (!skill.isBlank()) {
                    skills.add(skill);
                }
            }
        }

        if (skills.isEmpty()) {
            skills.addAll(inferSkillsFromJobTitle(jobDescription.getTitle()));
        }

        return new ArrayList<>(skills);
    }

    private List<String> inferSkillsFromJobTitle(String title) {
        String lower = safe(title).toLowerCase(Locale.ROOT);
        Set<String> skills = new LinkedHashSet<>();

        if (containsAny(lower, "java backend", "java back-end", "backend developer", "backend engineer")) {
            skills.addAll(Arrays.asList("Java", "Spring Boot", "REST API", "SQL", "Git"));
        } else if (containsAny(lower, "backend", "back-end")) {
            skills.addAll(Arrays.asList("REST API", "SQL", "Git"));
        } else if (containsAny(lower, "frontend", "front-end")) {
            skills.addAll(Arrays.asList("HTML", "CSS", "JavaScript"));
        } else if (lower.contains("full stack") || lower.contains("full-stack")) {
            skills.addAll(Arrays.asList("HTML", "CSS", "JavaScript", "REST API", "SQL", "Git"));
        } else if (containsAny(lower, "machine learning", "ml", "data scientist", "data science")) {
            skills.addAll(Arrays.asList("Python", "Machine Learning", "SQL"));
        } else if (containsAny(lower, "artificial intelligence", "ai engineer", "ai developer")) {
            skills.addAll(Arrays.asList("Python", "AI", "Machine Learning"));
        } else if (lower.contains("nlp") || lower.contains("natural language")) {
            skills.addAll(Arrays.asList("Python", "NLP", "Machine Learning"));
        } else if (lower.contains("computer vision") || lower.contains("cv engineer")) {
            skills.addAll(Arrays.asList("Python", "Computer Vision", "Machine Learning"));
        } else if (containsAny(lower, "devops", "site reliability", "sre")) {
            skills.addAll(Arrays.asList("Git", "Docker", "Kubernetes"));
        } else if (containsAny(lower, "cloud", "cloud engineer", "cloud developer")) {
            skills.addAll(Arrays.asList("Git", "Docker"));
        }

        return new ArrayList<>(skills);
    }

    private String findMatchingStudentSkill(String requiredSkill, List<String> studentSkills) {
        for (String studentSkill : studentSkills) {
            if (skillsMatch(requiredSkill, studentSkill)) {
                return studentSkill;
            }
        }
        return null;
    }

    private boolean skillsMatch(String requiredSkill, String studentSkill) {
        String required = normalize(requiredSkill);
        String student = normalize(studentSkill);

        if (required.equals(student)) {
            return true;
        }

        Map<String, Set<String>> synonymGroups = getSynonymGroups();

        for (Set<String> group : synonymGroups.values()) {
            if (group.contains(required) && group.contains(student)) {
                return true;
            }
        }

        return false;
    }

    private SkillGap buildSkillGap(String skill, String jobTitle) {
        String normalized = normalize(skill);

        SkillGap gap = new SkillGap();
        gap.setSkill(skill);
        gap.setPriority(determinePriority(normalized, jobTitle));
        gap.setReason(buildReason(normalized));
        gap.setLearningRecommendation(buildLearningRecommendation(normalized));
        gap.setSuggestedProject(buildSuggestedProject(normalized, jobTitle));
        gap.setEstimatedLevel(determineStartingLevel(normalized));

        return gap;
    }

    private String determinePriority(String skill, String jobTitle) {
        String lowerTitle = safe(jobTitle).toLowerCase(Locale.ROOT);

        if (containsAny(skill,
                "spring boot", "rest api", "java", "python",
                "javascript", "react", "sql", "machine learning")) {
            return "HIGH";
        }

        if (lowerTitle.contains("backend") && containsAny(skill, "jpa", "hibernate", "postgresql", "mysql", "docker")) {
            return "HIGH";
        }

        if (containsAny(skill, "git", "github", "html", "css", "junit", "maven", "docker")) {
            return "MEDIUM";
        }

        return "LOW";
    }

    private String buildReason(String skill) {
        if (containsAny(skill, "spring boot")) {
            return "Spring Boot is a core backend framework and is directly relevant to building production-ready Java services.";
        }
        if (containsAny(skill, "rest api", "api development")) {
            return "REST API development is a core backend capability used to expose application functionality through HTTP services.";
        }
        if (containsAny(skill, "sql", "postgresql", "mysql")) {
            return "Database skills are important for storing, querying and managing application data in backend systems.";
        }
        if (containsAny(skill, "java")) {
            return "Java is a primary programming skill for the target role and should be demonstrated through practical development work.";
        }
        if (containsAny(skill, "docker")) {
            return "Docker strengthens deployment and environment consistency for modern software applications.";
        }
        if (containsAny(skill, "kubernetes")) {
            return "Kubernetes is commonly used to orchestrate containerized applications at scale.";
        }
        if (containsAny(skill, "junit")) {
            return "Testing knowledge helps demonstrate software quality, maintainability and engineering discipline.";
        }
        return "This skill appears in the target job requirements but is not currently present in the student's detected skill set.";
    }

    private String buildLearningRecommendation(String skill) {
        if (containsAny(skill, "spring boot")) {
            return "Learn Spring Boot fundamentals, dependency injection, configuration, REST controllers, service layers and Spring Data JPA.";
        }
        if (containsAny(skill, "rest api", "api development")) {
            return "Learn HTTP methods, status codes, JSON, request validation, exception handling and REST endpoint design.";
        }
        if (containsAny(skill, "sql", "postgresql", "mysql")) {
            return "Practice SELECT, JOIN, GROUP BY, indexes, constraints and CRUD operations, then connect the database to a Java application.";
        }
        if (containsAny(skill, "docker")) {
            return "Learn images, containers, Dockerfiles, volumes and networking, then containerize an existing backend project.";
        }
        if (containsAny(skill, "kubernetes")) {
            return "Learn pods, deployments, services, configuration and basic scaling for containerized applications.";
        }
        if (containsAny(skill, "junit")) {
            return "Learn unit testing with JUnit, assertions, test structure and integration testing for Spring applications.";
        }
        if (containsAny(skill, "git", "github")) {
            return "Practice branches, commits, pull requests, merge conflict resolution and clean repository workflows.";
        }
        if (containsAny(skill, "react")) {
            return "Learn components, props, state, hooks, API integration and responsive UI development with React.";
        }
        if (containsAny(skill, "javascript")) {
            return "Practice modern JavaScript, arrays, objects, functions, async/await, DOM concepts and API calls.";
        }
        if (containsAny(skill, "python")) {
            return "Strengthen Python fundamentals and build a small practical project using the libraries relevant to the target role.";
        }
        if (containsAny(skill, "machine learning")) {
            return "Study supervised learning, preprocessing, model evaluation and practical model-building workflows.";
        }
        return "Study the fundamentals of " + skill + " and demonstrate it through a small practical project.";
    }

    private String buildSuggestedProject(String skill, String jobTitle) {
        String title = safe(jobTitle);

        if (containsAny(skill, "spring boot", "rest api")) {
            return "Build a " + (title.isBlank() ? "Java Backend" : title) + " project with Spring Boot, REST APIs, CRUD operations and PostgreSQL.";
        }
        if (containsAny(skill, "sql", "postgresql", "mysql")) {
            return "Extend a Java application with PostgreSQL, relational tables, CRUD operations and useful SQL queries.";
        }
        if (containsAny(skill, "docker")) {
            return "Containerize your existing Spring Boot application with Docker and document the run/deployment process.";
        }
        if (containsAny(skill, "junit")) {
            return "Add unit and integration tests to your strongest Java backend project using JUnit.";
        }
        return "Create a small project that uses " + skill + " in a realistic " + (title.isBlank() ? "job-role" : title) + " scenario.";
    }

    private String determineStartingLevel(String skill) {
        if (containsAny(skill, "spring boot", "rest api", "docker", "kubernetes", "junit")) {
            return "BEGINNER";
        }
        return "FOUNDATION";
    }

    private String generateOverallAdvice(List<SkillGap> gaps, double coverage) {
        if (gaps.isEmpty()) {
            return "You currently cover all detected required skills. Focus on strengthening project evidence and practical depth for this role.";
        }

        long highPriority = gaps.stream()
                .filter(gap -> "HIGH".equals(gap.getPriority()))
                .count();

        if (highPriority > 0) {
            return "Prioritize the HIGH-priority gaps first. Build one role-specific project while learning the missing core skills so your resume gains both knowledge and practical evidence.";
        }

        if (coverage >= 70) {
            return "Your skill coverage is already strong. Close the remaining gaps selectively and focus on demonstrating them through projects or experience.";
        }

        return "Build your missing skills progressively, starting with the highest-priority requirements and turning each completed skill into practical project evidence.";
    }

    private Map<String, Set<String>> getSynonymGroups() {
        Map<String, Set<String>> groups = new HashMap<>();

        groups.put("spring", setOf("spring", "spring framework", "spring boot"));
        groups.put("rest", setOf("rest api", "restful api", "restful services", "api development"));
        groups.put("sql", setOf("sql", "mysql", "postgresql", "database"));
        groups.put("git", setOf("git", "github"));
        groups.put("js", setOf("javascript", "js"));
        groups.put("ml", setOf("machine learning", "ml"));
        groups.put("cv", setOf("computer vision", "cv"));
        groups.put("nlp", setOf("nlp", "natural language processing"));

        return groups;
    }

    private Set<String> setOf(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    private List<String> extractNames(List<SkillGap> gaps) {
        List<String> names = new ArrayList<>();
        for (SkillGap gap : gaps) {
            names.add(gap.getSkill());
        }
        return names;
    }

    private int priorityRank(String priority) {
        if ("HIGH".equals(priority)) return 3;
        if ("MEDIUM".equals(priority)) return 2;
        return 1;
    }

    private boolean containsAny(String text, String... values) {
        String lower = safe(text).toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (lower.contains(value.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return safe(value)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public static class SkillGap {
        private String skill;
        private String priority;
        private String reason;
        private String learningRecommendation;
        private String suggestedProject;
        private String estimatedLevel;

        public String getSkill() { return skill; }
        public void setSkill(String skill) { this.skill = skill; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getLearningRecommendation() { return learningRecommendation; }
        public void setLearningRecommendation(String learningRecommendation) { this.learningRecommendation = learningRecommendation; }
        public String getSuggestedProject() { return suggestedProject; }
        public void setSuggestedProject(String suggestedProject) { this.suggestedProject = suggestedProject; }
        public String getEstimatedLevel() { return estimatedLevel; }
        public void setEstimatedLevel(String estimatedLevel) { this.estimatedLevel = estimatedLevel; }
    }

    public static class SkillGapResult {
        private Long studentId;
        private Long jobDescriptionId;
        private String jobTitle;
        private List<String> studentSkills;
        private List<String> requiredSkills;
        private List<String> matchedSkills;
        private List<String> missingSkills;
        private double skillCoverage;
        private int totalRequiredSkills;
        private int matchedSkillCount;
        private int missingSkillCount;
        private List<SkillGap> skillGaps;
        private String overallAdvice;

        public Long getStudentId() { return studentId; }
        public void setStudentId(Long studentId) { this.studentId = studentId; }
        public Long getJobDescriptionId() { return jobDescriptionId; }
        public void setJobDescriptionId(Long jobDescriptionId) { this.jobDescriptionId = jobDescriptionId; }
        public String getJobTitle() { return jobTitle; }
        public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
        public List<String> getStudentSkills() { return studentSkills; }
        public void setStudentSkills(List<String> studentSkills) { this.studentSkills = studentSkills; }
        public List<String> getRequiredSkills() { return requiredSkills; }
        public void setRequiredSkills(List<String> requiredSkills) { this.requiredSkills = requiredSkills; }
        public List<String> getMatchedSkills() { return matchedSkills; }
        public void setMatchedSkills(List<String> matchedSkills) { this.matchedSkills = matchedSkills; }
        public List<String> getMissingSkills() { return missingSkills; }
        public void setMissingSkills(List<String> missingSkills) { this.missingSkills = missingSkills; }
        public double getSkillCoverage() { return skillCoverage; }
        public void setSkillCoverage(double skillCoverage) { this.skillCoverage = skillCoverage; }
        public int getTotalRequiredSkills() { return totalRequiredSkills; }
        public void setTotalRequiredSkills(int totalRequiredSkills) { this.totalRequiredSkills = totalRequiredSkills; }
        public int getMatchedSkillCount() { return matchedSkillCount; }
        public void setMatchedSkillCount(int matchedSkillCount) { this.matchedSkillCount = matchedSkillCount; }
        public int getMissingSkillCount() { return missingSkillCount; }
        public void setMissingSkillCount(int missingSkillCount) { this.missingSkillCount = missingSkillCount; }
        public List<SkillGap> getSkillGaps() { return skillGaps; }
        public void setSkillGaps(List<SkillGap> skillGaps) { this.skillGaps = skillGaps; }
        public String getOverallAdvice() { return overallAdvice; }
        public void setOverallAdvice(String overallAdvice) { this.overallAdvice = overallAdvice; }
    }
}
