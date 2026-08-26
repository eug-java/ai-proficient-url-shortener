package com.example.shortener.audit;

import com.example.shortener.integration.IntegrationEventPublisher;
import com.example.shortener.org.OrgAccessService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    public record AuditView(
            UUID id,
            String actorSub,
            String action,
            String entityType,
            String entityId,
            String details,
            java.time.Instant createdAt
    ) {}

    private final OrgAuditLogRepository repository;
    private final OrgAccessService access;
    private final IntegrationEventPublisher events;
    private final ObjectMapper json;
    private final Clock clock;

    public AuditService(
            OrgAuditLogRepository repository,
            OrgAccessService access,
            IntegrationEventPublisher events,
            ObjectMapper json,
            Clock clock
    ) {
        this.repository = repository;
        this.access = access;
        this.events = events;
        this.json = json;
        this.clock = clock;
    }

    @Transactional
    public void record(
            UUID orgId,
            String actorSub,
            String action,
            String entityType,
            String entityId,
            Map<String, ?> details
    ) {
        String payload;
        try {
            payload = details == null || details.isEmpty() ? null : json.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            payload = String.valueOf(details);
        }
        repository.save(new OrgAuditLog(
                UUID.randomUUID(),
                orgId,
                actorSub,
                action,
                entityType,
                entityId,
                payload,
                clock.instant()
        ));
        events.publish(orgId, action, entityType, entityId, details == null ? Map.of() : details);
    }

    @Transactional(readOnly = true)
    public List<AuditView> list(UUID orgId, String actorSub) {
        access.requireManage(orgId, actorSub);
        return repository.findTop100ByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(a -> new AuditView(
                        a.getId(),
                        a.getActorSub(),
                        a.getAction(),
                        a.getEntityType(),
                        a.getEntityId(),
                        a.getDetails(),
                        a.getCreatedAt()
                ))
                .toList();
    }
}
