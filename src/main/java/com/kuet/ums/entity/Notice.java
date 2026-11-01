package com.kuet.ums.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notices")
public class Notice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, columnDefinition = "text")
    private String title;
    @Column(nullable = false, columnDefinition = "text")
    private String body;
    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private UserAccount author;

    protected Notice() { }
    public Notice(String title, String body, UserAccount author) {
        update(title, body);
        this.author = author;
        this.publishedAt = Instant.now();
    }
    public void update(String title, String body) { this.title = title; this.body = body; }
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public Instant getPublishedAt() { return publishedAt; }
    public UserAccount getAuthor() { return author; }
}
