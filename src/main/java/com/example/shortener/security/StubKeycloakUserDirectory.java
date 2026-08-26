package com.example.shortener.security;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Deterministic local directory used when Keycloak Admin is disabled (tests / local without KC).
 */
@Component
@ConditionalOnProperty(name = "app.keycloak.admin.enabled", havingValue = "false", matchIfMissing = true)
public class StubKeycloakUserDirectory implements KeycloakUserDirectory {

    @Override
    public DirectoryUser findOrInviteByEmail(String email) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        String sub = UUID.nameUUIDFromBytes(("invite:" + normalized).getBytes(StandardCharsets.UTF_8)).toString();
        String local = normalized.contains("@") ? normalized.substring(0, normalized.indexOf('@')) : normalized;
        return new DirectoryUser(sub, normalized, local);
    }
}
