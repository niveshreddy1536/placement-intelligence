package com.placementintelligence.backend.service;

import com.placementintelligence.backend.entity.Resume;
import com.placementintelligence.backend.entity.ResumeAnalysis;
import com.placementintelligence.backend.repository.ResumeAnalysisRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResumeAnalysisService {

    private final ResumeAnalysisRepository resumeAnalysisRepository;

    public ResumeAnalysisService(
            ResumeAnalysisRepository resumeAnalysisRepository) {

        this.resumeAnalysisRepository =
                resumeAnalysisRepository;
    }

    public ResumeAnalysis analyzeResume(
            Resume resume) {

        String text =
                resume.getExtractedText();

        if (text == null || text.isBlank()) {

            throw new RuntimeException(
                    "Resume text is empty"
            );
        }

        ResumeAnalysis analysis =
                resumeAnalysisRepository
                        .findByResume(resume)
                        .orElse(new ResumeAnalysis());

        analysis.setResume(resume);

        String normalizedText =
                normalizeResumeText(text);

        Map<SectionType, String> sections =
                extractSections(normalizedText);

        analysis.setSummary(
                getSection(
                        sections,
                        SectionType.SUMMARY
                )
        );

        analysis.setEducation(
                getSection(
                        sections,
                        SectionType.EDUCATION
                )
        );

        analysis.setExperience(
                getSection(
                        sections,
                        SectionType.EXPERIENCE
                )
        );

        analysis.setProjects(
                getSection(
                        sections,
                        SectionType.PROJECTS
                )
        );

        analysis.setCertifications(
                getSection(
                        sections,
                        SectionType.CERTIFICATIONS
                )
        );

        analysis.setAchievements(
                getSection(
                        sections,
                        SectionType.ACHIEVEMENTS
                )
        );

        return resumeAnalysisRepository.save(
                analysis
        );
    }

    /*
     * =========================================================
     * NORMALIZE RESUME TEXT
     * =========================================================
     */

    private String normalizeResumeText(
            String text) {

        String normalized =
                text.replace(
                        "\r\n",
                        "\n"
                );

        normalized =
                normalized.replace(
                        "\r",
                        "\n"
                );

        /*
         * PDF extraction can sometimes produce
         * literal escaped line-break characters.
         */

        normalized =
                normalized.replace(
                        "\\r\\n",
                        "\n"
                );

        normalized =
                normalized.replace(
                        "\\n",
                        "\n"
                );

        /*
         * Normalize spaces while preserving
         * actual line boundaries.
         */

        normalized =
                normalized.replaceAll(
                        "[ \\t]+",
                        " "
                );

        /*
         * Remove excessive blank lines.
         */

        normalized =
                normalized.replaceAll(
                        "\\n{3,}",
                        "\n\n"
                );

        return normalized.trim();
    }

    /*
     * =========================================================
     * EXTRACT ALL SECTIONS
     * =========================================================
     */

    private Map<SectionType, String> extractSections(
            String text) {

        Map<SectionType, String> sections =
                new LinkedHashMap<>();

        List<ResumeHeading> headings =
                findHeadings(text);

        /*
         * If no headings are detected, create
         * a small fallback summary.
         */

        if (headings.isEmpty()) {

            sections.put(
                    SectionType.SUMMARY,
                    extractFallbackSummary(text)
            );

            return sections;
        }

        /*
         * Extract content between headings.
         *
         * Boundary sections such as Research Work,
         * Skills and Contact are deliberately not
         * stored as ResumeAnalysis sections.
         */

        for (int i = 0;
             i < headings.size();
             i++) {

            ResumeHeading current =
                    headings.get(i);

            int contentStart =
                    current.endIndex;

            int contentEnd =
                    text.length();

            if (i + 1 < headings.size()) {

                contentEnd =
                        headings
                                .get(i + 1)
                                .startIndex;
            }

            if (contentStart >= contentEnd) {
                continue;
            }

            String content =
                    text.substring(
                            contentStart,
                            contentEnd
                    );

            content =
                    cleanSection(content);

            if (content.isBlank()) {
                continue;
            }

            /*
             * Only these sections are stored.
             * Boundary headings simply stop the
             * previous section from consuming content.
             */

            if (isStoredSection(current.type)) {

                sections.put(
                        current.type,
                        content
                );
            }
        }

        /*
         * Generate fallback summary if necessary.
         */

        if (!sections.containsKey(
                SectionType.SUMMARY)) {

            String summary =
                    extractFallbackSummary(text);

            if (!summary.isBlank()) {

                sections.put(
                        SectionType.SUMMARY,
                        summary
                );
            }
        }

        return sections;
    }

    /*
     * =========================================================
     * FIND HEADINGS
     * =========================================================
     */

    private List<ResumeHeading> findHeadings(
            String text) {

        List<ResumeHeading> headings =
                new ArrayList<>();

        String[] lines =
                text.split("\n");

        int currentPosition = 0;

        for (String line : lines) {

            String originalLine =
                    line;

            String trimmed =
                    line.trim();

            if (trimmed.isBlank()) {

                currentPosition +=
                        originalLine.length() + 1;

                continue;
            }

            SectionType type =
                    detectHeading(trimmed);

            if (type != null) {

                boolean duplicate =
                        false;

                if (!headings.isEmpty()) {

                    ResumeHeading previous =
                            headings.get(
                                    headings.size() - 1
                            );

                    if (previous.type == type
                            && currentPosition
                            - previous.startIndex
                            < 20) {

                        duplicate = true;
                    }
                }

                if (!duplicate) {

                    headings.add(
                            new ResumeHeading(
                                    type,
                                    currentPosition,
                                    currentPosition
                                            + originalLine.length()
                            )
                    );
                }
            }

            currentPosition +=
                    originalLine.length() + 1;
        }

        return headings;
    }

    /*
     * =========================================================
     * DETECT HEADING
     * =========================================================
     */

    private SectionType detectHeading(
            String line) {

        String cleaned =
                line.trim()
                        .replaceAll(
                                "^[•\\-–—]+\\s*",
                                ""
                        )
                        .replaceAll(
                                "[:|]+$",
                                ""
                        )
                        .trim();

        /*
         * Genuine resume headings are normally short.
         */

        if (cleaned.length() > 60) {
            return null;
        }

        String normalized =
                cleaned
                        .toUpperCase()
                        .replaceAll(
                                "\\s+",
                                " "
                        )
                        .trim();

        /*
         * Ignore sentence-like lines.
         */

        if (normalized.contains(". ")
                && normalized.length() > 25) {

            return null;
        }

        /*
         * ---------------------------------------------------------
         * SUMMARY
         * ---------------------------------------------------------
         */

        if (matchesHeading(
                normalized,
                "SUMMARY",
                "PROFESSIONAL SUMMARY",
                "PROFILE",
                "PROFESSIONAL PROFILE",
                "CAREER SUMMARY",
                "OBJECTIVE",
                "CAREER OBJECTIVE")) {

            return SectionType.SUMMARY;
        }

        /*
         * ---------------------------------------------------------
         * EXPERIENCE
         * ---------------------------------------------------------
         */

        if (matchesHeading(
                normalized,
                "EXPERIENCE",
                "WORK EXPERIENCE",
                "PROFESSIONAL EXPERIENCE",
                "EMPLOYMENT EXPERIENCE",
                "EMPLOYMENT HISTORY",
                "WORK HISTORY",
                "INTERNSHIP",
                "INTERNSHIPS",
                "INTERNSHIP EXPERIENCE")) {

            return SectionType.EXPERIENCE;
        }

        /*
         * ---------------------------------------------------------
         * PROJECTS
         * ---------------------------------------------------------
         */

        if (matchesHeading(
                normalized,
                "PROJECTS",
                "PROJECT",
                "ACADEMIC PROJECTS",
                "TECHNICAL PROJECTS",
                "PERSONAL PROJECTS",
                "ACADEMIC PROJECT",
                "TECHNICAL PROJECT")) {

            return SectionType.PROJECTS;
        }

        /*
         * ---------------------------------------------------------
         * EDUCATION
         * ---------------------------------------------------------
         */

        if (matchesHeading(
                normalized,
                "EDUCATION",
                "EDUCATIONAL BACKGROUND",
                "ACADEMIC BACKGROUND",
                "ACADEMIC QUALIFICATIONS",
                "EDUCATIONAL QUALIFICATIONS")) {

            return SectionType.EDUCATION;
        }

        /*
         * ---------------------------------------------------------
         * CERTIFICATIONS
         * ---------------------------------------------------------
         */

        if (matchesHeading(
                normalized,
                "CERTIFICATIONS",
                "CERTIFICATION",
                "CERTIFICATES",
                "COURSES",
                "CERTIFICATIONS & COURSES")) {

            return SectionType.CERTIFICATIONS;
        }

        /*
         * ---------------------------------------------------------
         * ACHIEVEMENTS
         * ---------------------------------------------------------
         */

        if (matchesHeading(
                normalized,
                "ACHIEVEMENTS",
                "ACHIEVEMENT",
                "ACCOMPLISHMENTS",
                "HONORS",
                "AWARDS",
                "AWARDS & ACHIEVEMENTS")) {

            return SectionType.ACHIEVEMENTS;
        }

        /*
         * ---------------------------------------------------------
         * SKILLS BOUNDARY
         * ---------------------------------------------------------
         */

        if (matchesHeading(
                normalized,
                "SKILLS",
                "TECHNICAL SKILLS",
                "CORE SKILLS",
                "TECHNOLOGIES",
                "TECHNOLOGIES & TOOLS")) {

            return SectionType.SKILLS_BOUNDARY;
        }

        /*
         * ---------------------------------------------------------
         * CONTACT BOUNDARY
         * ---------------------------------------------------------
         */

        if (matchesHeading(
                normalized,
                "CONTACT",
                "CONTACT INFORMATION",
                "PERSONAL INFORMATION")) {

            return SectionType.CONTACT_BOUNDARY;
        }

        /*
         * ---------------------------------------------------------
         * RESEARCH / PUBLICATIONS BOUNDARY
         *
         * IMPORTANT:
         * This prevents Research Work from being
         * included inside the Projects section.
         * ---------------------------------------------------------
         */

        if (matchesHeading(
                normalized,
                "RESEARCH WORK",
                "RESEARCH",
                "PUBLICATIONS",
                "PUBLICATION",
                "RESEARCH PROJECTS",
                "RESEARCH PROJECT")) {

            return SectionType.OTHER_BOUNDARY;
        }

        /*
         * ---------------------------------------------------------
         * OTHER BOUNDARIES
         * ---------------------------------------------------------
         */

        if (matchesHeading(
                normalized,
                "EXTRACURRICULAR",
                "EXTRACURRICULAR ACTIVITIES",
                "LEADERSHIP",
                "LEADERSHIP ACTIVITIES")) {

            return SectionType.OTHER_BOUNDARY;
        }

        return null;
    }

    /*
     * =========================================================
     * MATCH HEADING
     * =========================================================
     */

    private boolean matchesHeading(
            String normalized,
            String... headings) {

        for (String heading : headings) {

            if (normalized.equals(
                    heading.toUpperCase()
            )) {

                return true;
            }
        }

        return false;
    }

    /*
     * =========================================================
     * STORED SECTION CHECK
     * =========================================================
     */

    private boolean isStoredSection(
            SectionType type) {

        return type == SectionType.SUMMARY
                || type == SectionType.EDUCATION
                || type == SectionType.EXPERIENCE
                || type == SectionType.PROJECTS
                || type == SectionType.CERTIFICATIONS
                || type == SectionType.ACHIEVEMENTS;
    }

    /*
     * =========================================================
     * GET SECTION
     * =========================================================
     */

    private String getSection(
            Map<SectionType, String> sections,
            SectionType type) {

        String value =
                sections.get(type);

        if (value == null) {
            return "";
        }

        /*
         * Projects receive additional cleaning so
         * repository/demo metadata does not become
         * a fake project.
         */

        if (type == SectionType.PROJECTS) {

            return cleanProjectsSection(
                    value
            );
        }

        return cleanSection(value);
    }

    /*
     * =========================================================
     * CLEAN PROJECTS SECTION
     * =========================================================
     */

    private String cleanProjectsSection(
            String section) {

        if (section == null
                || section.isBlank()) {

            return "";
        }

        String[] lines =
                section.split("\n");

        StringBuilder result =
                new StringBuilder();

        for (String line : lines) {

            String value =
                    line.trim();

            if (value.isBlank()) {

                if (result.length() > 0
                        && result.charAt(
                        result.length() - 1
                ) != '\n') {

                    result.append('\n');
                }

                continue;
            }

            String lower =
                    value.toLowerCase();

            /*
             * These are metadata lines attached to
             * projects, not separate projects.
             *
             * Example:
             * GitHub: Smart-Resume-Screener | Live: Demo
             */

            if (lower.startsWith("github:")
                    || lower.startsWith("github :")
                    || lower.startsWith("github |")
                    || lower.startsWith("github-")
                    || lower.equals("github")
                    || lower.startsWith("live:")
                    || lower.startsWith("live |")
                    || lower.startsWith("live demo")
                    || lower.equals("demo")) {

                continue;
            }

            if (result.length() > 0
                    && result.charAt(
                    result.length() - 1
            ) != '\n') {

                result.append('\n');
            }

            result.append(value);
        }

        return cleanSection(
                result.toString()
        );
    }

    /*
     * =========================================================
     * FALLBACK SUMMARY
     * =========================================================
     */

    private String extractFallbackSummary(
            String text) {

        if (text == null
                || text.isBlank()) {

            return "";
        }

        String[] lines =
                text.split("\n");

        StringBuilder summary =
                new StringBuilder();

        int count = 0;

        for (String line : lines) {

            String cleaned =
                    line.trim();

            if (cleaned.isBlank()) {
                continue;
            }

            if (detectHeading(cleaned)
                    != null) {

                break;
            }

            if (containsContactInformation(
                    cleaned)) {

                continue;
            }

            summary.append(
                    cleaned
            ).append(" ");

            count++;

            if (count >= 4) {
                break;
            }
        }

        return cleanSection(
                summary.toString()
        );
    }

    /*
     * =========================================================
     * CONTACT DETECTION
     * =========================================================
     */

    private boolean containsContactInformation(
            String line) {

        String lower =
                line.toLowerCase();

        if (lower.contains("@")) {
            return true;
        }

        if (lower.contains("linkedin")
                || lower.contains("github")) {

            return true;
        }

        return line.matches(
                ".*\\d{10,}.*"
        );
    }

    /*
     * =========================================================
     * CLEAN SECTION
     * =========================================================
     */

    private String cleanSection(
            String section) {

        if (section == null) {
            return "";
        }

        String cleaned =
                section;

        cleaned =
                cleaned.replaceAll(
                        "\\n{3,}",
                        "\n\n"
                );

        cleaned =
                cleaned.replaceAll(
                        "[ \\t]+",
                        " "
                );

        cleaned =
                cleaned.replaceAll(
                        "(?m)^\\s+$",
                        ""
                );

        return cleaned.trim();
    }

    /*
     * =========================================================
     * SECTION TYPES
     * =========================================================
     */

    private enum SectionType {

        SUMMARY,

        EDUCATION,

        EXPERIENCE,

        PROJECTS,

        CERTIFICATIONS,

        ACHIEVEMENTS,

        /*
         * These sections are boundaries only.
         * They prevent content from leaking into
         * the previous stored section.
         */

        SKILLS_BOUNDARY,

        CONTACT_BOUNDARY,

        OTHER_BOUNDARY
    }

    /*
     * =========================================================
     * RESUME HEADING
     * =========================================================
     */

    private static class ResumeHeading {

        private final SectionType type;

        private final int startIndex;

        private final int endIndex;

        private ResumeHeading(
                SectionType type,
                int startIndex,
                int endIndex) {

            this.type = type;

            this.startIndex =
                    startIndex;

            this.endIndex =
                    endIndex;
        }
    }
}