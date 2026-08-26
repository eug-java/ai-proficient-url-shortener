package com.example.shortener.org;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organization")
public class Organization {
    @Id private UUID id;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, unique = true, length = 64) private String slug;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected Organization() {}
    public Organization(UUID id, String name, String slug, Instant createdAt) {
        this.id = id; this.name = name; this.slug = slug; this.createdAt = createdAt;
    }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public Instant getCreatedAt() { return createdAt; }
}
