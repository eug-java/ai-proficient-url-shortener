package com.example.shortener.customdomain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "org_custom_domain")
public class OrgCustomDomain {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false, length = 253)
    private String hostname;

    @Column(name = "verification_token", nullable = false, length = 64)
    private String verificationToken;

    @Column(nullable = false)
    private boolean verified;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OrgCustomDomain() {}

    public OrgCustomDomain(
            UUID id,
            UUID organizationId,
            String hostname,
            String verificationToken,
            Instant createdAt
    ) {
        this.id = id;
        this.organizationId = organizationId;
        this.hostname = hostname;
        this.verificationToken = verificationToken;
        this.verified = false;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getHostname() {
        return hostname;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public boolean isVerified() {
        return verified;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void markVerified(Instant at) {
        this.verified = true;
        this.verifiedAt = at;
    }
}
