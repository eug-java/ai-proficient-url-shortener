package com.example.shortener.integration;

import com.example.shortener.audit.AuditService;
import com.example.shortener.domain.InvalidRequestException;
import com.example.shortener.org.OrgAccessService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntegrationService {

    public record WebhookView(UUID id, String targetUrl, String events, boolean enabled, java.time.Instant createdAt) {}

    public record CreatedWebhook(WebhookView view, String secret) {}

    public record ApiKeyView(UUID id, String name, String keyPrefix, java.time.Instant createdAt, java.time.Instant lastUsedAt) {}

    public record CreatedApiKey(ApiKeyView view, String apiKey) {}

    public record ResolvedApiKey(UUID organizationId, String actorSub) {}

    private final OrgWebhookEndpointRepository webhooks;
    private final OrgApiKeyRepository apiKeys;
    private final OrgAccessService access;
    private final AuditService audit;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public IntegrationService(
            OrgWebhookEndpointRepository webhooks,
            OrgApiKeyRepository apiKeys,
            OrgAccessService access,
            AuditService audit,
            Clock clock
    ) {
        this.webhooks = webhooks;
        this.apiKeys = apiKeys;
        this.access = access;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public CreatedWebhook createWebhook(UUID orgId, String actor, String targetUrl, String events) {
        access.requireManage(orgId, actor);
        validateUrl(targetUrl);
        String secret = "whsec_" + HexFormat.of().formatHex(random.generateSeed(24));
        String normalizedEvents = (events == null || events.isBlank()) ? "*" : events.trim();
        OrgWebhookEndpoint saved = webhooks.save(new OrgWebhookEndpoint(
                UUID.randomUUID(),
                orgId,
                targetUrl.trim(),
                secret,
                normalizedEvents,
                clock.instant()
        ));
        audit.record(orgId, actor, "WEBHOOK_CREATED", "Webhook", saved.getId().toString(),
                Map.of("targetUrl", targetUrl, "events", normalizedEvents));
        return new CreatedWebhook(toWebhookView(saved), secret);
    }

    @Transactional(readOnly = true)
    public List<WebhookView> listWebhooks(UUID orgId, String actor) {
        access.requireManage(orgId, actor);
        return webhooks.findAllByOrganizationIdOrderByCreatedAtDesc(orgId).stream().map(this::toWebhookView).toList();
    }

    @Transactional
    public void disableWebhook(UUID orgId, String actor, UUID webhookId) {
        access.requireManage(orgId, actor);
        OrgWebhookEndpoint endpoint = webhooks.findById(webhookId)
                .filter(w -> w.getOrganizationId().equals(orgId))
                .orElseThrow(() -> new InvalidRequestException("Webhook not found"));
        endpoint.disable();
        audit.record(orgId, actor, "WEBHOOK_DISABLED", "Webhook", webhookId.toString(), Map.of());
    }

    @Transactional
    public CreatedApiKey createApiKey(UUID orgId, String actor, String name) {
        access.requireManage(orgId, actor);
        if (name == null || name.isBlank()) {
            throw new InvalidRequestException("API key name is required");
        }
        byte[] raw = random.generateSeed(24);
        String secret = "sk_live_" + HexFormat.of().formatHex(raw);
        String prefix = secret.substring(0, 12);
        OrgApiKey saved = apiKeys.save(new OrgApiKey(
                UUID.randomUUID(),
                orgId,
                name.trim(),
                prefix,
                sha256(secret),
                actor,
                clock.instant()
        ));
        audit.record(orgId, actor, "API_KEY_CREATED", "ApiKey", saved.getId().toString(),
                Map.of("name", name.trim(), "prefix", prefix));
        return new CreatedApiKey(toApiKeyView(saved), secret);
    }

    @Transactional(readOnly = true)
    public List<ApiKeyView> listApiKeys(UUID orgId, String actor) {
        access.requireManage(orgId, actor);
        return apiKeys.findAllByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .filter(OrgApiKey::isActive)
                .map(this::toApiKeyView)
                .toList();
    }

    @Transactional
    public void revokeApiKey(UUID orgId, String actor, UUID keyId) {
        access.requireManage(orgId, actor);
        OrgApiKey key = apiKeys.findById(keyId)
                .filter(k -> k.getOrganizationId().equals(orgId))
                .orElseThrow(() -> new InvalidRequestException("API key not found"));
        key.revoke(clock.instant());
        audit.record(orgId, actor, "API_KEY_REVOKED", "ApiKey", keyId.toString(), Map.of());
    }

    @Transactional
    public Optional<ResolvedApiKey> resolveApiKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank() || !rawKey.startsWith("sk_live_")) {
            return Optional.empty();
        }
        return apiKeys.findByKeyHashAndRevokedAtIsNull(sha256(rawKey.trim())).map(key -> {
            key.touch(clock.instant());
            return new ResolvedApiKey(key.getOrganizationId(), "apikey:" + key.getId());
        });
    }

    private WebhookView toWebhookView(OrgWebhookEndpoint e) {
        return new WebhookView(e.getId(), e.getTargetUrl(), e.getEvents(), e.isEnabled(), e.getCreatedAt());
    }

    private ApiKeyView toApiKeyView(OrgApiKey k) {
        return new ApiKeyView(k.getId(), k.getName(), k.getKeyPrefix(), k.getCreatedAt(), k.getLastUsedAt());
    }

    private static void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new InvalidRequestException("Webhook URL is required");
        }
        String lower = url.trim().toLowerCase(Locale.ROOT);
        if (!lower.startsWith("https://") && !lower.startsWith("http://localhost") && !lower.startsWith("http://127.0.0.1")) {
            throw new InvalidRequestException("Webhook URL must be https (or localhost for development)");
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
