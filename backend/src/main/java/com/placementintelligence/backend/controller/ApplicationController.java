package com.placementintelligence.backend.controller;

import com.placementintelligence.backend.entity.Application;
import com.placementintelligence.backend.entity.JobDescription;
import com.placementintelligence.backend.entity.Student;
import com.placementintelligence.backend.repository.ApplicationRepository;
import com.placementintelligence.backend.repository.JobDescriptionRepository;
import com.placementintelligence.backend.repository.StudentRepository;
import com.placementintelligence.backend.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/applications")
@CrossOrigin(origins = "http://localhost:5173")
public class ApplicationController {

    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final AuthService authService;

    public ApplicationController(
            ApplicationRepository applicationRepository,
            StudentRepository studentRepository,
            JobDescriptionRepository jobDescriptionRepository,
            AuthService authService) {

        this.applicationRepository =
                applicationRepository;

        this.studentRepository =
                studentRepository;

        this.jobDescriptionRepository =
                jobDescriptionRepository;

        this.authService =
                authService;
    }


    /* =============================================================
       AUTHENTICATION
       ============================================================= */

    private Student authenticate(
            String authorizationHeader) {

        if (authorizationHeader == null
                || authorizationHeader.isBlank()) {

            throw new RuntimeException(
                    "Authentication required"
            );
        }

        if (!authorizationHeader.startsWith("Bearer ")) {

            throw new RuntimeException(
                    "Invalid authorization header"
            );
        }

        String token =
                authorizationHeader
                        .substring(7)
                        .trim();

        if (token.isBlank()) {

            throw new RuntimeException(
                    "Authentication token is missing"
            );
        }

        return authService.authenticate(token);
    }


    private Student authenticateStudent(
            Long requestedStudentId,
            String authorizationHeader) {

        Student authenticatedStudent =
                authenticate(
                        authorizationHeader
                );

        if (!authenticatedStudent
                .getId()
                .equals(requestedStudentId)) {

            throw new RuntimeException(
                    "You are not authorized to access this student's applications"
            );
        }

        return authenticatedStudent;
    }


    private Student authenticateApplicationOwner(
            Application application,
            String authorizationHeader) {

        if (application == null) {

            throw new RuntimeException(
                    "Application not found"
            );
        }

        Student authenticatedStudent =
                authenticate(
                        authorizationHeader
                );

        if (application.getStudent() == null
                || application.getStudent().getId() == null) {

            throw new RuntimeException(
                    "Application has no valid student owner"
            );
        }

        if (!authenticatedStudent
                .getId()
                .equals(
                        application
                                .getStudent()
                                .getId()
                )) {

            throw new RuntimeException(
                    "You are not authorized to access this application"
            );
        }

        return authenticatedStudent;
    }


    /* =============================================================
       GET ALL APPLICATIONS
       ============================================================= */

    @GetMapping
    public List<Application> getAllApplications(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorizationHeader) {

        Student authenticatedStudent =
                authenticate(
                        authorizationHeader
                );

        /*
         * IMPORTANT:
         *
         * Do NOT return every student's applications.
         * Even though this endpoint is called "getAllApplications",
         * a normal logged-in student should only receive their own.
         */
        return applicationRepository
                .findByStudent(
                        authenticatedStudent
                );
    }


    /* =============================================================
       CREATE APPLICATION - LEGACY
       ============================================================= */

    @PostMapping
    public Application createApplication(
            @RequestBody Application application,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorizationHeader) {

        Student authenticatedStudent =
                authenticate(
                        authorizationHeader
                );

        /*
         * If the request supplies a student,
         * it MUST belong to the logged-in user.
         */
        if (application.getStudent() != null
                && application.getStudent().getId() != null) {

            Long requestedStudentId =
                    application
                            .getStudent()
                            .getId();

            if (!authenticatedStudent
                    .getId()
                    .equals(requestedStudentId)) {

                throw new RuntimeException(
                        "You cannot create an application for another student"
                );
            }
        }

        /*
         * Always use the authenticated student.
         *
         * This prevents a client from changing the
         * student relationship manually.
         */
        Student student =
                studentRepository.findById(
                        authenticatedStudent.getId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Student not found"
                        )
                );

        application.setStudent(
                student
        );

        if (application.getStudentName() == null
                || application.getStudentName().isBlank()) {

            application.setStudentName(
                    student.getName()
            );
        }


        /*
         * Resolve Job Pool job if supplied.
         */
        if (application.getJobDescription() != null
                && application
                .getJobDescription()
                .getId() != null) {

            JobDescription jobDescription =
                    jobDescriptionRepository.findById(
                            application
                                    .getJobDescription()
                                    .getId()
                    ).orElseThrow(() ->
                            new RuntimeException(
                                    "Job description not found"
                            )
                    );

            application.setJobDescription(
                    jobDescription
            );

            /*
             * New Job Pool applications don't use
             * the legacy Job relationship.
             */
            application.setJob(null);
        }


