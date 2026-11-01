package com.kuet.ums.dto;

import com.kuet.ums.entity.Enrollment;
import java.time.Instant;

public record EnrollmentResponse(
    Long id,
    Long courseId,
    String courseCode,
    String courseTitle,
    int credits,
    String teacherName,
    Long studentId,
    String studentName,
    String studentEmail,
    Instant enrolledAt
) {
    public static EnrollmentResponse from(Enrollment enrollment) {
        return new EnrollmentResponse(
            enrollment.getId(),
            enrollment.getCourse().getId(),
            enrollment.getCourse().getCode(),
            enrollment.getCourse().getTitle(),
            enrollment.getCourse().getCredits(),
            enrollment.getCourse().getTeacher().getFullName(),
            enrollment.getStudent().getId(),
            enrollment.getStudent().getFullName(),
            enrollment.getStudent().getEmail(),
            enrollment.getEnrolledAt()
        );
    }
}
