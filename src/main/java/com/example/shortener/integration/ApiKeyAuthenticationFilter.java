package com.example.shortener.integration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Accepts {@code Authorization: ApiKey sk_live_...} for machine integrations.
 * Registered only via SecurityConfig so SecurityContextHolderFilter cannot wipe auth.
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final IntegrationService integration;

    public ApiKeyAuthenticationFilter(IntegrationService integration) {
        this.integration = integration;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.regionMatches(true, 0, "ApiKey ", 0, 7)) {
                String raw = header.substring(7).trim();
                integration.resolveApiKey(raw).ifPresent(resolved -> {
                    ApiKeyRequestContext.setOrganizationId(resolved.organizationId());
                    var auth = new UsernamePasswordAuthenticationToken(
                            resolved.actorSub(),
                            "api-key",
                            List.of(new SimpleGrantedAuthority("ROLE_API_KEY"))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
            }
            chain.doFilter(request, response);
        } finally {
            ApiKeyRequestContext.clear();
        }
    }
}
