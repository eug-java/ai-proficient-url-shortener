package com.example.shortener.api;

import java.time.Instant;

public record AnalyticsResponse(
        String shortCode,
        long totalClicks,
        Instant lastAccessedAt,
        Instant expiresAt
) {
}
