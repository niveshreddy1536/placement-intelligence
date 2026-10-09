package com.placementintelligence.backend.controller;

import com.placementintelligence.backend.entity.Student;
import com.placementintelligence.backend.repository.StudentRepository;
import com.placementintelligence.backend.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "http://localhost:5173")
public class StudentController {

    private final StudentRepository studentRepository;
    private final AuthService authService;

    public StudentController(
            StudentRepository studentRepository,
            AuthService authService
    ) {
        this.studentRepository = studentRepository;
        this.authService = authService;
    }

    /*
     * ============================================================
     * AUTHENTICATION HELPER
     * ============================================================
     *
     * Reads:
     *
     * Authorization: Bearer <token>
     *
     * Then finds the Student associated with that token.
     *
     * ============================================================
     */

    private Student authenticate(
            String authorizationHeader
    ) {

        if (
                authorizationHeader == null ||
                        authorizationHeader.isBlank()
        ) {
            throw new RuntimeException(
                    "Authentication required"
            );
        }

        if (
                !authorizationHeader.startsWith(
                        "Bearer "
                )
        ) {
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


    /*
     * ============================================================
     * VERIFY THAT THE REQUESTED STUDENT BELONGS TO THE TOKEN
     * ============================================================
     */

    private Student authenticateStudent(
            Long requestedStudentId,
            String authorizationHeader
    ) {

        Student authenticatedStudent =
                authenticate(
                        authorizationHeader
                );

        if (
                requestedStudentId == null ||
                        !authenticatedStudent
                                .getId()
                                .equals(requestedStudentId)
        ) {

            throw new RuntimeException(
                    "You are not authorized to access this student's profile"
            );
        }

        return authenticatedStudent;
    }


    /*
     * ============================================================
     * GET ALL STUDENTS
     * ============================================================
     *
     * IMPORTANT:
     *
     * This endpoint is now user-isolated.
     *
     * Instead of returning every student in the database,
     * it returns only the currently authenticated student.
     *
     * ============================================================
     */

    @GetMapping
    public List<Student> getAllStudents(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorizationHeader
    ) {

        Student authenticatedStudent =
                authenticate(
                        authorizationHeader
                );

        return List.of(
                authenticatedStudent
        );
    }


    /*
     * ============================================================
     * GET STUDENT BY ID
     * ============================================================
     *
     * Example:
     *
     * GET /api/students/2
     *
     * Student #3 cannot use Student #2's token to access this.
     *
     * ============================================================
     */

    @GetMapping("/{id}")
    public Student getStudentById(
            @PathVariable Long id,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorizationHeader
    ) {

        return authenticateStudent(
                id,
                authorizationHeader
        );
    }


    /*
     * ============================================================
     * UPDATE STUDENT PROFILE
     * ============================================================
     *
     * PUT /api/students/{id}
     *
     * Only the authenticated owner of the profile can update it.
     *
     * ============================================================
     */

    @PutMapping("/{id}")
    public Student updateStudent(
            @PathVariable Long id,
            @RequestBody Student student,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorizationHeader
    ) {

        Student existingStudent =
                authenticateStudent(
                        id,
                        authorizationHeader
                );

        /*
         * ========================================================
         * BASIC INFORMATION
         * ========================================================
         */

        if (
                student.getName() != null &&
                        !student.getName().isBlank()
        ) {
            existingStudent.setName(
                    student.getName()
            );
        }


        /*
         * ========================================================
         * EMAIL
         * ========================================================
         *
         * Do not blindly replace the email.
         *
         * The authenticated account is based on the email/token
         * relationship.
         *
         * If the user provides a different email, verify that
         * another student is not already using it.
         *
         * ========================================================
         */

        if (
                student.getEmail() != null &&
                        !student.getEmail().isBlank() &&
                        !student.getEmail()
                                .equalsIgnoreCase(
                                        existingStudent.getEmail()
                                )
        ) {

            studentRepository
                    .findByEmailIgnoreCase(
                            student.getEmail()
                    )
                    .ifPresent(
                            otherStudent -> {

                                if (
                                        !otherStudent
                                                .getId()
                                                .equals(
                                                        existingStudent
                                                                .getId()
                                                )
                                ) {
                                    throw new RuntimeException(
                                            "Email is already registered to another account"
                                    );
                                }
                            }
                    );

            existingStudent.setEmail(
                    student.getEmail()
            );
        }


        /*
         * ========================================================
         * PERSONAL INFORMATION
         * ========================================================
         */

        existingStudent.setPhone(
                student.getPhone()
        );

        existingStudent.setLocation(
                student.getLocation()
        );


        /*
         * ========================================================
         * EDUCATION
         * ========================================================
         */

        existingStudent.setCollege(
                student.getCollege()
        );

        existingStudent.setBranch(
                student.getBranch()
        );

        existingStudent.setGraduationYear(
                student.getGraduationYear()
        );

        existingStudent.setCgpa(
                student.getCgpa()
        );


        /*
         * ========================================================
         * CAREER PREFERENCES
         * ========================================================
         */

        existingStudent.setPreferredRole(
                student.getPreferredRole()
        );

        existingStudent.setPreferredLocation(
                student.getPreferredLocation()
        );

        existingStudent.setWorkMode(
                student.getWorkMode()
        );


        /*
         * ========================================================
         * SOCIAL / PORTFOLIO LINKS
         * ========================================================
         */

        existingStudent.setLinkedinUrl(
                student.getLinkedinUrl()
        );

        existingStudent.setGithubUrl(
                student.getGithubUrl()
        );

        existingStudent.setPortfolioUrl(
                student.getPortfolioUrl()
        );


        /*
         * ========================================================
         * SKILLS
         * ========================================================
         *
         * Only replace skills when the request actually contains
         * a skills list.
         *
         * This prevents accidental clearing of existing skills.
         * ========================================================
         */

        if (student.getSkills() != null) {

            existingStudent.setSkills(
                    student.getSkills()
            );
        }


        /*
         * ========================================================
         * IMPORTANT SECURITY RULE
         * ========================================================
         *
         * We intentionally DO NOT copy:
         *
         * passwordHash
         * passwordSalt
         * authToken
         *
         * from the request body.
         *
         * Those fields belong to authentication and must never be
         * changed through the normal profile endpoint.
         *
         * ========================================================
         */

        return studentRepository.save(
                existingStudent
        );
    }


    /*
     * ============================================================
     * CREATE STUDENT
     * ============================================================
     *
     * Normal account creation should happen through:
     *
     * POST /api/auth/register
     *
     * Therefore this old endpoint is intentionally blocked.
     *
     * ============================================================
     */

    @PostMapping
    public Student createStudent(
            @RequestBody Student student,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorizationHeader
    ) {

        authenticate(
                authorizationHeader
        );

        throw new RuntimeException(
                "Student creation must be done through /api/auth/register"
        );
    }


    /*
     * ============================================================
     * DELETE STUDENT
     * ============================================================
     *
     * Only the authenticated owner can delete their own account.
     *
     * ============================================================
     */

    @DeleteMapping("/{id}")
    public void deleteStudent(
            @PathVariable Long id,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorizationHeader
    ) {

        Student authenticatedStudent =
                authenticateStudent(
                        id,
                        authorizationHeader
                );

        studentRepository.delete(
                authenticatedStudent
        );
    }
}