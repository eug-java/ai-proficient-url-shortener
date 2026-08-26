package com.example.shortener.integration;

import com.example.shortener.messaging.OutboxEvent;
import com.example.shortener.messaging.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enqueues cross-service integration events onto the shared outbox (eventType Integration.*).
 */
@Service
public class IntegrationEventPublisher {

    public static final String EVENT_TYPE = "Integration.Event";

    private final OutboxEventRepository outbox;
    private final ObjectMapper json;
    private final Clock clock;

    public IntegrationEventPublisher(OutboxEventRepository outbox, ObjectMapper json, Clock clock) {
        this.outbox = outbox;
        this.json = json;
        this.clock = clock;
    }

    @Transactional
    public void publish(
            UUID orgId,
            String action,
            String entityType,
            String entityId,
            Map<String, ?> details
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("organizationId", orgId.toString());
        payload.put("action", action);
        payload.put("entityType", entityType);
        payload.put("entityId", entityId);
        payload.put("occurredAt", clock.instant().toString());
        payload.put("details", details == null ? Map.of() : details);
        try {
            outbox.save(new OutboxEvent(
                    UUID.randomUUID(),
                    "Organization",
                    orgId.toString(),
                    EVENT_TYPE,
                    UUID.randomUUID(),
                    json.writeValueAsString(payload),
                    clock.instant()
            ));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize integration event", e);
        }
    }
}
