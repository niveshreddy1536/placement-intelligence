package com.placementintelligence.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================
    // BASIC INFORMATION
    // =========================================================

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    private String location;

    // =========================================================
    // EDUCATION
    // =========================================================

    private String college;

    private String branch;

    private Integer graduationYear;

    private Double cgpa;

    // =========================================================
    // CAREER PREFERENCES
    // =========================================================

    private String preferredRole;

    private String preferredLocation;

    private String workMode;

    // =========================================================
    // SOCIAL / PORTFOLIO LINKS
    // =========================================================

    private String linkedinUrl;

    private String githubUrl;

    private String portfolioUrl;

    // =========================================================
    // AUTHENTICATION
    // =========================================================

    /*
     * Password hash is never returned in API responses.
     */
    @JsonIgnore
    @Column(name = "password_hash")
    private String passwordHash;

    /*
     * Password salt is never returned in API responses.
     */
    @JsonIgnore
    @Column(name = "password_salt")
    private String passwordSalt;

    /*
     * Authentication token is never returned through
     * normal Student API responses.
     */
    @JsonIgnore
    @Column(name = "auth_token", unique = true)
    private String authToken;

    // =========================================================
    // SKILLS
    // =========================================================

    @ManyToMany
    @JoinTable(
            name = "student_skills",
            joinColumns = @JoinColumn(name = "student_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private List<Skill> skills;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public Student() {
    }

    // =========================================================
    // ID
    // =========================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // =========================================================
    // BASIC INFORMATION
    // =========================================================

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    // =========================================================
    // EDUCATION
    // =========================================================

    public String getCollege() {
        return college;
    }

    public void setCollege(String college) {
        this.college = college;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public Integer getGraduationYear() {
        return graduationYear;
    }

    public void setGraduationYear(Integer graduationYear) {
        this.graduationYear = graduationYear;
    }

    public Double getCgpa() {
        return cgpa;
    }

    public void setCgpa(Double cgpa) {
        this.cgpa = cgpa;
    }

    // =========================================================
    // CAREER PREFERENCES
    // =========================================================

    public String getPreferredRole() {
        return preferredRole;
    }

    public void setPreferredRole(String preferredRole) {
        this.preferredRole = preferredRole;
    }

    public String getPreferredLocation() {
        return preferredLocation;
    }

    public void setPreferredLocation(String preferredLocation) {
        this.preferredLocation = preferredLocation;
    }

    public String getWorkMode() {
        return workMode;
    }

    public void setWorkMode(String workMode) {
        this.workMode = workMode;
    }

    // =========================================================
    // SOCIAL / PORTFOLIO LINKS
    // =========================================================

    public String getLinkedinUrl() {
        return linkedinUrl;
    }

    public void setLinkedinUrl(String linkedinUrl) {
        this.linkedinUrl = linkedinUrl;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public String getPortfolioUrl() {
        return portfolioUrl;
    }

    public void setPortfolioUrl(String portfolioUrl) {
        this.portfolioUrl = portfolioUrl;
    }

    // =========================================================
    // AUTHENTICATION GETTERS / SETTERS
    // =========================================================

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    // =========================================================
    // SKILLS
    // =========================================================

    public List<Skill> getSkills() {
        return skills;
    }

    public void setSkills(List<Skill> skills) {
        this.skills = skills;
    }
}