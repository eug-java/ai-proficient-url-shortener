package com.example.shortener.application;

import java.time.Instant;

public record CreateUrlCommand(
        String originalUrl,
        String customAlias,
        Instant expiresAt
) {
}
