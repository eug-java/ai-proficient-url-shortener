package com.example.shortener.application;

import java.time.Instant;

public record AnalyticsView(
        String shortCode,
        long totalClicks,
        Instant lastAccessedAt,
        Instant expiresAt
) {
}
