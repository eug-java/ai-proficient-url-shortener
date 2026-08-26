package com.example.shortener;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        properties = {
                "app.security.enabled=true",
                "app.cache.enabled=false",
                "app.messaging.mode=inline",
                "app.rate-limit.enabled=false",
                "app.retention.enabled=false",
                "app.legacy-unscoped-api.enabled=false",
                "app.keycloak.admin.enabled=false"
        }
)
@AutoConfigureMockMvc
class JwtSecurityMockMvcTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.base-url", () -> "http://localhost");
        registry.add("app.security.jwt.issuer-uri", () -> "http://test/realms/shortener");
        registry.add("app.security.jwt.jwk-set-uri", () -> "http://test/realms/shortener/protocol/openid-connect/certs");
    }

    @TestConfiguration
    static class JwtDecoderConfig {
        @Bean
        @Primary
        JwtDecoder testJwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .subject("decoder-sub")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
        }
    }

    @Autowired
    MockMvc mockMvc;

    @Test
    void unauthenticatedApiIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/orgs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void jwtAllowsOrgCreateAndPublicRedirectStaysOpen() throws Exception {
        String slug = "jwt-" + UUID.randomUUID().toString().substring(0, 8);
        String body = """
                {"name":"JWT Org","slug":"%s"}
                """.formatted(slug);

        String orgJson = mockMvc.perform(post("/api/v1/orgs")
                        .with(jwt().jwt(j -> j.subject("jwt-owner").claim("email", "owner@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String orgId = orgJson.replaceAll("(?s).*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        String alias = "jw-" + UUID.randomUUID().toString().substring(0, 8);

        mockMvc.perform(post("/api/v1/orgs/" + orgId + "/urls")
                        .with(jwt().jwt(j -> j.subject("jwt-owner")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originalUrl":"https://example.com/jwt","customAlias":"%s"}
                                """.formatted(alias)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/" + alias))
                .andExpect(status().isFound());
    }
}
