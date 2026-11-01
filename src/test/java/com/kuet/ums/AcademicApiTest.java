package com.kuet.ums;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kuet.ums.dto.*;
import com.kuet.ums.entity.*;
import com.kuet.ums.exception.ApiException;
import com.kuet.ums.repository.*;
import com.kuet.ums.security.AccountPrincipal;
import com.kuet.ums.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.config.import=")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
class AcademicApiTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AccountRepository accounts;
    @Autowired CourseRepository courses;
    @Autowired EnrollmentRepository enrollments;
    @Autowired NoticeRepository notices;
    @Autowired PasswordEncoder passwords;
    @Autowired CourseService courseService;
    @Autowired EnrollmentService enrollmentService;
    @Autowired NoticeService noticeService;
    private UserAccount admin, teacher, otherTeacher, student, otherStudent;
    private Course course, otherCourse;

    @BeforeEach void reset() {
        enrollments.deleteAll(); notices.deleteAll(); courses.deleteAll(); accounts.deleteAll();
        String hash = passwords.encode("TestPass123");
        admin = accounts.save(new UserAccount("Admin User", "admin@test.edu", hash, Role.ADMIN));
        teacher = accounts.save(new UserAccount("Teacher One", "teacher@test.edu", hash, Role.TEACHER));
        otherTeacher = accounts.save(new UserAccount("Teacher Two", "teacher2@test.edu", hash, Role.TEACHER));
        student = accounts.save(new UserAccount("Student One", "student@test.edu", hash, Role.STUDENT));
        otherStudent = accounts.save(new UserAccount("Student Two", "student2@test.edu", hash, Role.STUDENT));
        course = courses.saveAndFlush(new Course("CSE 3230", "Web Programming Laboratory", 2, 2, teacher));
        otherCourse = courses.saveAndFlush(new Course("CSE 3211", "Software Engineering", 3, 20, otherTeacher));
    }

    @Test void courseCrudUsesRestConventionsAndValidatesTeacherAndCode() throws Exception {
        String body = courseBody(" cse 3201 ", 3, 40, teacher.getId());
        MvcResult created = mvc.perform(post("/api/courses").with(user(principal(admin))).with(csrf()).contentType("application/json").content(body))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.code").value("CSE 3201")).andExpect(jsonPath("$.enrolledCount").value(0)).andReturn();
        String location = created.getResponse().getHeader("Location");
        mvc.perform(get(location).with(user(principal(student)))).andExpect(status().isOk()).andExpect(jsonPath("$.teacherName").value("Teacher One"));
        mvc.perform(post("/api/courses").with(user(principal(admin))).with(csrf()).contentType("application/json").content(body)).andExpect(status().isConflict());
        mvc.perform(put(location).with(user(principal(admin))).with(csrf()).contentType("application/json").content(courseBody("CSE 3202", 4, 40, otherTeacher.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.teacherId").value(otherTeacher.getId()));
        mvc.perform(delete(location).with(user(principal(admin))).with(csrf())).andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(get(location).with(user(principal(admin)))).andExpect(status().isNotFound());
        mvc.perform(post("/api/courses").with(user(principal(admin))).with(csrf()).contentType("application/json").content(courseBody("CSE 3201", 3, 40, student.getId())))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_TEACHER"));
        mvc.perform(post("/api/courses").with(user(principal(admin))).with(csrf()).contentType("application/json").content(courseBody("!", 0, 0, teacher.getId())))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.code").exists()).andExpect(jsonPath("$.fields.credits").exists());
    }

    @ParameterizedTest @EnumSource(value = Role.class, names = {"TEACHER", "STUDENT"})
    void academicWritesAreAdminOnlyExceptStudentEnrollment(Role role) throws Exception {
        var actor = principal(role == Role.TEACHER ? teacher : student);
        for (String path : List.of("/api/courses", "/api/notices")) {
            mvc.perform(post(path).with(user(actor)).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isForbidden());
            mvc.perform(put(path + "/1").with(user(actor)).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isForbidden());
            mvc.perform(delete(path + "/1").with(user(actor)).with(csrf())).andExpect(status().isForbidden());
        }
        if (role == Role.TEACHER) {
            mvc.perform(post("/api/enrollments").with(user(actor)).with(csrf()).contentType("application/json").content(enrollmentBody(course.getId(), student.getId())))
                    .andExpect(status().isForbidden());
        }
    }

    @Test void studentEnrollmentIsPrivateAndCannotImpersonateAnotherStudent() throws Exception {
        mvc.perform(post("/api/enrollments").with(user(principal(student))).with(csrf()).contentType("application/json").content(enrollmentBody(course.getId(), otherStudent.getId())))
                .andExpect(status().isForbidden());
        MvcResult result = mvc.perform(post("/api/enrollments").with(user(principal(student))).with(csrf()).contentType("application/json").content(enrollmentBody(course.getId(), null)))
                .andExpect(status().isCreated()).andExpect(header().exists("Location")).andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist()).andReturn();
        String location = result.getResponse().getHeader("Location");
        mvc.perform(get("/api/enrollments").with(user(principal(otherStudent)))).andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get(location).with(user(principal(otherStudent)))).andExpect(status().isNotFound());
        mvc.perform(delete(location).with(user(principal(otherStudent))).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get(location).with(user(principal(otherTeacher)))).andExpect(status().isNotFound());
        mvc.perform(get("/api/enrollments").with(user(principal(teacher)))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].studentEmail").value(student.getEmail()));
        mvc.perform(delete(location).with(user(principal(teacher))).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(delete(location).with(user(principal(student))).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(delete(location).with(user(principal(student))).with(csrf())).andExpect(status().isNotFound());
    }

    @Test void teachersSeeOnlyAssignedCoursesAndRostersAndReassignmentMovesAccess() throws Exception {
        Enrollment first = enrollments.saveAndFlush(new Enrollment(course, student));
        enrollments.saveAndFlush(new Enrollment(otherCourse, otherStudent));
        mvc.perform(get("/api/courses").with(user(principal(teacher)))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(course.getId())).andExpect(jsonPath("$[0].enrolledCount").value(1));
        mvc.perform(get("/api/courses/" + otherCourse.getId()).with(user(principal(teacher)))).andExpect(status().isNotFound());
        mvc.perform(get("/api/enrollments").with(user(principal(teacher)))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(put("/api/courses/" + course.getId()).with(user(principal(admin))).with(csrf()).contentType("application/json")
                .content(courseBody(course.getCode(), 2, 2, otherTeacher.getId()))).andExpect(status().isOk());
        mvc.perform(get("/api/enrollments/" + first.getId()).with(user(principal(teacher)))).andExpect(status().isNotFound());
        mvc.perform(get("/api/enrollments/" + first.getId()).with(user(principal(otherTeacher)))).andExpect(status().isOk());
        mvc.perform(get("/api/courses").with(user(principal(student)))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }

    @Test void duplicatesCapacityAndDependentDeletesReturnConflict() throws Exception {
        Enrollment first = enrollments.saveAndFlush(new Enrollment(course, student));
        enrollments.saveAndFlush(new Enrollment(course, otherStudent));
        mvc.perform(post("/api/enrollments").with(user(principal(student))).with(csrf()).contentType("application/json").content(enrollmentBody(course.getId(), null)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ALREADY_ENROLLED"));
        mvc.perform(put("/api/courses/" + course.getId()).with(user(principal(admin))).with(csrf()).contentType("application/json").content(courseBody(course.getCode(), 2, 1, teacher.getId())))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CAPACITY_CONFLICT"));
        mvc.perform(delete("/api/courses/" + course.getId()).with(user(principal(admin))).with(csrf())).andExpect(status().isConflict());
        mvc.perform(delete("/api/teachers/" + teacher.getId()).with(user(principal(admin))).with(csrf())).andExpect(status().isConflict());
        mvc.perform(delete("/api/students/" + student.getId()).with(user(principal(admin))).with(csrf())).andExpect(status().isConflict());
        mvc.perform(delete("/api/enrollments/" + first.getId()).with(user(principal(admin))).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(delete("/api/students/" + student.getId()).with(user(principal(admin))).with(csrf())).andExpect(status().isNoContent());
        assertThat(enrollments.countByCourseId(course.getId())).isEqualTo(1);
    }

    @Test void concurrentEnrollmentCannotOverbookTheLastSeat() throws Exception {
        course.update(course.getCode(), course.getTitle(), 2, 1, teacher);
        courses.saveAndFlush(course);
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (UserAccount actor : List.of(student, otherStudent)) {
                results.add(threads.submit(() -> asAccount(actor, () -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Concurrent test did not start");
                    try { enrollmentService.create(new EnrollmentRequest(course.getId(), null)); return 201; }
                    catch (ApiException ex) { return ex.getStatus().value(); }
                })));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); start.countDown();
            assertThat(List.of(results.get(0).get(10, TimeUnit.SECONDS), results.get(1).get(10, TimeUnit.SECONDS))).containsExactlyInAnyOrder(201, 409);
            assertThat(enrollments.countByCourseId(course.getId())).isEqualTo(1);
        } finally { start.countDown(); threads.shutdownNow(); }
    }

    @Test void noticesHaveCrudAndStayReadableAfterTheirAuthorIsDeleted() throws Exception {
        MvcResult created = mvc.perform(post("/api/notices").with(user(principal(admin))).with(csrf()).contentType("application/json")
                .content("{\"title\":\"Registration open\",\"body\":\"Choose your courses from the catalog.\"}"))
                .andExpect(status().isCreated()).andExpect(header().exists("Location")).andExpect(jsonPath("$.publishedAt").exists()).andReturn();
        String location = created.getResponse().getHeader("Location");
        for (UserAccount actor : List.of(admin, teacher, student)) {
            mvc.perform(get(location).with(user(principal(actor)))).andExpect(status().isOk()).andExpect(jsonPath("$.authorName").value(admin.getFullName()));
        }
        mvc.perform(put(location).with(user(principal(admin))).with(csrf()).contentType("application/json").content("{\"title\":\"Updated notice\",\"body\":\"Updated registration information.\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Updated notice"));
        UserAccount secondAdmin = accounts.saveAndFlush(new UserAccount("Second Admin", "admin2@test.edu", admin.getPasswordHash(), Role.ADMIN));
        mvc.perform(delete("/api/admins/" + admin.getId()).with(user(principal(secondAdmin))).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get(location).with(user(principal(student)))).andExpect(status().isOk()).andExpect(jsonPath("$.authorName").value("University office"));
        mvc.perform(delete(location).with(user(principal(secondAdmin))).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get(location).with(user(principal(student)))).andExpect(status().isNotFound());
    }

    @Test void missingInvalidAndUnknownFieldsHaveProperStatusCodes() throws Exception {
        var actor = principal(admin);
        mvc.perform(post("/api/enrollments").with(user(actor)).with(csrf()).contentType("application/json").content(enrollmentBody(course.getId(), null))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/enrollments").with(user(actor)).with(csrf()).contentType("application/json").content(enrollmentBody(course.getId(), teacher.getId()))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/enrollments").with(user(principal(student))).with(csrf()).contentType("application/json").content(enrollmentBody(Long.MAX_VALUE, null))).andExpect(status().isNotFound());
        mvc.perform(post("/api/enrollments").with(user(principal(student))).with(csrf()).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.courseId").exists());
        ObjectNode fractional = (ObjectNode) json.readTree(courseBody("CSE 3201", 3, 40, teacher.getId()));
        fractional.put("credits", 2.5);
        mvc.perform(post("/api/courses").with(user(actor)).with(csrf()).contentType("application/json").content(json.writeValueAsString(fractional)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/notices").with(user(actor)).with(csrf()).contentType("application/json").content("{\"title\":\"Hi\",\"body\":\"Body\",\"authorId\":123}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/notices").with(user(actor)).with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("title", " ", "body", " "))))
                .andExpect(status().isBadRequest());
        for (String resource : List.of("courses", "enrollments", "notices")) {
            mvc.perform(get("/api/" + resource + "/" + Long.MAX_VALUE).with(user(actor))).andExpect(status().isNotFound());
            mvc.perform(get("/api/" + resource)).andExpect(status().isUnauthorized());
            mvc.perform(post("/api/" + resource).with(user(actor)).contentType("application/json").content("{}"))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_ERROR"));
        }
    }

    @Test void servicesEnforceRoleAndOwnershipWithoutDependingOnTheController() throws Exception {
        Enrollment other = enrollments.saveAndFlush(new Enrollment(otherCourse, otherStudent));
        asAccount(student, () -> {
            forbidden(() -> courseService.create(new CourseRequest("CSE 1111", "Course", 2, 20, teacher.getId())), 403);
            forbidden(() -> noticeService.create(new NoticeRequest("Notice", "Notice body")), 403);
            forbidden(() -> enrollmentService.get(other.getId()), 404);
            forbidden(() -> enrollmentService.delete(other.getId()), 404);
            return null;
        });
        asAccount(teacher, () -> {
            forbidden(() -> courseService.get(otherCourse.getId()), 404);
            forbidden(() -> enrollmentService.create(new EnrollmentRequest(course.getId(), student.getId())), 403);
            forbidden(() -> enrollmentService.delete(other.getId()), 403);
            assertThat(enrollmentService.list()).isEmpty();
            return null;
        });
    }

    private void forbidden(Runnable action, int status) {
        assertThatThrownBy(action::run).isInstanceOf(ApiException.class).extracting(ex -> ((ApiException) ex).getStatus().value()).isEqualTo(status);
    }
    private AccountPrincipal principal(UserAccount account) { return new AccountPrincipal(account); }
    private <T> T asAccount(UserAccount actor, Callable<T> action) throws Exception {
        var original = SecurityContextHolder.getContext();
        var context = SecurityContextHolder.createEmptyContext();
        var principal = principal(actor);
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
        SecurityContextHolder.setContext(context);
        try { return action.call(); } finally { SecurityContextHolder.setContext(original); }
    }
    private String courseBody(String code, int credits, int capacity, Long teacherId) throws Exception {
        return json.writeValueAsString(Map.of("code", code, "title", "Test Course", "credits", credits, "capacity", capacity, "teacherId", teacherId));
    }
    private String enrollmentBody(Long courseId, Long studentId) throws Exception {
        Map<String, Long> values = new LinkedHashMap<>(); values.put("courseId", courseId);
        if (studentId != null) values.put("studentId", studentId);
        return json.writeValueAsString(values);
    }
}
