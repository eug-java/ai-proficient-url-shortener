package com.example.shortener.org;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {
    Optional<OrganizationMember> findByOrganizationIdAndUserSub(UUID organizationId, String userSub);
    List<OrganizationMember> findAllByUserSub(String userSub);
    List<OrganizationMember> findAllByOrganizationId(UUID organizationId);
}
