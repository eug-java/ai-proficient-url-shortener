package com.example.shortener.integration;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgApiKeyRepository extends JpaRepository<OrgApiKey, UUID> {
    Optional<OrgApiKey> findByKeyHashAndRevokedAtIsNull(String keyHash);

    List<OrgApiKey> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
