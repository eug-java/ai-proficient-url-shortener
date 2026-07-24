package com.example.shortener.api;

import java.time.Instant;
import java.util.UUID;

public record UrlDetailsResponse(
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
