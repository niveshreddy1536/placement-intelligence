package com.placementintelligence.backend.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class JobDescriptionAnalyzerService {

    /*
     * Skill groups used by the free local job analyzer.
     *
     * The analyzer does not call any paid AI API.
     * It identifies technologies and concepts from the
     * job title and job description.
     */

    private final Map<String, List<String>> skillKeywords =
            new LinkedHashMap<>();

    public JobDescriptionAnalyzerService() {

        addSkill(
                "Java",
                "java",
                "core java",
                "java programming"
        );

        addSkill(
                "Spring Boot",
                "spring boot",
                "springboot"
        );

        addSkill(
                "Spring",
                "spring framework",
                "spring framework"
        );

        addSkill(
                "REST API",
                "rest api",
                "restful api",
                "rest services",
                "restful services"
        );

        addSkill(
                "SQL",
                "sql",
                "structured query language"
        );

        addSkill(
                "PostgreSQL",
                "postgresql",
                "postgres"
        );

        addSkill(
                "MySQL",
                "mysql"
        );

        addSkill(
                "MongoDB",
                "mongodb",
                "mongo db",
                "mongo"
        );

        addSkill(
                "Python",
                "python"
        );

        addSkill(
                "C",
                " c ",
                "c programming"
        );

        addSkill(
                "C++",
                "c++",
                "cpp"
        );

        addSkill(
                "HTML",
                "html"
        );

        addSkill(
                "CSS",
                "css"
        );

        addSkill(
                "JavaScript",
                "javascript",
                "java script"
        );

        addSkill(
                "React",
                "react",
                "react.js",
                "reactjs"
        );

        addSkill(
                "Angular",
                "angular"
        );

        addSkill(
                "Node.js",
                "node.js",
                "nodejs",
                "node js"
        );

        addSkill(
                "Git",
                "git",
                "version control"
        );

        addSkill(
                "GitHub",
                "github"
        );

        addSkill(
                "Docker",
                "docker",
                "containerization",
                "containers"
        );

        addSkill(
                "Kubernetes",
                "kubernetes",
                "k8s"
        );

        addSkill(
                "AWS",
                "aws",
                "amazon web services"
        );

        addSkill(
                "Azure",
                "azure",
                "microsoft azure"
        );

        addSkill(
                "IBM Cloud",
                "ibm cloud"
        );

        addSkill(
                "Machine Learning",
                "machine learning",
                "ml"
        );

        addSkill(
                "Artificial Intelligence",
                "artificial intelligence",
                "ai"
        );

        addSkill(
                "NLP",
                "nlp",
                "natural language processing"
        );

        addSkill(
                "Computer Vision",
                "computer vision",
                "image processing"
        );

        addSkill(
                "TensorFlow",
                "tensorflow"
        );

        addSkill(
                "PyTorch",
                "pytorch"
        );

        addSkill(
                "Jupyter Notebook",
                "jupyter",
                "jupyter notebook"
        );

        addSkill(
                "Prompt Engineering",
                "prompt engineering",
                "prompt engineer"
        );

        addSkill(
                "API Development",
                "api development",
                "api development"
        );

        addSkill(
                "Microservices",
                "microservices",
                "microservice architecture"
        );

        addSkill(
                "Hibernate",
                "hibernate"
        );

        addSkill(
                "JPA",
                "jpa",
                "java persistence api"
        );

        addSkill(
                "Maven",
                "maven"
        );

        addSkill(
                "JUnit",
                "junit",
                "unit testing"
        );
    }

    private void addSkill(
            String skillName,
            String... keywords) {

        skillKeywords.put(
                skillName,
                Arrays.asList(keywords)
        );
    }

    public List<String> extractSkills(
            String jobTitle,
            String jobDescription) {

        String combinedText =
                ((jobTitle == null ? "" : jobTitle) + " " +
                        (jobDescription == null ? "" : jobDescription))
                        .toLowerCase();

        List<String> extractedSkills =
                new ArrayList<>();

        for (Map.Entry<String, List<String>> entry :
                skillKeywords.entrySet()) {

            String skillName = entry.getKey();

            List<String> keywords =
                    entry.getValue();

            for (String keyword : keywords) {

                if (containsKeyword(combinedText, keyword)) {

                    extractedSkills.add(skillName);

                    break;
                }
            }
        }

        /*
         * Add intelligent defaults based on job title.
         *
         * Example:
         *
         * "Java Backend Developer"
         *
         * can imply backend technologies even when
         * the description is very short.
         */

        applyJobTitleInference(
                combinedText,
                extractedSkills
        );

        return removeDuplicates(extractedSkills);
    }

    private void applyJobTitleInference(
            String text,
            List<String> skills) {

        /*
         * Java Backend Developer
         */

        if (text.contains("java")
                && (text.contains("backend")
                || text.contains("back-end")
                || text.contains("backend developer")
                || text.contains("java developer"))) {

            addIfMissing(skills, "Java");
            addIfMissing(skills, "Spring Boot");
            addIfMissing(skills, "REST API");
            addIfMissing(skills, "SQL");
            addIfMissing(skills, "Git");
        }

        /*
         * Backend Developer
         */

        if (text.contains("backend")
                || text.contains("back-end")) {

            addIfMissing(skills, "REST API");
            addIfMissing(skills, "SQL");
            addIfMissing(skills, "Git");
        }

        /*
         * Frontend Developer
         */

        if (text.contains("frontend")
                || text.contains("front-end")) {

            addIfMissing(skills, "HTML");
            addIfMissing(skills, "CSS");
            addIfMissing(skills, "JavaScript");
        }

        /*
         * Full Stack Developer
         */

        if (text.contains("full stack")
                || text.contains("full-stack")) {

            addIfMissing(skills, "HTML");
            addIfMissing(skills, "CSS");
            addIfMissing(skills, "JavaScript");
            addIfMissing(skills, "REST API");
            addIfMissing(skills, "SQL");
            addIfMissing(skills, "Git");
        }

        /*
         * Data / Machine Learning roles
         */

        if (text.contains("machine learning")
                || text.contains("data scientist")
                || text.contains("data science")) {

            addIfMissing(skills, "Python");
            addIfMissing(skills, "Machine Learning");
            addIfMissing(skills, "SQL");
        }

        /*
         * AI roles
         */

        if (text.contains("artificial intelligence")
                || text.contains(" ai ")
                || text.startsWith("ai ")
                || text.endsWith(" ai")) {

            addIfMissing(skills, "Python");
            addIfMissing(skills, "Artificial Intelligence");
            addIfMissing(skills, "Machine Learning");
        }

        /*
         * NLP roles
         */

        if (text.contains("nlp")
                || text.contains("natural language")) {

            addIfMissing(skills, "Python");
            addIfMissing(skills, "NLP");
            addIfMissing(skills, "Machine Learning");
        }

        /*
         * Computer Vision roles
         */

        if (text.contains("computer vision")
                || text.contains("image processing")) {

            addIfMissing(skills, "Python");
            addIfMissing(skills, "Computer Vision");
            addIfMissing(skills, "Machine Learning");
        }

        /*
         * Cloud roles
         */

        if (text.contains("cloud")
                || text.contains("cloud engineer")) {

            addIfMissing(skills, "Git");
        }

        /*
         * DevOps roles
         */

        if (text.contains("devops")
                || text.contains("dev ops")) {

            addIfMissing(skills, "Git");
            addIfMissing(skills, "Docker");
            addIfMissing(skills, "Kubernetes");
        }
    }

    private boolean containsKeyword(
            String text,
            String keyword) {

        String normalizedKeyword =
                keyword.toLowerCase().trim();

        /*
         * Short terms such as C or AI require
         * special handling so that they don't
         * accidentally match unrelated words.
         */

        if (normalizedKeyword.equals("c")) {

            return text.matches(".*\\bc\\b.*");
        }

        if (normalizedKeyword.equals("ml")) {

            return text.matches(".*\\bml\\b.*");
        }

        if (normalizedKeyword.equals("ai")) {

            return text.matches(".*\\bai\\b.*");
        }

        return text.contains(normalizedKeyword);
    }

    private void addIfMissing(
            List<String> skills,
            String skill) {

        boolean exists = false;

        for (String existing : skills) {

            if (existing.equalsIgnoreCase(skill)) {
                exists = true;
                break;
            }
        }

        if (!exists) {
            skills.add(skill);
        }
    }

    private List<String> removeDuplicates(
            List<String> skills) {

        List<String> result =
                new ArrayList<>();

        for (String skill : skills) {

            addIfMissing(result, skill);
        }

        return result;
    }
}