        /*
         * Defaults.
         */
        if (application.getStatus() == null
                || application.getStatus().isBlank()) {

            application.setStatus(
                    "APPLIED"
            );
        } else {

            application.setStatus(
                    normalizeStatus(
                            application.getStatus()
                    )
            );
        }

        if (application.getAppliedDate() == null) {

            application.setAppliedDate(
                    LocalDate.now()
            );
        }


        return applicationRepository.save(
                application
        );
    }


    /* =============================================================
       CREATE APPLICATION FROM JOB POOL
       ============================================================= */

    @PostMapping("/from-job-description")
    public Application createFromJobDescription(
            @RequestBody JobPoolApplicationRequest request,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorizationHeader) {

        Student authenticatedStudent =
                authenticate(
                        authorizationHeader
                );


        if (request.getStudentId() == null) {

            throw new IllegalArgumentException(
                    "Student ID is required"
            );
        }


        if (!authenticatedStudent
                .getId()
                .equals(
                        request.getStudentId()
                )) {

            throw new RuntimeException(
                    "You cannot create an application for another student"
            );
        }


        if (request.getJobDescriptionId() == null) {

            throw new IllegalArgumentException(
                    "Job description ID is required"
            );
        }


        Student student =
                studentRepository.findById(
                        authenticatedStudent.getId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Student not found"
                        )
                );


        JobDescription jobDescription =
                jobDescriptionRepository.findById(
                        request.getJobDescriptionId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Job description not found"
                        )
                );


        /*
         * Prevent duplicate applications for
         * the same student + Job Pool job.
         */
        List<Application> existingApplications =
                applicationRepository.findByStudent(
                        student
                );


        for (Application existing :
                existingApplications) {

            if (existing.getJobDescription() != null
                    && existing
                    .getJobDescription()
                    .getId() != null
                    && existing
                    .getJobDescription()
                    .getId()
                    .equals(
                            jobDescription.getId()
                    )) {

                return existing;
            }
        }


        Application application =
                new Application();


        application.setStudent(
                student
        );


        application.setStudentName(
                student.getName()
        );


        application.setJobDescription(
                jobDescription
        );


        application.setStatus(
                "APPLIED"
        );


        application.setAppliedDate(
                LocalDate.now()
        );


        application.setNotes(
                request.getNotes()
        );


        /*
         * New applications do NOT use
         * the legacy Job relationship.
         */
        application.setJob(null);


        return applicationRepository.save(
                application
        );
    }


    /* =============================================================
       GET APPLICATION BY ID
       ============================================================= */

    @GetMapping("/{id}")
    public Application getApplicationById(
            @PathVariable Long id,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorizationHeader) {

        Application application =
                applicationRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Application not found"
                                )
                        );


        authenticateApplicationOwner(
                application,
                authorizationHeader
        );


        return application;
    }


    /* =============================================================
       GET APPLICATIONS FOR STUDENT
       ============================================================= */

    @GetMapping("/student/{studentId}")
    public List<Application> getApplicationsByStudent(
            @PathVariable Long studentId,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorizationHeader) {

        Student authenticatedStudent =
                authenticateStudent(
                        studentId,
                        authorizationHeader
                );


        return applicationRepository
                .findByStudent(
                        authenticatedStudent
                );
    }


    /* =============================================================
       UPDATE APPLICATION
       ============================================================= */

    @PutMapping("/{id}")
    public Application updateApplication(
            @PathVariable Long id,
            @RequestBody Application updatedApplication,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorizationHeader) {

        Application existingApplication =
                applicationRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Application not found"
                                )
                        );


        Student authenticatedStudent =
                authenticateApplicationOwner(
                        existingApplication,
                        authorizationHeader
                );


        /*
         * Student ownership can NEVER be changed
         * through this endpoint.
         */
        existingApplication.setStudent(
                authenticatedStudent
        );


        /*
         * Student name
         */
        if (updatedApplication
                .getStudentName() != null) {

            existingApplication.setStudentName(
                    updatedApplication
                            .getStudentName()
            );
        }


        /*
         * Status
         */
        if (updatedApplication.getStatus() != null
                && !updatedApplication
                .getStatus()
                .isBlank()) {

            existingApplication.setStatus(
                    normalizeStatus(
                            updatedApplication
                                    .getStatus()
                    )
            );
        }


        /*
         * Applied date
         */
        if (updatedApplication
                .getAppliedDate() != null) {

            existingApplication.setAppliedDate(
                    updatedApplication
                            .getAppliedDate()
            );
        }


        /*
         * Notes
         */
        if (updatedApplication.getNotes() != null) {

            existingApplication.setNotes(
                    updatedApplication
                            .getNotes()
            );
        }


        /*
         * Legacy Job relationship.
         *
         * Preserve existing functionality but
         * only allow changing it within the user's
         * own application.
         */
        if (updatedApplication.getJob() != null) {

            existingApplication.setJob(
                    updatedApplication.getJob()
            );
        }


        /*
         * Job Pool relationship.
         */
        if (updatedApplication
                .getJobDescription() != null
                && updatedApplication
                .getJobDescription()
                .getId() != null) {

            JobDescription jobDescription =
                    jobDescriptionRepository.findById(
                            updatedApplication
                                    .getJobDescription()
                                    .getId()
                    ).orElseThrow(() ->
                            new RuntimeException(
                                    "Job description not found"
                            )
                    );


            existingApplication.setJobDescription(
                    jobDescription
            );


            /*
             * Job Pool application takes priority.
             */
            existingApplication.setJob(
                    null
            );
        }


        return applicationRepository.save(
                existingApplication
        );
    }


    /* =============================================================
       LINK EXISTING APPLICATION TO JOB POOL
       ============================================================= */

    @PutMapping(
            "/{applicationId}/job-description/{jobId}"
    )
    public Application linkApplicationToJobDescription(
            @PathVariable Long applicationId,
            @PathVariable Long jobId,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorizationHeader) {

        Application application =
                applicationRepository
                        .findById(applicationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Application not found"
                                )
                        );


        authenticateApplicationOwner(
                application,
                authorizationHeader
        );


        JobDescription jobDescription =
                jobDescriptionRepository
                        .findById(jobId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Job description not found"
                                )
                        );


        application.setJobDescription(
                jobDescription
        );


        /*
         * Remove legacy Job connection.
         */
        application.setJob(null);


        return applicationRepository.save(
                application
        );
    }


    /* =============================================================
       UPDATE APPLICATION STATUS
       ============================================================= */

    @PutMapping("/{id}/status")
    public Application updateApplicationStatus(
            @PathVariable Long id,
            @RequestBody StatusRequest request,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorizationHeader) {

        Application application =
                applicationRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Application not found"
                                )
                        );


        authenticateApplicationOwner(
                application,
                authorizationHeader
        );


        application.setStatus(
                normalizeStatus(
                        request.getStatus()
                )
        );


        return applicationRepository.save(
                application
        );
    }


    /* =============================================================
       UPDATE APPLICATION NOTES
       ============================================================= */

    @PutMapping("/{id}/notes")
    public Application updateApplicationNotes(
            @PathVariable Long id,
            @RequestBody NotesRequest request,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorizationHeader) {

        Application application =
                applicationRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Application not found"
                                )
                        );


        authenticateApplicationOwner(
                application,
                authorizationHeader
        );


        application.setNotes(
                request.getNotes()
        );


        return applicationRepository.save(
                application
        );
    }


    /* =============================================================
       DELETE APPLICATION
       ============================================================= */

    @DeleteMapping("/{id}")
    public void deleteApplication(
            @PathVariable Long id,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorizationHeader) {

        Application application =
                applicationRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Application not found"
                                )
                        );


        authenticateApplicationOwner(
                application,
                authorizationHeader
        );


        applicationRepository.delete(
                application
        );
    }


    /* =============================================================
       STATUS VALIDATION
       ============================================================= */

    private String normalizeStatus(
            String status) {

        if (status == null
                || status.isBlank()) {

            throw new IllegalArgumentException(
                    "Application status cannot be empty"
            );
        }


        String normalized =
                status
                        .trim()
                        .toUpperCase();


        switch (normalized) {

            case "APPLIED":
            case "SHORTLISTED":
            case "INTERVIEW":
            case "SELECTED":
            case "REJECTED":
            case "WITHDRAWN":

                return normalized;

            default:

                throw new IllegalArgumentException(
                        "Invalid application status. "
                                + "Allowed values: "
                                + "APPLIED, SHORTLISTED, INTERVIEW, "
                                + "SELECTED, REJECTED, WITHDRAWN"
                );
        }
    }


    /* =============================================================
       JOB POOL APPLICATION REQUEST
       ============================================================= */

    public static class JobPoolApplicationRequest {

        private Long studentId;

        private Long jobDescriptionId;

        private String notes;


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


        public String getNotes() {
            return notes;
        }


        public void setNotes(
                String notes) {

            this.notes =
                    notes;
        }
    }


    /* =============================================================
       STATUS REQUEST
       ============================================================= */

    public static class StatusRequest {

        private String status;


        public String getStatus() {
            return status;
        }


        public void setStatus(
                String status) {

            this.status =
                    status;
        }
    }


    /* =============================================================
       NOTES REQUEST
       ============================================================= */

    public static class NotesRequest {

        private String notes;


        public String getNotes() {
            return notes;
        }


        public void setNotes(
                String notes) {

            this.notes =
                    notes;
        }
    }
}