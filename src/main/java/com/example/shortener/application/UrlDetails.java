package com.example.shortener.application;

import java.time.Instant;
import java.util.UUID;

public record UrlDetails(
        UUID id,
        String shortCode,
        String shortUrl,
        String originalUrl,
        Instant expiresAt,
        Instant createdAt,
        long totalClicks,
        Instant lastAccessedAt
) {
}
