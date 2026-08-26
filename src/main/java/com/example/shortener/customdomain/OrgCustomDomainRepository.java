package com.example.shortener.customdomain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgCustomDomainRepository extends JpaRepository<OrgCustomDomain, UUID> {
    List<OrgCustomDomain> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Optional<OrgCustomDomain> findByHostnameIgnoreCase(String hostname);

    Optional<OrgCustomDomain> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
