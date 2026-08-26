package com.example.shortener.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
    public record User(String sub, String email, String name) {}

    private final boolean securityEnabled;

    public CurrentUser(@Value("${app.security.enabled:true}") boolean securityEnabled) {
        this.securityEnabled = securityEnabled;
    }

    public User require(Authentication authentication, HttpServletRequest request) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return new User(jwt.getSubject(), jwt.getClaimAsString("email"), jwt.getClaimAsString("name"));
        }
        if (authentication != null && authentication.getPrincipal() instanceof String principal
                && principal.startsWith("apikey:")) {
            return new User(principal, null, "API Key");
        }
        if (!securityEnabled) {
            String sub = request.getHeader("X-Test-User-Sub");
            return new User(sub == null || sub.isBlank() ? "test-user" : sub, null, null);
        }
        throw new org.springframework.security.access.AccessDeniedException("Authentication required");
    }
}
