package com.example.shortener.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "url_mapping")
public class UrlMapping {

    @Id
    private UUID id;

    @Column(name = "short_code", nullable = false, unique = true, length = 32)
    private String shortCode;

    @Column(name = "original_url", nullable = false, length = 2048)
    private String originalUrl;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "total_clicks", nullable = false)
    private long totalClicks;

    @Column(name = "last_accessed_at")
    private Instant lastAccessedAt;

    /**
     * Reserved for entity-based updates. Click analytics use a bulk JPQL increment
     * that also bumps {@code version} so optimistic locking stays consistent if
     * entity updates are added later (see ADR-011).
     */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected UrlMapping() {
    }

    public UrlMapping(
            UUID id,
            String shortCode,
            String originalUrl,
            Instant expiresAt,
            Instant createdAt
    ) {
        this.id = id;
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }

    public UUID getId() {
        return id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public long getTotalClicks() {
        return totalClicks;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }
}
