package com.example.shortener.org;

import com.example.shortener.domain.NotFoundException;
import com.example.shortener.integration.ApiKeyRequestContext;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class OrgAccessService {
    private final OrganizationMemberRepository members;

    public OrgAccessService(OrganizationMemberRepository members) {
        this.members = members;
    }

    public OrganizationMember requireMember(UUID orgId, String sub) {
        if (isApiKeyForOrg(orgId, sub)) {
            return syntheticOwner(orgId, sub);
        }
        return members.findByOrganizationIdAndUserSub(orgId, sub)
                .orElseThrow(() -> new AccessDeniedException("Organization membership required"));
    }

    public OrganizationMember requireManage(UUID orgId, String sub) {
        if (isApiKeyForOrg(orgId, sub)) {
            return syntheticOwner(orgId, sub);
        }
        var member = requireMember(orgId, sub);
        if (member.getRole() != OrganizationMember.Role.OWNER) {
            throw new AccessDeniedException("Owner role required");
        }
        return member;
    }

    public void requireWrite(UUID orgId, String sub) {
        if (isApiKeyForOrg(orgId, sub)) {
            return;
        }
        var role = requireMember(orgId, sub).getRole();
        if (role == OrganizationMember.Role.VIEWER) {
            throw new AccessDeniedException("Write access required");
        }
    }

    public void requireDelete(UUID orgId, String sub) {
        if (isApiKeyForOrg(orgId, sub)) {
            return;
        }
        var role = requireMember(orgId, sub).getRole();
        if (role != OrganizationMember.Role.OWNER && role != OrganizationMember.Role.ADMIN) {
            throw new AccessDeniedException("Admin role required");
        }
    }

    private static boolean isApiKeyForOrg(UUID orgId, String sub) {
        return sub != null
                && sub.startsWith("apikey:")
                && ApiKeyRequestContext.organizationId().map(orgId::equals).orElse(false);
    }

    private static OrganizationMember syntheticOwner(UUID orgId, String sub) {
        return new OrganizationMember(
                UUID.nameUUIDFromBytes(sub.getBytes()),
                orgId,
                sub,
                null,
                "API Key",
                OrganizationMember.Role.OWNER,
                java.time.Instant.EPOCH
        );
    }
}
