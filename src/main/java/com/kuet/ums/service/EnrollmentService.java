package com.kuet.ums.service;

import com.kuet.ums.dto.*;
import com.kuet.ums.entity.*;
import com.kuet.ums.exception.ApiException;
import com.kuet.ums.repository.*;
import com.kuet.ums.security.AccountAccess;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class EnrollmentService {
    private final EnrollmentRepository enrollments;
    private final CourseRepository courses;
    private final AccountRepository accounts;
    private final AccountAccess access;

    public EnrollmentService(EnrollmentRepository enrollments, CourseRepository courses, AccountRepository accounts, AccountAccess access) {
        this.enrollments = enrollments;
        this.courses = courses;
        this.accounts = accounts;
        this.access = access;
    }

    public List<EnrollmentResponse> list() {
        var actor = access.currentAccount();
        List<Enrollment> records = switch (actor.getRole()) {
            case ADMIN -> enrollments.findAllByOrderByIdDesc();
            case TEACHER -> enrollments.findAllByCourseTeacherIdOrderByIdDesc(actor.getId());
            case STUDENT -> enrollments.findAllByStudentIdOrderByIdDesc(actor.getId());
        };
        return records.stream().map(EnrollmentResponse::from).toList();
    }

    public EnrollmentResponse get(Long id) { return EnrollmentResponse.from(requireVisible(id)); }

    @Transactional
    public EnrollmentResponse create(EnrollmentRequest request) {
        access.requireRole(Role.ADMIN, Role.STUDENT);
        var actor = access.currentAccount();
        Long studentId = request.studentId();
        if (actor.getRole() == Role.STUDENT) {
            if (studentId != null && !studentId.equals(actor.getId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You can enroll only your own account.");
            }
            studentId = actor.getId();
        }
        if (studentId == null) throw new ApiException(HttpStatus.BAD_REQUEST, "STUDENT_REQUIRED", "Select a student account.");
        UserAccount student = accounts.findByIdAndRole(studentId, Role.STUDENT).orElseThrow(() ->
                new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STUDENT", "Select an existing student account."));
        // Every enrollment/capacity change locks this row until its short transaction commits.
        Course course = courses.lockById(request.courseId()).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Course not found."));
        if (enrollments.existsByCourseIdAndStudentId(course.getId(), studentId)) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_ENROLLED", "This student is already enrolled in the course.");
        }
        if (enrollments.countByCourseId(course.getId()) >= course.getCapacity()) {
            throw new ApiException(HttpStatus.CONFLICT, "COURSE_FULL", "This course has no seats available.");
        }
        return EnrollmentResponse.from(enrollments.saveAndFlush(new Enrollment(course, student)));
    }

    @Transactional
    public void delete(Long id) {
        access.requireRole(Role.ADMIN, Role.STUDENT);
        Enrollment enrollment = requireVisible(id);
        courses.lockById(enrollment.getCourse().getId()).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Course not found."));
        enrollments.delete(enrollment);
        enrollments.flush();
    }

    private Enrollment requireVisible(Long id) {
        var actor = access.currentAccount();
        Enrollment enrollment = enrollments.findById(id).orElseThrow(this::notFound);
        boolean visible = switch (actor.getRole()) {
            case ADMIN -> true;
            case STUDENT -> enrollment.getStudent().getId().equals(actor.getId());
            case TEACHER -> enrollment.getCourse().getTeacher().getId().equals(actor.getId());
        };
        if (!visible) throw notFound();
        return enrollment;
    }
    private ApiException notFound() { return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Enrollment not found."); }
}
