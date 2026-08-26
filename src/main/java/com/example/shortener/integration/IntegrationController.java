package com.example.shortener.integration;

import com.example.shortener.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orgs/{orgId}/integrations")
public class IntegrationController {

    public record CreateWebhook(@NotBlank String targetUrl, String events) {}

    public record CreateApiKey(@NotBlank String name) {}

    private final IntegrationService integration;
    private final CurrentUser currentUser;

    public IntegrationController(IntegrationService integration, CurrentUser currentUser) {
        this.integration = integration;
        this.currentUser = currentUser;
    }

    @GetMapping("/webhooks")
    List<IntegrationService.WebhookView> webhooks(
            @PathVariable UUID orgId,
            Authentication auth,
            HttpServletRequest request
    ) {
        return integration.listWebhooks(orgId, currentUser.require(auth, request).sub());
    }

    @PostMapping("/webhooks")
    IntegrationService.CreatedWebhook createWebhook(
            @PathVariable UUID orgId,
            @Valid @RequestBody CreateWebhook body,
            Authentication auth,
            HttpServletRequest request
    ) {
        return integration.createWebhook(
                orgId,
                currentUser.require(auth, request).sub(),
                body.targetUrl(),
                body.events()
        );
    }

    @DeleteMapping("/webhooks/{webhookId}")
    void disableWebhook(
            @PathVariable UUID orgId,
            @PathVariable UUID webhookId,
            Authentication auth,
            HttpServletRequest request
    ) {
        integration.disableWebhook(orgId, currentUser.require(auth, request).sub(), webhookId);
    }

    @GetMapping("/api-keys")
    List<IntegrationService.ApiKeyView> apiKeys(
            @PathVariable UUID orgId,
            Authentication auth,
            HttpServletRequest request
    ) {
        return integration.listApiKeys(orgId, currentUser.require(auth, request).sub());
    }

    @PostMapping("/api-keys")
    IntegrationService.CreatedApiKey createApiKey(
            @PathVariable UUID orgId,
            @Valid @RequestBody CreateApiKey body,
            Authentication auth,
            HttpServletRequest request
    ) {
        return integration.createApiKey(orgId, currentUser.require(auth, request).sub(), body.name());
    }

    @DeleteMapping("/api-keys/{keyId}")
    void revokeApiKey(
            @PathVariable UUID orgId,
            @PathVariable UUID keyId,
            Authentication auth,
            HttpServletRequest request
    ) {
        integration.revokeApiKey(orgId, currentUser.require(auth, request).sub(), keyId);
    }
}
