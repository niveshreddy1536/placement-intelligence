package com.placementintelligence.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Student name is kept for backward compatibility.
     * The actual student relationship is stored below.
     */
    private String studentName;

    /*
     * Application status.
     *
     * Allowed values:
     * APPLIED
     * SHORTLISTED
     * INTERVIEW
     * SELECTED
     * REJECTED
     * WITHDRAWN
     */
    @Column(nullable = false)
    private String status = "APPLIED";

    private LocalDate appliedDate;

    @Column(length = 2000)
    private String notes;

    /*
     * =============================================================
     * STUDENT
     * =============================================================
     */

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    /*
     * =============================================================
     * LEGACY JOB
     * =============================================================
     *
     * Kept so existing application records do not break.
     *
     * New applications should use jobDescription instead.
     */

    @ManyToOne
    @JoinColumn(name = "job_id")
    private Job job;

    /*
     * =============================================================
     * CURRENT JOB POOL CONNECTION
     * =============================================================
     *
     * This is the important new relationship.
     *
     * Job Pool
     *      ↓
     * JobDescription
     *      ↓
     * Application
     */

    @ManyToOne
    @JoinColumn(name = "job_description_id")
    private JobDescription jobDescription;

    /*
     * =============================================================
     * CONSTRUCTORS
     * =============================================================
     */

    public Application() {
        this.status = "APPLIED";
        this.appliedDate = LocalDate.now();
    }

    /*
     * =============================================================
     * GETTERS / SETTERS
     * =============================================================
     */

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getAppliedDate() {
        return appliedDate;
    }

    public void setAppliedDate(LocalDate appliedDate) {
        this.appliedDate = appliedDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public JobDescription getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(JobDescription jobDescription) {
        this.jobDescription = jobDescription;
    }
}