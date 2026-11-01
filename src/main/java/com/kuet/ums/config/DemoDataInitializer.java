package com.kuet.ums.config;

import com.kuet.ums.entity.*;
import com.kuet.ums.repository.AccountRepository;
import com.kuet.ums.repository.CourseRepository;
import com.kuet.ums.repository.EnrollmentRepository;
import com.kuet.ums.repository.NoticeRepository;
import com.kuet.ums.validation.PasswordValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("demo")
public class DemoDataInitializer implements ApplicationRunner {
    private final AccountRepository accounts;
    private final PasswordEncoder passwords;
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final NoticeRepository notices;
    private final String adminPassword;
    private final String teacherPassword;
    private final String studentPassword;

    public DemoDataInitializer(AccountRepository accounts, PasswordEncoder passwords,
            CourseRepository courses, EnrollmentRepository enrollments, NoticeRepository notices,
            @Value("${DEMO_ADMIN_PASSWORD}") String adminPassword,
            @Value("${DEMO_TEACHER_PASSWORD}") String teacherPassword,
            @Value("${DEMO_STUDENT_PASSWORD}") String studentPassword) {
        this.accounts = accounts;
        this.passwords = passwords;
        this.courses = courses;
        this.enrollments = enrollments;
        this.notices = notices;
        this.adminPassword = adminPassword;
        this.teacherPassword = teacherPassword;
        this.studentPassword = studentPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (accounts.count() != 0) return;
        UserAccount admin = seed("Demo Admin", "admin@ums.test", adminPassword, Role.ADMIN);
        UserAccount teacher = seed("Demo Teacher", "teacher@ums.test", teacherPassword, Role.TEACHER);
        UserAccount student = seed("Demo Student", "student@ums.test", studentPassword, Role.STUDENT);
        Course web = courses.save(new Course("CSE 3230", "Web Programming Laboratory", 2, 40, teacher));
        courses.save(new Course("CSE 3211", "Software Engineering", 3, 40, teacher));
        courses.save(new Course("CSE 3201", "Database Management Systems", 3, 40, teacher));
        enrollments.save(new Enrollment(web, student));
        notices.save(new Notice("Welcome to the university portal", "Students can browse courses and manage their enrollments. Teachers can review assigned courses and student rosters. Keep your profile details up to date.", admin));
    }

    private UserAccount seed(String name, String email, String password, Role role) {
        if (!PasswordValidator.isValidPassword(password)) {
            throw new IllegalStateException("Demo passwords must have at least 8 characters and at most 72 UTF-8 bytes.");
        }
        return accounts.save(new UserAccount(name, email, passwords.encode(password), role));
    }
}
