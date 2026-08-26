package com.example.shortener.org;

import com.example.shortener.domain.NotFoundException;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class OrgAccessService {
    private final OrganizationMemberRepository members;
    public OrgAccessService(OrganizationMemberRepository members) { this.members = members; }

    public OrganizationMember requireMember(UUID orgId, String sub) {
        return members.findByOrganizationIdAndUserSub(orgId, sub)
                .orElseThrow(() -> new AccessDeniedException("Organization membership required"));
    }
    public OrganizationMember requireManage(UUID orgId, String sub) {
        var member = requireMember(orgId, sub);
        if (member.getRole() != OrganizationMember.Role.OWNER) {
            throw new AccessDeniedException("Owner role required");
        }
        return member;
    }
    public void requireWrite(UUID orgId, String sub) {
        var role = requireMember(orgId, sub).getRole();
        if (role == OrganizationMember.Role.VIEWER) throw new AccessDeniedException("Write access required");
    }
    public void requireDelete(UUID orgId, String sub) {
        var role = requireMember(orgId, sub).getRole();
        if (role != OrganizationMember.Role.OWNER && role != OrganizationMember.Role.ADMIN)
            throw new AccessDeniedException("Admin role required");
    }
}
