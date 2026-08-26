package com.example.shortener.audit;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgAuditLogRepository extends JpaRepository<OrgAuditLog, UUID> {
    List<OrgAuditLog> findTop100ByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
