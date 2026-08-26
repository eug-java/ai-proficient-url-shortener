package com.example.shortener.integration;

import java.util.Optional;
import java.util.UUID;

public final class ApiKeyRequestContext {

    private static final ThreadLocal<UUID> ORG = new ThreadLocal<>();

    private ApiKeyRequestContext() {}

    public static void setOrganizationId(UUID organizationId) {
        ORG.set(organizationId);
    }

    public static Optional<UUID> organizationId() {
        return Optional.ofNullable(ORG.get());
    }

    public static void clear() {
        ORG.remove();
    }
}
