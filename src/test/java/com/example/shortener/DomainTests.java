package com.example.shortener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.shortener.domain.InvalidRequestException;
import com.example.shortener.domain.ShortCodeGenerator;
import com.example.shortener.domain.UrlMapping;
import com.example.shortener.domain.UrlPolicy;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DomainTests {

    private final UrlPolicy policy = new UrlPolicy();

    @Test
    void expirationBoundary() {
        Instant time = Instant.parse("2026-07-21T12:00:00Z");
        UrlMapping mapping = new UrlMapping(
                UUID.randomUUID(),
                "abc1234",
                "https://example.com",
                time,
                time.minusSeconds(1)
        );

        assertThat(mapping.isExpired(time.minusNanos(1))).isFalse();
        assertThat(mapping.isExpired(time)).isTrue();
    }

    @Test
    void generatorUsesBase62() {
        String code = new ShortCodeGenerator(7).generate();
        assertThat(code).matches("[A-Za-z0-9]{7}");
    }

    @Test
    void policyAllowsOnlyHttpAndHttps() {
        assertThat(policy.validateUrl("https://example.com/path"))
                .isEqualTo("https://example.com/path");

        assertThatThrownBy(() -> policy.validateUrl("file:///etc/passwd"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Only HTTP and HTTPS are supported");
    }

    @Test
    void policyRejectsReservedAlias() {
        assertThatThrownBy(() -> policy.validateAlias("actuator"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Custom alias is reserved");
    }

    @Test
    void policyRejectsInvalidAliasLengthAndCharacters() {
        assertThatThrownBy(() -> policy.validateAlias("ab"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Custom alias must contain 3 to 32 characters");

        assertThatThrownBy(() -> policy.validateAlias("bad/alias"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Custom alias may contain only letters, digits, underscores, and hyphens");
    }

    @Test
    void policyRejectsPrivateAndLoopbackHosts() {
        assertThatThrownBy(() -> policy.validateUrl("http://127.0.0.1/admin"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("URL host is not allowed");

        assertThatThrownBy(() -> policy.validateUrl("http://localhost/secret"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("URL host is not allowed");

        assertThatThrownBy(() -> policy.validateUrl("http://169.254.169.254/latest/meta-data"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("URL host is not allowed");

        assertThatThrownBy(() -> policy.validateUrl("http://10.0.0.5/internal"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("URL host is not allowed");
    }

    @Test
    void policyRejectsIpv6UniqueLocalAndCarrierGradeNatHosts() {
        assertThatThrownBy(() -> policy.validateUrl("http://[fc00::1]/"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("URL host is not allowed");

        assertThatThrownBy(() -> policy.validateUrl("http://[fd12:3456:789a::1]/"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("URL host is not allowed");

        assertThatThrownBy(() -> policy.validateUrl("http://100.64.0.1/internal"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("URL host is not allowed");
    }

    @Test
    void policyRejectsUnresolvedHosts() {
        assertThatThrownBy(() ->
                policy.validateUrl("http://no-such-host-for-shortener-tests.invalid/path")
        )
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("URL host could not be resolved");
    }

    @Test
    void policyRejectsUrlsWithEmbeddedCredentials() {
        assertThatThrownBy(() ->
                policy.validateUrl("https://user:secret@example.com/private")
        )
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("URL credentials are not supported");
    }
}
