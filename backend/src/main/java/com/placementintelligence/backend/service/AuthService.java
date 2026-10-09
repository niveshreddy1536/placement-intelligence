package com.placementintelligence.backend.service;

import com.placementintelligence.backend.entity.Student;
import com.placementintelligence.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

@Service
public class AuthService {

    private final StudentRepository studentRepository;

    private static final int SALT_LENGTH = 16;
    private static final int TOKEN_LENGTH = 32;
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;

    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    // =========================================================
    // REGISTER
    // =========================================================

    public AuthResponse register(
            String name,
            String email,
            String password
    ) {

        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Name is required");
        }

        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Email is required");
        }

        if (password == null || password.length() < 6) {
            throw new RuntimeException(
                    "Password must be at least 6 characters"
            );
        }

        String normalizedEmail =
                email.trim().toLowerCase();

        Optional<Student> existingOptional =
                studentRepository.findByEmailIgnoreCase(
                        normalizedEmail
                );

        Student student;

        if (existingOptional.isPresent()) {

            /*
             * IMPORTANT:
             *
             * Your existing Student record already contains
             * your profile, skills, resume-related data,
             * projects, etc.
             *
             * Therefore we NEVER create a new Student when
             * this email already exists.
             *
             * We simply configure authentication on the
             * existing Student record.
             */
            student = existingOptional.get();

            student.setName(name.trim());
            student.setEmail(normalizedEmail);

        } else {

            student = new Student();

            student.setName(name.trim());
            student.setEmail(normalizedEmail);
        }

        // -----------------------------------------------------
        // Create password salt
        // -----------------------------------------------------

        byte[] salt = generateSalt();

        String encodedSalt =
                Base64.getEncoder()
                        .encodeToString(salt);

        // -----------------------------------------------------
        // Hash password
        // -----------------------------------------------------

        String passwordHash =
                hashPassword(
                        password,
                        salt
                );

        // -----------------------------------------------------
        // Create authentication token
        // -----------------------------------------------------

        String authToken =
                generateToken();

        // -----------------------------------------------------
        // Save authentication information
        // -----------------------------------------------------

        student.setPasswordSalt(encodedSalt);
        student.setPasswordHash(passwordHash);
        student.setAuthToken(authToken);

        Student savedStudent =
                studentRepository.save(student);

        return new AuthResponse(
                savedStudent.getId(),
                savedStudent.getName(),
                savedStudent.getEmail(),
                savedStudent.getAuthToken()
        );
    }

    // =========================================================
    // LOGIN
    // =========================================================

    public AuthResponse login(
            String email,
            String password
    ) {

        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Email is required");
        }

        if (password == null || password.isEmpty()) {
            throw new RuntimeException("Password is required");
        }

        String normalizedEmail =
                email.trim().toLowerCase();

        Student student =
                studentRepository
                        .findByEmailIgnoreCase(normalizedEmail)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Invalid email or password"
                                )
                        );

        // -----------------------------------------------------
        // Check authentication configuration
        // -----------------------------------------------------

        if (student.getPasswordHash() == null
                || student.getPasswordHash().isBlank()
                || student.getPasswordSalt() == null
                || student.getPasswordSalt().isBlank()) {

            throw new RuntimeException(
                    "This account is not configured for login yet. " +
                            "Please use Create Account once to configure it."
            );
        }

        // -----------------------------------------------------
        // Verify password
        // -----------------------------------------------------

        boolean validPassword =
                verifyPassword(
                        password,
                        student.getPasswordHash(),
                        student.getPasswordSalt()
                );

        if (!validPassword) {

            throw new RuntimeException(
                    "Invalid email or password"
            );
        }

        // -----------------------------------------------------
        // Generate fresh login token
        // -----------------------------------------------------

        String newToken =
                generateToken();

        student.setAuthToken(newToken);

        Student savedStudent =
                studentRepository.save(student);

        return new AuthResponse(
                savedStudent.getId(),
                savedStudent.getName(),
                savedStudent.getEmail(),
                savedStudent.getAuthToken()
        );
    }

    // =========================================================
    // AUTHENTICATE TOKEN
    // =========================================================

    public Student authenticate(String token) {

        if (token == null || token.trim().isEmpty()) {

            throw new RuntimeException(
                    "Authentication token is missing"
            );
        }

        return studentRepository
                .findByAuthToken(token)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Invalid authentication token"
                        )
                );
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    public void logout(String token) {

        if (token == null || token.trim().isEmpty()) {
            return;
        }

        Optional<Student> studentOptional =
                studentRepository.findByAuthToken(token);

        if (studentOptional.isPresent()) {

            Student student =
                    studentOptional.get();

            student.setAuthToken(null);

            studentRepository.save(student);
        }
    }

    // =========================================================
    // PASSWORD HASHING
    // =========================================================

    private String hashPassword(
            String password,
            byte[] salt
    ) {

        try {

            PBEKeySpec spec =
                    new PBEKeySpec(
                            password.toCharArray(),
                            salt,
                            ITERATIONS,
                            KEY_LENGTH
                    );

            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance(
                            "PBKDF2WithHmacSHA256"
                    );

            byte[] hash =
                    factory
                            .generateSecret(spec)
                            .getEncoded();

            spec.clearPassword();

            return Base64
                    .getEncoder()
                    .encodeToString(hash);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Could not hash password",
                    e
            );
        }
    }

    // =========================================================
    // PASSWORD VERIFICATION
    // =========================================================

    private boolean verifyPassword(
            String password,
            String encodedHash,
            String encodedSalt
    ) {

        try {

            byte[] salt =
                    Base64
                            .getDecoder()
                            .decode(encodedSalt);

            String calculatedHash =
                    hashPassword(
                            password,
                            salt
                    );

            return MessageDigest.isEqual(
                    calculatedHash.getBytes(
                            StandardCharsets.UTF_8
                    ),
                    encodedHash.getBytes(
                            StandardCharsets.UTF_8
                    )
            );

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // GENERATE SALT
    // =========================================================

    private byte[] generateSalt() {

        byte[] salt =
                new byte[SALT_LENGTH];

        secureRandom.nextBytes(salt);

        return salt;
    }

    // =========================================================
    // GENERATE AUTH TOKEN
    // =========================================================

    private String generateToken() {

        byte[] tokenBytes =
                new byte[TOKEN_LENGTH];

        secureRandom.nextBytes(tokenBytes);

        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);
    }

    // =========================================================
    // AUTH RESPONSE
    // =========================================================

    public static class AuthResponse {

        private Long studentId;

        private String name;

        private String email;

        private String token;

        public AuthResponse() {
        }

        public AuthResponse(
                Long studentId,
                String name,
                String email,
                String token
        ) {
            this.studentId = studentId;
            this.name = name;
            this.email = email;
            this.token = token;
        }

        public Long getStudentId() {
            return studentId;
        }

        public void setStudentId(Long studentId) {
            this.studentId = studentId;
        }

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

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}