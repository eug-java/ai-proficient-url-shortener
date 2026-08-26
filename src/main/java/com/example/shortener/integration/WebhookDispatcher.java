package com.example.shortener.integration;

import com.example.shortener.messaging.OutboxEvent;
import com.example.shortener.messaging.OutboxEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Delivers Integration.Event outbox rows to org webhook endpoints with HMAC signatures.
 */
@Service
public class WebhookDispatcher {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatcher.class);

    private final OutboxEventRepository outbox;
    private final OrgWebhookEndpointRepository endpoints;
    private final ObjectMapper json;
    private final Clock clock;
    private final boolean enabled;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    public WebhookDispatcher(
            OutboxEventRepository outbox,
            OrgWebhookEndpointRepository endpoints,
            ObjectMapper json,
            Clock clock,
            @Value("${app.webhooks.dispatch-enabled:true}") boolean enabled
    ) {
        this.outbox = outbox;
        this.endpoints = endpoints;
        this.json = json;
        this.clock = clock;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${app.webhooks.poll-ms:2000}")
    @Transactional
    public void dispatch() {
        if (!enabled) {
            return;
        }
        List<OutboxEvent> pending = outbox.findTop100ByEventTypeAndPublishedAtIsNullOrderByCreatedAt(
                IntegrationEventPublisher.EVENT_TYPE
        );
        for (OutboxEvent event : pending) {
            try {
                JsonNode node = json.readTree(event.getPayload());
                UUID orgId = UUID.fromString(node.path("organizationId").asText());
                String action = node.path("action").asText();
                String body = event.getPayload();
                for (OrgWebhookEndpoint endpoint : endpoints.findAllByOrganizationIdAndEnabledTrue(orgId)) {
                    if (!endpoint.accepts(action)) {
                        continue;
                    }
                    deliver(endpoint, body, event.getEventId());
                }
                event.published(clock.instant());
            } catch (Exception ex) {
                log.warn("Webhook dispatch failed for outboxId={}", event.getId(), ex);
            }
        }
    }

    private void deliver(OrgWebhookEndpoint endpoint, String body, UUID eventId) throws Exception {
        String signature = hmac(endpoint.getSecret(), body);
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint.getTargetUrl()))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .header("X-Shortener-Event-Id", eventId.toString())
                .header("X-Shortener-Signature", "sha256=" + signature)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IllegalStateException("Webhook HTTP " + response.statusCode());
        }
    }

    public static String hmac(String secret, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }
}
