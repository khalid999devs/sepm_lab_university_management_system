package com.kuet.ums.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kuet.ums.dto.EnrollmentRequest;
import com.kuet.ums.entity.Course;
import com.kuet.ums.entity.Enrollment;
import com.kuet.ums.entity.Role;
import com.kuet.ums.entity.UserAccount;
import com.kuet.ums.exception.ApiException;
import com.kuet.ums.repository.AccountRepository;
import com.kuet.ums.repository.CourseRepository;
import com.kuet.ums.repository.EnrollmentRepository;
import com.kuet.ums.security.AccountAccess;
import com.kuet.ums.security.AccountPrincipal;
import com.kuet.ums.service.EnrollmentService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

// Tests the enrollment rules with fake (mocked) repositories, so no database is needed.
class EnrollmentServiceUnitTest {

    private final EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
    private final CourseRepository courses = mock(CourseRepository.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final AccountAccess access = mock(AccountAccess.class);
    private final EnrollmentService service = new EnrollmentService(
        enrollments,
        courses,
        accounts,
        access
    );

    @Test
    void studentCannotEnrollAnotherStudent() {
        signInAsStudent(4L);

        // Student 4 asks to enroll student 9 in course 1.
        var request = new EnrollmentRequest(1L, 9L);

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOfSatisfying(ApiException.class, error -> {
                assertThat(error.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                assertThat(error.getCode()).isEqualTo("FORBIDDEN");
            });

        verify(enrollments, never()).saveAndFlush(any(Enrollment.class));
    }

    @Test
    void enrollingInAFullCourseFails() {
        signInAsStudent(4L);

        // The course has 30 seats and 30 students are already enrolled.
        Course fullCourse = mock(Course.class);
        when(fullCourse.getId()).thenReturn(1L);
        when(fullCourse.getCapacity()).thenReturn(30);
        when(accounts.findByIdAndRole(4L, Role.STUDENT)).thenReturn(
            Optional.of(mock(UserAccount.class))
        );
        when(courses.lockById(1L)).thenReturn(Optional.of(fullCourse));
        when(enrollments.countByCourseId(1L)).thenReturn(30L);

        assertThatThrownBy(() -> service.create(new EnrollmentRequest(1L, null)))
            .isInstanceOfSatisfying(ApiException.class, error -> {
                assertThat(error.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                assertThat(error.getCode()).isEqualTo("COURSE_FULL");
            });

        verify(enrollments, never()).saveAndFlush(any(Enrollment.class));
    }

    private void signInAsStudent(Long id) {
        AccountPrincipal student = mock(AccountPrincipal.class);
        when(student.getRole()).thenReturn(Role.STUDENT);
        when(student.getId()).thenReturn(id);
        when(access.currentAccount()).thenReturn(student);
    }
}
