package com.example.shortener.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GeoCountryResolverTest {

    @Test
    void prefersCountryHeaderOverMaxMind() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("CF-IPCountry")).thenReturn("fr");
        when(request.getHeader("X-Forwarded-For")).thenReturn("8.8.8.8");

        GeoCountryResolver resolver = new GeoCountryResolver(ip -> Optional.of("US"));
        assertThat(resolver.resolve(request)).contains("FR");
    }

    @Test
    void fallsBackToMaxMindUsingClientIp() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("CF-IPCountry")).thenReturn(null);
        when(request.getHeader("X-Country-Code")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn("8.8.8.8, 1.1.1.1");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        GeoCountryResolver resolver = new GeoCountryResolver(ip -> {
            assertThat(ip.getHostAddress()).isEqualTo("8.8.8.8");
            return Optional.of("US");
        });
        assertThat(resolver.resolve(request)).contains("US");
    }

    @Test
    void emptyWhenNoHeaderAndNoLookup() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("CF-IPCountry")).thenReturn(null);
        when(request.getHeader("X-Country-Code")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        GeoCountryResolver resolver = new GeoCountryResolver(ip -> Optional.empty());
        assertThat(resolver.resolve(request)).isEmpty();
    }
}
