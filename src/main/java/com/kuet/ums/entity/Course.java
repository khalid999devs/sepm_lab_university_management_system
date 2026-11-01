package com.kuet.ums.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "courses")
public class Course {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, columnDefinition = "text")
    private String code;
    @Column(nullable = false, columnDefinition = "text")
    private String title;
    @Column(nullable = false)
    private int credits;
    @Column(nullable = false)
    private int capacity;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private UserAccount teacher;

    protected Course() { }
    public Course(String code, String title, int credits, int capacity, UserAccount teacher) {
        update(code, title, credits, capacity, teacher);
    }
    public void update(String code, String title, int credits, int capacity, UserAccount teacher) {
        this.code = code;
        this.title = title;
        this.credits = credits;
        this.capacity = capacity;
        this.teacher = teacher;
    }
    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getTitle() { return title; }
    public int getCredits() { return credits; }
    public int getCapacity() { return capacity; }
    public UserAccount getTeacher() { return teacher; }
}
