package com.example.shortener.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateUrlRequest(
        @NotBlank @Size(max = 2048) String originalUrl,
        @Size(max = 32) String customAlias,
        Instant expiresAt,
        @Size(max = 200) String title
) {
}
