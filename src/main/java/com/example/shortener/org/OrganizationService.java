package com.example.shortener.org;

import com.example.shortener.audit.AuditService;
import com.example.shortener.domain.InvalidRequestException;
import com.example.shortener.security.CurrentUser;
import com.example.shortener.security.KeycloakUserDirectory;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {

    public record OrgView(UUID id, String name, String slug, String role, java.time.Instant createdAt) {}

    public record MemberView(
            UUID id,
            String userSub,
            String email,
            String displayName,
            String role
    ) {}

    private final OrganizationRepository organizations;
    private final OrganizationMemberRepository members;
    private final OrgAccessService access;
    private final KeycloakUserDirectory userDirectory;
    private final AuditService audit;
    private final Clock clock;

    public OrganizationService(
            OrganizationRepository organizations,
            OrganizationMemberRepository members,
            OrgAccessService access,
            KeycloakUserDirectory userDirectory,
            AuditService audit,
            Clock clock
    ) {
        this.organizations = organizations;
        this.members = members;
        this.access = access;
        this.userDirectory = userDirectory;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public OrgView create(String name, String slug, CurrentUser.User user) {
        String normalized = slug.toLowerCase(Locale.ROOT).trim();
        if (!normalized.matches("[a-z0-9][a-z0-9-]{1,62}[a-z0-9]") || organizations.existsBySlug(normalized)) {
            throw new InvalidRequestException("Organization slug is invalid or already used");
        }
        Organization org = organizations.save(
                new Organization(UUID.randomUUID(), name.trim(), normalized, clock.instant())
        );
        members.save(new OrganizationMember(
                UUID.randomUUID(),
                org.getId(),
                user.sub(),
                user.email(),
                user.name(),
                OrganizationMember.Role.OWNER,
                clock.instant()
        ));
        audit.record(org.getId(), user.sub(), "ORG_CREATED", "Organization", org.getId().toString(),
                Map.of("slug", normalized, "name", name.trim()));
        return new OrgView(org.getId(), org.getName(), org.getSlug(), "OWNER", org.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<OrgView> mine(String sub) {
        return members.findAllByUserSub(sub).stream()
                .map(m -> {
                    Organization org = organizations.findById(m.getOrganizationId()).orElseThrow();
                    return new OrgView(
                            org.getId(),
                            org.getName(),
                            org.getSlug(),
                            m.getRole().name(),
                            org.getCreatedAt()
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MemberView> members(UUID orgId, String sub) {
        access.requireMember(orgId, sub);
        return members.findAllByOrganizationId(orgId).stream()
                .map(m -> new MemberView(
                        m.getId(),
                        m.getUserSub(),
                        m.getEmail(),
                        m.getDisplayName(),
                        m.getRole().name()
                ))
                .toList();
    }

    @Transactional
    public MemberView inviteByEmail(
            UUID orgId,
            String actor,
            String email,
            OrganizationMember.Role role
    ) {
        access.requireManage(orgId, actor);
        if (role == OrganizationMember.Role.OWNER) {
            throw new InvalidRequestException("Cannot assign OWNER via invite; transfer ownership separately");
        }
        KeycloakUserDirectory.DirectoryUser user = userDirectory.findOrInviteByEmail(email);
        return addResolved(orgId, actor, user.sub(), user.email(), user.displayName(), role, "MEMBER_INVITED");
    }

    @Transactional
    public MemberView add(
            UUID orgId,
            String actor,
            String sub,
            String email,
            String name,
            OrganizationMember.Role role
    ) {
        access.requireManage(orgId, actor);
        if (role == OrganizationMember.Role.OWNER) {
            throw new InvalidRequestException("Cannot assign OWNER via invite; transfer ownership separately");
        }
        return addResolved(orgId, actor, sub, email, name, role, "MEMBER_ADDED");
    }

    private MemberView addResolved(
            UUID orgId,
            String actor,
            String sub,
            String email,
            String name,
            OrganizationMember.Role role,
            String auditAction
    ) {
        if (members.findByOrganizationIdAndUserSub(orgId, sub).isPresent()) {
            throw new InvalidRequestException("User is already a member of this organization");
        }
        OrganizationMember saved = members.save(new OrganizationMember(
                UUID.randomUUID(),
                orgId,
                sub,
                email,
                name,
                role,
                clock.instant()
        ));
        audit.record(orgId, actor, auditAction, "Member", saved.getId().toString(),
                Map.of("userSub", sub, "role", role.name()));
        return new MemberView(
                saved.getId(),
                saved.getUserSub(),
                saved.getEmail(),
                saved.getDisplayName(),
                saved.getRole().name()
        );
    }

    @Transactional
    public MemberView role(UUID orgId, UUID memberId, String actor, OrganizationMember.Role role) {
        access.requireManage(orgId, actor);
        OrganizationMember member = members.findById(memberId)
                .orElseThrow(() -> new InvalidRequestException("Member not found"));
        if (!member.getOrganizationId().equals(orgId)) {
            throw new InvalidRequestException("Member not in organization");
        }
        if (role == OrganizationMember.Role.OWNER) {
            throw new InvalidRequestException("Cannot promote to OWNER via this endpoint; use transfer");
        }
        if (member.getRole() == OrganizationMember.Role.OWNER) {
            long owners = members.findAllByOrganizationId(orgId).stream()
                    .filter(m -> m.getRole() == OrganizationMember.Role.OWNER)
                    .count();
            if (owners <= 1) {
                throw new InvalidRequestException("Cannot demote the last OWNER");
            }
        }
        member.changeRole(role);
        audit.record(orgId, actor, "MEMBER_ROLE_CHANGED", "Member", memberId.toString(),
                Map.of("role", role.name(), "userSub", member.getUserSub()));
        return new MemberView(
                member.getId(),
                member.getUserSub(),
                member.getEmail(),
                member.getDisplayName(),
                member.getRole().name()
        );
    }

    @Transactional
    public MemberView transferOwnership(UUID orgId, String actorSub, String newOwnerSub) {
        access.requireManage(orgId, actorSub);
        OrganizationMember current = members.findByOrganizationIdAndUserSub(orgId, actorSub)
                .orElseThrow(() -> new AccessDeniedException("Organization membership required"));
        if (current.getRole() != OrganizationMember.Role.OWNER) {
            throw new AccessDeniedException("Owner role required");
        }
        OrganizationMember target = members.findByOrganizationIdAndUserSub(orgId, newOwnerSub)
                .orElseThrow(() -> new InvalidRequestException("Target member not found"));
        current.changeRole(OrganizationMember.Role.ADMIN);
        target.changeRole(OrganizationMember.Role.OWNER);
        audit.record(orgId, actorSub, "OWNERSHIP_TRANSFERRED", "Organization", orgId.toString(),
                Map.of("newOwnerSub", newOwnerSub));
        return new MemberView(
                target.getId(),
                target.getUserSub(),
                target.getEmail(),
                target.getDisplayName(),
                target.getRole().name()
        );
    }
}
