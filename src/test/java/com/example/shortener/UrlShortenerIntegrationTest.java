package com.example.shortener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.shortener.domain.UrlMapping;
import com.example.shortener.persistence.UrlMappingRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class UrlShortenerIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.base-url", () -> "http://localhost");
    }

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UrlMappingRepository repository;

    @Test
    void redirectShouldUpdateAnalyticsWithoutFailingReadTransaction() throws Exception {
        String alias = "google-transaction-test";
        Map<String, Object> request = Map.of(
                "originalUrl", "https://www.google.com",
                "customAlias", alias
        );

        ResponseEntity<Map> created = restTemplate.postForEntity(
                "/api/v1/urls",
                request,
                Map.class
        );

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        mockMvc.perform(get("/" + alias))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://www.google.com"));

        ResponseEntity<Map> analytics = restTemplate.getForEntity(
                "/api/v1/urls/" + alias + "/analytics",
                Map.class
        );

        assertThat(analytics.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) analytics.getBody().get("totalClicks")).longValue())
                .isEqualTo(1L);
    }

    @Test
    void unknownShortCodeShouldReturnProblemDetail() throws Exception {
        mockMvc.perform(get("/missing-code"))
                .andExpect(status().isNotFound())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/problem+json"))
                .andExpect(jsonPath("$.code").value("SHORT_URL_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void duplicateCustomAliasShouldReturnConflict() throws Exception {
        String alias = "duplicate-alias-test";
        Map<String, Object> request = Map.of(
                "originalUrl", "https://example.com/first",
                "customAlias", alias
        );

        ResponseEntity<Map> first = restTemplate.postForEntity(
                "/api/v1/urls",
                request,
                Map.class
        );

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originalUrl": "https://example.com/second",
                                  "customAlias": "duplicate-alias-test"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/problem+json"))
                .andExpect(jsonPath("$.code").value("DUPLICATE_ALIAS"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void createWithoutAliasShouldGenerateRandomShortCode() {
        Map<String, Object> request = Map.of(
                "originalUrl", "https://example.com/random"
        );

        ResponseEntity<Map> created = restTemplate.postForEntity(
                "/api/v1/urls",
                request,
                Map.class
        );

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();

        String shortCode = (String) created.getBody().get("shortCode");
        String shortUrl = (String) created.getBody().get("shortUrl");

        assertThat(shortCode)
                .isNotBlank()
                .matches("[A-Za-z0-9]{7}");
        assertThat(shortUrl).isEqualTo("http://localhost/" + shortCode);
        assertThat(created.getHeaders().getLocation())
                .isEqualTo(java.net.URI.create("/api/v1/urls/" + shortCode));
    }

    @Test
    void createLocationShouldBeRetrievable() {
        String alias = "location-follow-test";
        ResponseEntity<Map> created = restTemplate.postForEntity(
                "/api/v1/urls",
                Map.of(
                        "originalUrl", "https://example.com/location",
                        "customAlias", alias
                ),
                Map.class
        );

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getHeaders().getLocation())
                .isEqualTo(java.net.URI.create("/api/v1/urls/" + alias));

        ResponseEntity<Map> details = restTemplate.getForEntity(
                created.getHeaders().getLocation(),
                Map.class
        );

        assertThat(details.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(details.getBody()).isNotNull();
        assertThat(details.getBody().get("shortCode")).isEqualTo(alias);
        assertThat(details.getBody().get("originalUrl"))
                .isEqualTo("https://example.com/location");
        assertThat(details.getBody().get("shortUrl"))
                .isEqualTo("http://localhost/" + alias);
    }

    @Test
    void privateDestinationHostShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originalUrl": "http://127.0.0.1/admin"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.detail").value("URL host is not allowed"));
    }

    @Test
    void responseShouldIncludeRequestIdHeader() throws Exception {
        mockMvc.perform(get("/actuator/health").header("X-Request-Id", "test-request-1"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "test-request-1"));
    }

    @Test
    void unsafeRequestIdShouldBeReplaced() throws Exception {
        mockMvc.perform(get("/actuator/health").header("X-Request-Id", "bad id with spaces"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String requestId = result.getResponse().getHeader("X-Request-Id");
                    assertThat(requestId)
                            .isNotBlank()
                            .isNotEqualTo("bad id with spaces")
                            .matches("[0-9a-fA-F-]{36}");
                });
    }


    @Test
    void reservedAliasShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originalUrl": "https://example.com",
                                  "customAlias": "actuator"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.detail").value("Custom alias is reserved"));
    }

    @Test
    void unsupportedUrlSchemeShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originalUrl": "file:///etc/passwd"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.detail").value("Only HTTP and HTTPS are supported"));
    }

    @Test
    void expirationInPastShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originalUrl": "https://example.com",
                                  "expiresAt": "2020-01-01T00:00:00Z"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.detail").value("Expiration must be in the future"));
    }

    @Test
    void expiredShortCodeShouldReturnGone() throws Exception {
        Instant now = Instant.now();
        repository.saveAndFlush(new UrlMapping(
                UUID.randomUUID(),
                "expired-link-test",
                "https://example.com/expired",
                now.minusSeconds(1),
                now.minusSeconds(60)
        ));

        mockMvc.perform(get("/expired-link-test"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("SHORT_URL_EXPIRED"))
                .andExpect(jsonPath("$.status").value(410));
    }

    @Test
    void healthAndOpenApiEndpointsShouldBeAvailable() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists());

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isNotFound());
    }

}
