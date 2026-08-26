package com.example.shortener.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StubKeycloakUserDirectoryTest {

    @Test
    void isDeterministicForEmail() {
        StubKeycloakUserDirectory directory = new StubKeycloakUserDirectory();
        var a = directory.findOrInviteByEmail("Alice@Example.COM");
        var b = directory.findOrInviteByEmail("alice@example.com");
        assertThat(a.sub()).isEqualTo(b.sub());
        assertThat(a.email()).isEqualTo("alice@example.com");
        assertThat(a.displayName()).isEqualTo("alice");
    }
}
