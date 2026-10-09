package com.placementintelligence.backend.service;

import com.placementintelligence.backend.entity.Skill;
import com.placementintelligence.backend.repository.SkillRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class SkillExtractionService {

    private final SkillRepository skillRepository;

    public SkillExtractionService(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    public List<Skill> extractSkills(String resumeText) {

        List<Skill> extractedSkills = new ArrayList<>();

        String text = resumeText.toLowerCase();

        List<String> possibleSkills = Arrays.asList(
                "Java",
                "Python",
                "SQL",
                "HTML",
                "CSS",
                "JavaScript",
                "React",
                "Spring Boot",
                "C++",
                "C",
                "Machine Learning",
                "Deep Learning",
                "Artificial Intelligence",
                "NLP",
                "Natural Language Processing",
                "Computer Vision",
                "YOLO",
                "TensorFlow",
                "PyTorch",
                "Jupyter Notebook",
                "Git",
                "GitHub",
                "Docker",
                "AWS",
                "Azure",
                "Google Cloud",
                "IBM Cloud",
                "IBM watsonx.ai",
                "Watson Studio",
                "Prompt Engineering",
                "MongoDB",
                "PostgreSQL",
                "MySQL",
                "Oracle",
                "REST API",
                "Spring",
                "Data Structures",
                "Algorithms"
        );

        for (String skillName : possibleSkills) {

            if (text.contains(skillName.toLowerCase())) {

                Skill skill = findExistingSkill(skillName);

                if (skill == null) {
                    skill = new Skill();
                    skill.setName(skillName);
                    skill = skillRepository.save(skill);
                }

                extractedSkills.add(skill);
            }
        }

        return extractedSkills;
    }

    private Skill findExistingSkill(String skillName) {

        List<Skill> allSkills = skillRepository.findAll();

        for (Skill skill : allSkills) {

            if (skill.getName().equalsIgnoreCase(skillName)) {
                return skill;
            }
        }

        return null;
    }
}