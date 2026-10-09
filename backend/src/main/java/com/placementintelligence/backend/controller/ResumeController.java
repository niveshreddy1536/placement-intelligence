package com.placementintelligence.backend.controller;

import com.placementintelligence.backend.entity.Resume;
import com.placementintelligence.backend.entity.Skill;
import com.placementintelligence.backend.entity.Student;
import com.placementintelligence.backend.entity.ResumeAnalysis;
import com.placementintelligence.backend.repository.ResumeRepository;
import com.placementintelligence.backend.repository.StudentRepository;
import com.placementintelligence.backend.repository.ResumeAnalysisRepository;
import com.placementintelligence.backend.service.AuthService;
import com.placementintelligence.backend.service.PdfTextExtractor;
import com.placementintelligence.backend.service.ResumeAnalysisService;
import com.placementintelligence.backend.service.SkillExtractionService;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeRepository resumeRepository;
    private final StudentRepository studentRepository;
    private final PdfTextExtractor pdfTextExtractor;
    private final SkillExtractionService skillExtractionService;
    private final ResumeAnalysisService resumeAnalysisService;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final AuthService authService;

    public ResumeController(
            ResumeRepository resumeRepository,
            StudentRepository studentRepository,
            PdfTextExtractor pdfTextExtractor,
            SkillExtractionService skillExtractionService,
            ResumeAnalysisService resumeAnalysisService,
            ResumeAnalysisRepository resumeAnalysisRepository,
            AuthService authService) {

        this.resumeRepository = resumeRepository;
        this.studentRepository = studentRepository;
        this.pdfTextExtractor = pdfTextExtractor;
        this.skillExtractionService = skillExtractionService;
        this.resumeAnalysisService = resumeAnalysisService;
        this.resumeAnalysisRepository = resumeAnalysisRepository;
        this.authService = authService;
    }

    /*
     * =========================================================
     * AUTHENTICATED STUDENT CHECK
     * =========================================================
     *
     * The frontend sends:
     *
     * Authorization: Bearer <token>
     *
     * We authenticate that token and make sure that the
     * authenticated student's ID matches the requested ID.
     *
     * This prevents:
     *
     * User A -> /api/resumes/.../2
     * User A -> /api/resumes/.../3
     *
     * from accessing another student's data.
     * =========================================================
     */

    private Student authenticateStudent(
            Long requestedStudentId,
            String authorizationHeader) {

        if (authorizationHeader == null
                || authorizationHeader.isBlank()) {

            throw new RuntimeException(
                    "Authentication required");
        }

        if (!authorizationHeader.startsWith("Bearer ")) {

            throw new RuntimeException(
                    "Invalid authorization header");
        }

        String token =
                authorizationHeader
                        .substring(7)
                        .trim();

        if (token.isBlank()) {

            throw new RuntimeException(
                    "Authentication token is missing");
        }

        Student authenticatedStudent =
                authService.authenticate(token);

        if (!authenticatedStudent
                .getId()
                .equals(requestedStudentId)) {

            throw new RuntimeException(
                    "You are not authorized to access this student's resume");
        }

        return authenticatedStudent;
    }

    /*
     * =========================================================
     * UPLOAD RESUME
     * =========================================================
     */

    @PostMapping("/upload/{studentId}")
    public Resume uploadResume(
            @PathVariable Long studentId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorizationHeader) throws IOException {

        /*
         * Authenticate the logged-in student first.
         */
        Student student =
                authenticateStudent(
                        studentId,
                        authorizationHeader
                );

        if (file.isEmpty()) {

            throw new RuntimeException(
                    "Resume file is empty");
        }

        String originalFileName =
                file.getOriginalFilename();

        if (originalFileName == null
                || !originalFileName
                .toLowerCase()
                .endsWith(".pdf")) {

            throw new RuntimeException(
                    "Only PDF resumes are supported");
        }

        String uploadDirectory =
                "uploads/";

        Files.createDirectories(
                Paths.get(uploadDirectory));

        /*
         * Add the student ID to the stored filename.
         *
         * This prevents different users from accidentally
         * overwriting files with the same filename.
         */
        String safeFileName =
                studentId
                        + "_"
                        + System.currentTimeMillis()
                        + "_"
                        + Paths.get(originalFileName)
                        .getFileName()
                        .toString();

        String filePath =
                uploadDirectory
                        + safeFileName;

        Path path =
                Paths.get(filePath);

        Files.write(
                path,
                file.getBytes());

        String resumeText =
                pdfTextExtractor.extractText(
                        filePath);

        if (resumeText == null
                || resumeText.isBlank()) {

            throw new RuntimeException(
                    "Could not extract text from the resume");
        }

        /*
         * =========================================================
         * EXTRACT PERSONAL INFORMATION
         * =========================================================
         */

        extractAndUpdateStudentDetails(
                student,
                resumeText
        );

        /*
         * =========================================================
         * EXTRACT SKILLS
         * =========================================================
         */

        List<Skill> extractedSkills =
                skillExtractionService.extractSkills(
                        resumeText);

        student.setSkills(
                extractedSkills);

        studentRepository.save(student);

        /*
         * =========================================================
         * SAVE RESUME
         * =========================================================
         */

        Resume resume =
                resumeRepository
                        .findByStudent(student)
                        .orElse(new Resume());

        resume.setStudent(student);
        resume.setFileName(originalFileName);
        resume.setFilePath(filePath);
        resume.setExtractedText(resumeText);

        Resume savedResume =
                resumeRepository.save(resume);

        /*
         * =========================================================
         * ANALYZE RESUME
         * =========================================================
         */

        resumeAnalysisService.analyzeResume(
                savedResume);

        return savedResume;
    }

    /*
     * =========================================================
     * EXTRACT PERSONAL DETAILS
     * =========================================================
     */

    private void extractAndUpdateStudentDetails(
            Student student,
            String resumeText) {

        if (student == null
                || resumeText == null
                || resumeText.isBlank()) {

            return;
        }

        /*
         * ---------------------------------------------------------
         * NORMALIZE TEXT
         * ---------------------------------------------------------
         */

        String normalizedText =
                resumeText
                        .replace("\r\n", "\n")
                        .replace("\r", "\n");

        String[] lines =
                normalizedText.split("\n");

        /*
         * ---------------------------------------------------------
         * NAME
         * ---------------------------------------------------------
         */

        String extractedName =
                extractName(lines);

        if (extractedName != null
                && !extractedName.isBlank()) {

            student.setName(extractedName);
        }

        /*
         * ---------------------------------------------------------
         * EMAIL
         * ---------------------------------------------------------
         */

        String extractedEmail =
                extractEmail(normalizedText);

        if (extractedEmail != null
                && !extractedEmail.isBlank()) {

            student.setEmail(extractedEmail);
        }

        /*
         * ---------------------------------------------------------
         * COLLEGE
         * ---------------------------------------------------------
         */

        String extractedCollege =
                extractCollege(normalizedText);

        if (extractedCollege != null
                && !extractedCollege.isBlank()) {

            student.setCollege(extractedCollege);
        }

        /*
         * ---------------------------------------------------------
         * BRANCH
         * ---------------------------------------------------------
         */

        String extractedBranch =
                extractBranch(normalizedText);

        if (extractedBranch != null
                && !extractedBranch.isBlank()) {

            student.setBranch(extractedBranch);
        }

        /*
         * ---------------------------------------------------------
         * GRADUATION YEAR
         * ---------------------------------------------------------
         */

        Integer extractedGraduationYear =
                extractGraduationYear(
                        normalizedText);

        if (extractedGraduationYear != null) {

            student.setGraduationYear(
                    extractedGraduationYear);
        }
    }

    /*
     * =========================================================
     * NAME EXTRACTION
     * =========================================================
     */

    private String extractName(
            String[] lines) {

        if (lines == null) {
            return null;
        }

        for (String line : lines) {

            if (line == null) {
                continue;
            }

            String cleaned =
                    line.trim()
                            .replaceAll(
                                    "\\s+",
                                    " ");

            if (cleaned.isBlank()) {
                continue;
            }

            String lower =
                    cleaned.toLowerCase();

            /*
             * Ignore obvious labels.
             */

            if (lower.equals("resume")
                    || lower.equals(
                    "curriculum vitae")
                    || lower.equals("cv")) {

                continue;
            }

            /*
             * Ignore contact information.
             */

            if (lower.contains("@")
                    || lower.contains("linkedin")
                    || lower.contains("github")
                    || lower.contains("mobile")
                    || lower.contains("phone")
                    || lower.contains("email")) {

                continue;
            }

            /*
             * A person's name generally contains
             * letters, spaces and optionally dots/hyphens.
             */

            if (cleaned.matches(
                    "[A-Za-z][A-Za-z .'-]{2,100}")) {

                if (!isResumeSectionHeading(
                        lower)) {

                    return cleaned;
                }
            }
        }

        return null;
    }

    /*
     * =========================================================
     * EMAIL EXTRACTION
     * =========================================================
     */

    private String extractEmail(
            String text) {

        Pattern pattern =
                Pattern.compile(
                        "[A-Za-z0-9._%+-]+"
                                + "@"
                                + "[A-Za-z0-9.-]+"
                                + "\\.[A-Za-z]{2,}",
                        Pattern.CASE_INSENSITIVE);

        Matcher matcher =
                pattern.matcher(text);

        if (matcher.find()) {

            return matcher.group()
                    .trim()
                    .toLowerCase();
        }

        return null;
    }

    /*
     * =========================================================
     * COLLEGE EXTRACTION
     * =========================================================
     */

    private String extractCollege(
            String text) {

        String[] lines =
                text.split("\n");

        boolean insideEducation = false;

        for (String line : lines) {

            if (line == null) {
                continue;
            }

            String cleaned =
                    line.trim()
                            .replaceAll(
                                    "\\s+",
                                    " ");

            if (cleaned.isBlank()) {
                continue;
            }

            String lower =
                    cleaned.toLowerCase();

            /*
             * Detect education section.
             */

            if (lower.equals("education")
                    || lower.equals(
                    "academic background")
                    || lower.equals(
                    "education background")) {

                insideEducation = true;
                continue;
            }

            /*
             * Stop when another major section starts.
             */

            if (insideEducation
                    && isMajorResumeSection(
                    lower)) {

                break;
            }

            if (!insideEducation) {
                continue;
            }

            /*
             * Look for institutions.
             */

            if (lower.contains("institute")
                    || lower.contains("university")
                    || lower.contains("college")
                    || lower.contains("school")) {

                /*
                 * Prefer the institution associated
                 * with Bachelor's / B.Tech education.
                 */

                if (lower.contains("vellore")
                        || lower.contains("vit")) {

                    return cleaned;
                }
            }
        }

        /*
         * Fallback: search complete resume.
         */

        for (String line : lines) {

            if (line == null) {
                continue;
            }

            String cleaned =
                    line.trim()
                            .replaceAll(
                                    "\\s+",
                                    " ");

            String lower =
                    cleaned.toLowerCase();

            if (lower.contains(
                    "vellore institute of technology")) {

                return cleaned;
            }
        }

        return null;
    }

    /*
     * =========================================================
     * BRANCH EXTRACTION
     * =========================================================
     */

    private String extractBranch(
            String text) {

        Pattern pattern =
                Pattern.compile(
                        "(?:Bachelor\\s+of\\s+Technology|B\\.Tech)"
                                + "\\s*(?:-|:)??\\s*(?:in)?\\s*"
                                + "([A-Za-z][A-Za-z &/.-]{2,100})",
                        Pattern.CASE_INSENSITIVE);

        Matcher matcher =
                pattern.matcher(text);

        if (matcher.find()) {

            String branch =
                    matcher.group(1)
                            .trim();

            /*
             * Remove information captured after branch.
             */

            branch =
                    branch.replaceAll(
                            "\\s+(?:CGPA|GPA|Percentage|\\d{4}).*$",
                            ""
                    ).trim();

            if (!branch.isBlank()) {
                return branch;
            }
        }

        /*
         * Direct fallback.
         */

        Pattern directPattern =
                Pattern.compile(
                        "(Computer\\s+Science\\s+and\\s+Engineering)",
                        Pattern.CASE_INSENSITIVE);

        Matcher directMatcher =
                directPattern.matcher(text);

        if (directMatcher.find()) {

            return "Computer Science and Engineering";
        }

        return null;
    }

    /*
     * =========================================================
     * GRADUATION YEAR EXTRACTION
     * =========================================================
     */

    private Integer extractGraduationYear(
            String text) {

        Pattern explicitPattern =
                Pattern.compile(
                        "(?:graduation\\s*year|expected\\s*graduation|"
                                + "graduating|graduation)"
                                + "\\s*(?:year)?\\s*[:\\-]?\\s*"
                                + "(20\\d{2})",
                        Pattern.CASE_INSENSITIVE);

        Matcher explicitMatcher =
                explicitPattern.matcher(text);

        if (explicitMatcher.find()) {

            return Integer.parseInt(
                    explicitMatcher.group(1));
        }

        /*
         * Do not infer graduation year from
         * unrelated dates.
         */

        return null;
    }

    /*
     * =========================================================
     * SECTION HELPERS
     * =========================================================
     */

    private boolean isResumeSectionHeading(
            String text) {

        return
                text.equals("education")
                        || text.equals("skills")
                        || text.equals("skills summary")
                        || text.equals("technical skills")
                        || text.equals("experience")
                        || text.equals("internships")
                        || text.equals("projects")
                        || text.equals("certifications")
                        || text.equals("achievements")
                        || text.equals("career objective")
                        || text.equals("professional summary")
                        || text.equals("summary");
    }

    private boolean isMajorResumeSection(
            String text) {

        return
                text.equals("skills")
                        || text.equals("skills summary")
                        || text.equals("technical skills")
                        || text.equals("experience")
                        || text.equals("internships")
                        || text.equals("projects")
                        || text.equals("certifications")
                        || text.equals("achievements")
                        || text.equals("career objective")
                        || text.equals("professional summary")
                        || text.equals("summary");
    }

    /*
     * =========================================================
     * RE-ANALYZE EXISTING RESUME
     * =========================================================
     */

    @PostMapping("/reanalyze/{studentId}")
    public ResumeAnalysis reanalyzeResume(
            @PathVariable Long studentId,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorizationHeader) {

        /*
         * Authenticate first.
         */

        Student student =
                authenticateStudent(
                        studentId,
                        authorizationHeader
                );

        Resume resume =
                resumeRepository
                        .findByStudent(student)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Resume not found for student"));

        if (resume.getExtractedText() == null
                || resume.getExtractedText()
                .isBlank()) {

            throw new RuntimeException(
                    "Resume extracted text is empty");
        }

        /*
         * Extract personal information again.
         */

        extractAndUpdateStudentDetails(
                student,
                resume.getExtractedText()
        );

        /*
         * Re-extract skills.
         */

        List<Skill> extractedSkills =
                skillExtractionService.extractSkills(
                        resume.getExtractedText());

        student.setSkills(
                extractedSkills);

        studentRepository.save(student);

        return resumeAnalysisService.analyzeResume(
                resume);
    }

    /*
     * =========================================================
     * GET RESUME ANALYSIS
     * =========================================================
     */

    @GetMapping("/analysis/{studentId}")
    public ResumeAnalysis getResumeAnalysis(
            @PathVariable Long studentId,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorizationHeader) {

        /*
         * Authenticate first.
         */

        Student student =
                authenticateStudent(
                        studentId,
                        authorizationHeader
                );

        Resume resume =
                resumeRepository
                        .findByStudent(student)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Resume not found for student"));

        return resumeAnalysisRepository
                .findByResume(resume)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Resume analysis not found"));
    }
}