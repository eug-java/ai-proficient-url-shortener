package com.example.shortener.org;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class OrgAccessServiceTest {

    @Mock
    OrganizationMemberRepository members;

    @InjectMocks
    OrgAccessService access;

    @Test
    void viewerCannotWrite() {
        UUID orgId = UUID.randomUUID();
        when(members.findByOrganizationIdAndUserSub(orgId, "v"))
                .thenReturn(Optional.of(member(orgId, "v", OrganizationMember.Role.VIEWER)));
        assertThatThrownBy(() -> access.requireWrite(orgId, "v"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void memberCannotDelete() {
        UUID orgId = UUID.randomUUID();
        when(members.findByOrganizationIdAndUserSub(orgId, "m"))
                .thenReturn(Optional.of(member(orgId, "m", OrganizationMember.Role.MEMBER)));
        assertThatThrownBy(() -> access.requireDelete(orgId, "m"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void adminCanDelete() {
        UUID orgId = UUID.randomUUID();
        when(members.findByOrganizationIdAndUserSub(orgId, "a"))
                .thenReturn(Optional.of(member(orgId, "a", OrganizationMember.Role.ADMIN)));
        access.requireDelete(orgId, "a");
    }

    private static OrganizationMember member(UUID orgId, String sub, OrganizationMember.Role role) {
        return new OrganizationMember(UUID.randomUUID(), orgId, sub, null, null, role, java.time.Instant.EPOCH);
    }
}
