package com.example.shortener.geo;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Resolves ISO-3166-1 alpha-2 country for analytics.
 * Prefer trusted edge headers; fall back to optional MaxMind Country DB.
 */
@Component
public class GeoCountryResolver {

    private final MaxMindCountryLookup maxMindLookup;

    public GeoCountryResolver(MaxMindCountryLookup maxMindLookup) {
        this.maxMindLookup = maxMindLookup == null ? address -> Optional.empty() : maxMindLookup;
    }

    public Optional<String> resolve(HttpServletRequest request) {
        String header = request.getHeader("CF-IPCountry");
        if (header == null || header.isBlank()) {
            header = request.getHeader("X-Country-Code");
        }
        if (header != null && header.matches("(?i)[A-Z]{2}")) {
            return Optional.of(header.toUpperCase());
        }

        return ClientIpResolver.resolve(request)
                .flatMap(ip -> {
                    try {
                        return maxMindLookup.countryIso(InetAddress.getByName(ip));
                    } catch (Exception ignored) {
                        return Optional.empty();
                    }
                });
    }
}
