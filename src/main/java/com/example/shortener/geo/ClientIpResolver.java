package com.example.shortener.geo;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

final class ClientIpResolver {

    private ClientIpResolver() {}

    static Optional<String> resolve(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String first = forwarded.split(",")[0].trim();
            if (!first.isBlank()) {
                return Optional.of(first);
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return Optional.of(realIp.trim());
        }
        String remote = request.getRemoteAddr();
        if (remote == null || remote.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(remote);
    }
}
