package com.example.shortener.integration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "org_webhook_endpoint")
public class OrgWebhookEndpoint {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "target_url", nullable = false, columnDefinition = "text")
    private String targetUrl;

    @Column(nullable = false, length = 128)
    private String secret;

    @Column(nullable = false, columnDefinition = "text")
    private String events;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OrgWebhookEndpoint() {}

    public OrgWebhookEndpoint(
            UUID id,
            UUID organizationId,
            String targetUrl,
            String secret,
            String events,
            Instant createdAt
    ) {
        this.id = id;
        this.organizationId = organizationId;
        this.targetUrl = targetUrl;
        this.secret = secret;
        this.events = events;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public String getSecret() {
        return secret;
    }

    public String getEvents() {
        return events;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Set<String> eventSet() {
        return Arrays.stream(events.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    public boolean accepts(String action) {
        Set<String> set = eventSet();
        return set.contains("*") || set.contains(action);
    }

    public void disable() {
        this.enabled = false;
    }
}
