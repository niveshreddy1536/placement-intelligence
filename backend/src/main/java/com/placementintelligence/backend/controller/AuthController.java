package com.placementintelligence.backend.controller;

import com.placementintelligence.backend.entity.Student;
import com.placementintelligence.backend.service.AuthService;
import com.placementintelligence.backend.service.AuthService.AuthResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // =========================================================
    // REGISTER
    // =========================================================

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody RegisterRequest request
    ) {

        try {

            AuthResponse response =
                    authService.register(
                            request.getName(),
                            request.getEmail(),
                            request.getPassword()
                    );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            new ErrorResponse(
                                    e.getMessage()
                            )
                    );
        }
    }

    // =========================================================
    // LOGIN
    // =========================================================

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request
    ) {

        try {

            AuthResponse response =
                    authService.login(
                            request.getEmail(),
                            request.getPassword()
                    );

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            new ErrorResponse(
                                    e.getMessage()
                            )
                    );
        }
    }

    // =========================================================
    // CURRENT USER
    // =========================================================

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorizationHeader
    ) {

        try {

            String token =
                    extractToken(
                            authorizationHeader
                    );

            Student student =
                    authService.authenticate(token);

            AuthResponse response =
                    new AuthResponse(
                            student.getId(),
                            student.getName(),
                            student.getEmail(),
                            student.getAuthToken()
                    );

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            new ErrorResponse(
                                    e.getMessage()
                            )
                    );
        }
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorizationHeader
    ) {

        try {

            String token =
                    extractToken(
                            authorizationHeader
                    );

            authService.logout(token);

            return ResponseEntity.ok(
                    new MessageResponse(
                            "Logged out successfully"
                    )
            );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            new ErrorResponse(
                                    e.getMessage()
                            )
                    );
        }
    }

    // =========================================================
    // TOKEN EXTRACTION
    // =========================================================

    private String extractToken(
            String authorizationHeader
    ) {

        if (authorizationHeader == null
                || authorizationHeader.trim().isEmpty()) {

            throw new RuntimeException(
                    "Authorization header is missing"
            );
        }

        if (!authorizationHeader.startsWith("Bearer ")) {

            throw new RuntimeException(
                    "Invalid authorization format"
            );
        }

        String token =
                authorizationHeader
                        .substring(7)
                        .trim();

        if (token.isEmpty()) {

            throw new RuntimeException(
                    "Authentication token is missing"
            );
        }

        return token;
    }

    // =========================================================
    // REGISTER REQUEST
    // =========================================================

    public static class RegisterRequest {

        private String name;

        private String email;

        private String password;

        public RegisterRequest() {
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

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    // =========================================================
    // LOGIN REQUEST
    // =========================================================

    public static class LoginRequest {

        private String email;

        private String password;

        public LoginRequest() {
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    // =========================================================
    // ERROR RESPONSE
    // =========================================================

    public static class ErrorResponse {

        private String message;

        public ErrorResponse() {
        }

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    // =========================================================
    // MESSAGE RESPONSE
    // =========================================================

    public static class MessageResponse {

        private String message;

        public MessageResponse() {
        }

        public MessageResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}