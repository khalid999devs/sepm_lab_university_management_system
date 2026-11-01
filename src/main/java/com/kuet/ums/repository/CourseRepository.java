package com.kuet.ums.repository;

import com.kuet.ums.entity.Course;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface CourseRepository extends JpaRepository<Course, Long> {
    @EntityGraph(attributePaths = "teacher")
    List<Course> findAllByOrderByCodeAsc();
    @EntityGraph(attributePaths = "teacher")
    List<Course> findAllByTeacherIdOrderByCodeAsc(Long teacherId);
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Course c where c.id = :id")
    Optional<Course> lockById(@Param("id") Long id);
}
