package com.example.shortener.org;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organization_member")
public class OrganizationMember {
    public enum Role { OWNER, ADMIN, MEMBER, VIEWER }
    @Id private UUID id;
    @Column(name = "organization_id", nullable = false) private UUID organizationId;
    @Column(name = "user_sub", nullable = false, length = 128) private String userSub;
    @Column(length = 320) private String email;
    @Column(name = "display_name", length = 160) private String displayName;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private Role role;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected OrganizationMember() {}
    public OrganizationMember(UUID id, UUID organizationId, String userSub, String email,
                              String displayName, Role role, Instant createdAt) {
        this.id=id; this.organizationId=organizationId; this.userSub=userSub; this.email=email;
        this.displayName=displayName; this.role=role; this.createdAt=createdAt;
    }
    public UUID getId(){return id;} public UUID getOrganizationId(){return organizationId;}
    public String getUserSub(){return userSub;} public String getEmail(){return email;}
    public String getDisplayName(){return displayName;} public Role getRole(){return role;}
    public Instant getCreatedAt(){return createdAt;}
    public void changeRole(Role role){this.role=role;}
}
