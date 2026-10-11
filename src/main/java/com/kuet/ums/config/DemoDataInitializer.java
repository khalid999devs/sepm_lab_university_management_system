package com.kuet.ums.config;

import com.kuet.ums.entity.*;
import com.kuet.ums.repository.AccountRepository;
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
    private final String adminPassword;
    private final String teacherPassword;
    private final String studentPassword;

    public DemoDataInitializer(AccountRepository accounts, PasswordEncoder passwords,
            @Value("${DEMO_ADMIN_PASSWORD}") String adminPassword,
            @Value("${DEMO_TEACHER_PASSWORD}") String teacherPassword,
            @Value("${DEMO_STUDENT_PASSWORD}") String studentPassword) {
        this.accounts = accounts;
        this.passwords = passwords;
        this.adminPassword = adminPassword;
        this.teacherPassword = teacherPassword;
        this.studentPassword = studentPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (accounts.count() != 0) return;
        seed("Demo Admin", "admin@ums.test", adminPassword, Role.ADMIN);
        seed("Demo Teacher", "teacher@ums.test", teacherPassword, Role.TEACHER);
        seed("Demo Student", "student@ums.test", studentPassword, Role.STUDENT);
    }

    private void seed(String name, String email, String password, Role role) {
        if (!PasswordValidator.isValidPassword(password)) {
            throw new IllegalStateException("Demo passwords must have at least 8 characters and at most 72 UTF-8 bytes.");
        }
        accounts.save(new UserAccount(name, email, passwords.encode(password), role));
    }
}
