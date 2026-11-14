package com.kuet.ums.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kuet.ums.entity.Course;
import com.kuet.ums.entity.Role;
import com.kuet.ums.entity.UserAccount;
import com.kuet.ums.repository.AccountRepository;
import com.kuet.ums.repository.CourseRepository;
import com.kuet.ums.repository.EnrollmentRepository;
import com.kuet.ums.repository.NoticeRepository;
import com.kuet.ums.security.AccountPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Three short integration tests for the lab presentation.
 *
 * Each test sends one HTTP request through the real application
 * (security -> controller -> service -> repository) and then checks
 * what was actually stored in a real PostgreSQL database.
 */
@SpringBootTest(properties = "spring.config.import=")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
class SimpleIntegrationTest {

    // Starts a temporary PostgreSQL database in Docker, only for these tests.
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    // Tells Spring to connect to that temporary database.
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    MockMvc mvc; // sends fake HTTP requests to our controllers

    @Autowired
    AccountRepository accounts;

    @Autowired
    CourseRepository courses;

    @Autowired
    EnrollmentRepository enrollments;

    @Autowired
    NoticeRepository notices;

    @Autowired
    PasswordEncoder passwords;

    UserAccount admin;
    UserAccount teacher;
    UserAccount student;
    Course course;

    // Runs before every test: empty the tables, then add one admin,
    // one teacher, one student and one course. Every test starts the same way.
    @BeforeEach
    void prepareData() {
        enrollments.deleteAll();
        notices.deleteAll();
        courses.deleteAll();
        accounts.deleteAll();

        String hash = passwords.encode("TestPass123");
        admin = accounts.save(new UserAccount("Admin User", "admin@test.edu", hash, Role.ADMIN));
        teacher = accounts.save(
            new UserAccount("Teacher One", "teacher@test.edu", hash, Role.TEACHER)
        );
        student = accounts.save(
            new UserAccount("Student One", "student@test.edu", hash, Role.STUDENT)
        );
        course = courses.save(new Course("CSE 3230", "Web Programming Laboratory", 2, 30, teacher));
    }

    @Test
    void adminCreatesAStudentAndItIsSavedInTheDatabase() throws Exception {
        String newStudent = """
            {"fullName": "Amina Rahman", "email": "amina@test.edu", "password": "AminaPass123"}
            """;

        // Act: the admin sends POST /api/students
        mvc.perform(
            post("/api/students")
                .with(user(new AccountPrincipal(admin)))
                .with(csrf())
                .contentType("application/json")
                .content(newStudent)
        )
            // Assert the HTTP response
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.fullName").value("Amina Rahman"))
            .andExpect(jsonPath("$.role").value("STUDENT"));

        // Assert the database: the row exists and the password is hashed, not plain text
        UserAccount saved = accounts.findByEmail("amina@test.edu").orElseThrow();
        assertThat(saved.getRole()).isEqualTo(Role.STUDENT);
        assertThat(saved.getPasswordHash()).isNotEqualTo("AminaPass123");
        assertThat(passwords.matches("AminaPass123", saved.getPasswordHash())).isTrue();
    }

    @Test
    void studentIsForbiddenFromCreatingATeacher() throws Exception {
        String newTeacher = """
            {"fullName": "Fake Teacher", "email": "fake@test.edu", "password": "FakePass123"}
            """;

        // Act: a student tries an admin-only action
        mvc.perform(
            post("/api/teachers")
                .with(user(new AccountPrincipal(student)))
                .with(csrf())
                .contentType("application/json")
                .content(newTeacher)
        )
            // Assert: the request is refused with 403 Forbidden
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        // Assert the database: nothing was saved
        assertThat(accounts.existsByEmail("fake@test.edu")).isFalse();
    }

    @Test
    void studentEnrollsInACourseOnlyOnce() throws Exception {
        String enrollment = """
            {"courseId": %d}
            """.formatted(course.getId());

        // Act 1: the student enrols in the course
        mvc.perform(
            post("/api/enrollments")
                .with(user(new AccountPrincipal(student)))
                .with(csrf())
                .contentType("application/json")
                .content(enrollment)
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber());

        // Act 2: the same student sends the same request again
        mvc.perform(
            post("/api/enrollments")
                .with(user(new AccountPrincipal(student)))
                .with(csrf())
                .contentType("application/json")
                .content(enrollment)
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("ALREADY_ENROLLED"));

        // Assert the database: exactly one enrollment was stored
        assertThat(enrollments.count()).isEqualTo(1);
    }
}
