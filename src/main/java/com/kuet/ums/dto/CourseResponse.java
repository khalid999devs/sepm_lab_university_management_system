package com.kuet.ums.dto;

import com.kuet.ums.entity.Course;

public record CourseResponse(Long id, String code, String title, int credits, int capacity,
                             Long teacherId, String teacherName, long enrolledCount) {
    public static CourseResponse from(Course course, long enrolled) {
        return new CourseResponse(course.getId(), course.getCode(), course.getTitle(), course.getCredits(),
                course.getCapacity(), course.getTeacher().getId(), course.getTeacher().getFullName(), enrolled);
    }
}
