package com.kuet.ums.service;

import com.kuet.ums.dto.*;
import com.kuet.ums.entity.*;
import com.kuet.ums.exception.ApiException;
import com.kuet.ums.repository.*;
import com.kuet.ums.security.AccountAccess;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CourseService {
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final AccountRepository accounts;
    private final AccountAccess access;

    public CourseService(CourseRepository courses, EnrollmentRepository enrollments, AccountRepository accounts, AccountAccess access) {
        this.courses = courses;
        this.enrollments = enrollments;
        this.accounts = accounts;
        this.access = access;
    }

    public List<CourseResponse> list() {
        var actor = access.currentAccount();
        List<Course> records = actor.getRole() == Role.TEACHER
                ? courses.findAllByTeacherIdOrderByCodeAsc(actor.getId()) : courses.findAllByOrderByCodeAsc();
        Map<Long, Long> counts = enrollments.countByCourse().stream()
                .collect(Collectors.toMap(EnrollmentRepository.CourseCount::getCourseId, EnrollmentRepository.CourseCount::getTotal));
        return records.stream().map(course -> CourseResponse.from(course, counts.getOrDefault(course.getId(), 0L))).toList();
    }

    public CourseResponse get(Long id) {
        var actor = access.currentAccount();
        Course course = courses.findById(id).orElseThrow(this::notFound);
        if (actor.getRole() == Role.TEACHER && !course.getTeacher().getId().equals(actor.getId())) throw notFound();
        return CourseResponse.from(course, enrollments.countByCourseId(id));
    }

    @Transactional
    public CourseResponse create(CourseRequest request) {
        access.requireRole(Role.ADMIN);
        if (courses.existsByCode(request.code())) throw duplicateCode();
        Course course = courses.saveAndFlush(new Course(request.code(), request.title(), request.credits(), request.capacity(), teacher(request.teacherId())));
        return CourseResponse.from(course, 0);
    }

    @Transactional
    public CourseResponse update(Long id, CourseRequest request) {
        access.requireRole(Role.ADMIN);
        Course course = courses.lockById(id).orElseThrow(this::notFound);
        if (courses.existsByCodeAndIdNot(request.code(), id)) throw duplicateCode();
        long enrolled = enrollments.countByCourseId(id);
        if (request.capacity() < enrolled) {
            throw new ApiException(HttpStatus.CONFLICT, "CAPACITY_CONFLICT", "Capacity cannot be lower than the number of enrolled students.");
        }
        course.update(request.code(), request.title(), request.credits(), request.capacity(), teacher(request.teacherId()));
        courses.flush();
        return CourseResponse.from(course, enrolled);
    }

    @Transactional
    public void delete(Long id) {
        access.requireRole(Role.ADMIN);
        Course course = courses.lockById(id).orElseThrow(this::notFound);
        if (enrollments.countByCourseId(id) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "COURSE_IN_USE", "Withdraw the enrolled students before deleting this course.");
        }
        courses.delete(course);
        courses.flush();
    }

    private UserAccount teacher(Long id) {
        return accounts.findByIdAndRole(id, Role.TEACHER).orElseThrow(() ->
                new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TEACHER", "Select an existing teacher account."));
    }
    private ApiException notFound() { return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Course not found."); }
    private ApiException duplicateCode() { return new ApiException(HttpStatus.CONFLICT, "DUPLICATE_CODE", "A course with this code already exists."); }
}
