package com.example.shortener.geo;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Geo resolution for analytics. Prefer edge headers; optional MaxMind path reserved for later.
 */
@Component
public class GeoCountryResolver {

    private final String maxmindPath;

    public GeoCountryResolver(@Value("${app.geo.maxmind-db:}") String maxmindPath) {
        this.maxmindPath = maxmindPath == null ? "" : maxmindPath.trim();
    }

    public Optional<String> resolve(HttpServletRequest request) {
        String header = request.getHeader("CF-IPCountry");
        if (header == null || header.isBlank()) {
            header = request.getHeader("X-Country-Code");
        }
        if (header != null && header.matches("(?i)[A-Z]{2}")) {
            return Optional.of(header.toUpperCase());
        }
        // MaxMind file integration can be wired when app.geo.maxmind-db is set.
        if (!maxmindPath.isBlank()) {
            return Optional.empty();
        }
        return Optional.empty();
    }
}
