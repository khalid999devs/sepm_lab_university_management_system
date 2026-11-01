package com.kuet.ums.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "enrollments", uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "student_id"}))
public class Enrollment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private UserAccount student;
    @Column(name = "enrolled_at", nullable = false)
    private Instant enrolledAt;

    protected Enrollment() { }
    public Enrollment(Course course, UserAccount student) {
        this.course = course;
        this.student = student;
        this.enrolledAt = Instant.now();
    }
    public Long getId() { return id; }
    public Course getCourse() { return course; }
    public UserAccount getStudent() { return student; }
    public Instant getEnrolledAt() { return enrolledAt; }
}
