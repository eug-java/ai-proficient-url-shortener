package com.example.shortener.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redis;
    private final boolean enabled;
    private final long limit;
    private final Duration window;

    public RateLimitFilter(
            ObjectProvider<StringRedisTemplate> redis,
            @Value("${app.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.rate-limit.requests:60}") long limit,
            @Value("${app.rate-limit.window:PT1M}") Duration window
    ) {
        this.redis = redis.getIfAvailable();
        this.enabled = enabled && this.redis != null;
        this.limit = limit;
        this.window = window;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        String uri = request.getRequestURI();
        String method = request.getMethod();
        return !uri.startsWith("/api/")
                || "GET".equals(method)
                || "OPTIONS".equals(method)
                || "HEAD".equals(method);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {
        String identity = request.getUserPrincipal() == null
                ? request.getRemoteAddr()
                : request.getUserPrincipal().getName();
        String key = "rate:" + identity + ":" + (System.currentTimeMillis() / window.toMillis());
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, window);
        }
        if (count != null && count > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/problem+json");
            response.getWriter().write("{\"code\":\"RATE_LIMITED\",\"status\":429}");
            return;
        }
        chain.doFilter(request, response);
    }
}
