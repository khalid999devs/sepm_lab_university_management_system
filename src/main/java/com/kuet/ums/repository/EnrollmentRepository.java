package com.kuet.ums.repository;

import com.kuet.ums.entity.Enrollment;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    @EntityGraph(attributePaths = { "course.teacher", "student" })
    List<Enrollment> findAllByOrderByIdDesc();

    @EntityGraph(attributePaths = { "course.teacher", "student" })
    List<Enrollment> findAllByStudentIdOrderByIdDesc(Long studentId);

    @EntityGraph(attributePaths = { "course.teacher", "student" })
    List<Enrollment> findAllByCourseTeacherIdOrderByIdDesc(Long teacherId);

    long countByCourseId(Long courseId);
    boolean existsByCourseIdAndStudentId(Long courseId, Long studentId);

    @Query(
        "select e.course.id as courseId, count(e) as total from Enrollment e group by e.course.id"
    )
    List<CourseCount> countByCourse();

    interface CourseCount {
        Long getCourseId();
        long getTotal();
    }
}
