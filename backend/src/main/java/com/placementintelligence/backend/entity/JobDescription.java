package com.placementintelligence.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "job_descriptions")
public class JobDescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * =========================================================
     * BASIC JOB INFORMATION
     * =========================================================
     */

    @Column(nullable = false)
    private String title;

    @Column
    private String companyName;

    @Column
    private String location;

    @Column
    private String jobType;

    @Column
    private String workMode;

    /*
     * =========================================================
     * COMPENSATION
     * =========================================================
     */

    @Column
    private Double salary;

    /*
     * Salary display text is useful when a company provides
     * values such as:
     *
     * "₹8-12 LPA"
     * "12 LPA"
     * "₹50,000/month"
     *
     * while salary can still store a numeric value when available.
     */

    @Column
    private String salaryText;

    /*
     * =========================================================
     * EXPERIENCE / ELIGIBILITY
     * =========================================================
     */

    @Column
    private String experience;

    @Column
    private String education;

    /*
     * =========================================================
     * APPLICATION INFORMATION
     * =========================================================
     */

    @Column(length = 1000)
    private String applicationUrl;

    @Column
    private String postedDate;

    @Column
    private String applicationDeadline;

    /*
     * =========================================================
     * JOB CONTENT
     * =========================================================
     */

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String extractedSkills;

    @Column(columnDefinition = "TEXT")
    private String extractedResponsibilities;

    @Column(columnDefinition = "TEXT")
    private String extractedRequirements;

    /*
     * =========================================================
     * CONSTRUCTOR
     * =========================================================
     */

    public JobDescription() {
    }

    /*
     * =========================================================
     * GETTERS / SETTERS
     * =========================================================
     */

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getWorkMode() {
        return workMode;
    }

    public void setWorkMode(String workMode) {
        this.workMode = workMode;
    }

    public Double getSalary() {
        return salary;
    }

    public void setSalary(Double salary) {
        this.salary = salary;
    }

    public String getSalaryText() {
        return salaryText;
    }

    public void setSalaryText(String salaryText) {
        this.salaryText = salaryText;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public String getApplicationUrl() {
        return applicationUrl;
    }

    public void setApplicationUrl(String applicationUrl) {
        this.applicationUrl = applicationUrl;
    }

    public String getPostedDate() {
        return postedDate;
    }

    public void setPostedDate(String postedDate) {
        this.postedDate = postedDate;
    }

    public String getApplicationDeadline() {
        return applicationDeadline;
    }

    public void setApplicationDeadline(String applicationDeadline) {
        this.applicationDeadline = applicationDeadline;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExtractedSkills() {
        return extractedSkills;
    }

    public void setExtractedSkills(String extractedSkills) {
        this.extractedSkills = extractedSkills;
    }

    public String getExtractedResponsibilities() {
        return extractedResponsibilities;
    }

    public void setExtractedResponsibilities(
            String extractedResponsibilities) {

        this.extractedResponsibilities =
                extractedResponsibilities;
    }

    public String getExtractedRequirements() {
        return extractedRequirements;
    }

    public void setExtractedRequirements(
            String extractedRequirements) {

        this.extractedRequirements =
                extractedRequirements;
    }
}