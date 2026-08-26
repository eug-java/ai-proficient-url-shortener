package com.example.shortener.integration;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgWebhookEndpointRepository extends JpaRepository<OrgWebhookEndpoint, UUID> {
    List<OrgWebhookEndpoint> findAllByOrganizationIdAndEnabledTrue(UUID organizationId);

    List<OrgWebhookEndpoint> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
