package com.example.shortener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
class OrgProductIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.base-url", () -> "http://localhost");
    }

    @Autowired
    TestRestTemplate rest;

    @Autowired
    MockMvc mockMvc;

    private HttpHeaders userHeaders(String sub) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Test-User-Sub", sub);
        return headers;
    }

    private UUID createOrg(String slug) {
        ResponseEntity<Map> created = rest.exchange(
                "/api/v1/orgs",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "Acme " + slug, "slug", slug), userHeaders("owner-1")),
                Map.class
        );
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().get("role")).isEqualTo("OWNER");
        return UUID.fromString(created.getBody().get("id").toString());
    }

    @Test
    void orgScopedCreateRedirectAndAnalyticsPipeline() throws Exception {
        UUID orgId = createOrg("acme-" + UUID.randomUUID().toString().substring(0, 8));
        String alias = "prod-" + UUID.randomUUID().toString().substring(0, 8);

        ResponseEntity<Map> created = rest.exchange(
                "/api/v1/orgs/" + orgId + "/urls",
                HttpMethod.POST,
                new HttpEntity<>(
                        Map.of(
                                "originalUrl", "https://example.com/product",
                                "customAlias", alias,
                                "title", "Product link"
                        ),
                        userHeaders("owner-1")
                ),
                Map.class
        );
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        mockMvc.perform(get("/" + alias)
                        .header("User-Agent", "Mozilla/5.0")
                        .header("Referer", "https://news.example/article")
                        .header("X-Country-Code", "US"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://example.com/product"));

        ResponseEntity<Map> summary = rest.exchange(
                "/api/v1/orgs/" + orgId + "/urls/" + alias + "/analytics",
                HttpMethod.GET,
                new HttpEntity<>(userHeaders("owner-1")),
                Map.class
        );
        assertThat(summary.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) summary.getBody().get("totalClicks")).longValue()).isEqualTo(1L);

        ResponseEntity<List> series = rest.exchange(
                "/api/v1/orgs/" + orgId + "/urls/" + alias + "/analytics/timeseries?days=7",
                HttpMethod.GET,
                new HttpEntity<>(userHeaders("owner-1")),
                List.class
        );
        assertThat(series.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(series.getBody()).isNotEmpty();

        ResponseEntity<List> links = rest.exchange(
                "/api/v1/orgs/" + orgId + "/urls",
                HttpMethod.GET,
                new HttpEntity<>(userHeaders("owner-1")),
                List.class
        );
        assertThat(links.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(links.getBody()).isNotEmpty();

        ResponseEntity<List> referrers = rest.exchange(
                "/api/v1/orgs/" + orgId + "/urls/" + alias + "/analytics/breakdowns/referrer?days=7",
                HttpMethod.GET,
                new HttpEntity<>(userHeaders("owner-1")),
                List.class
        );
        assertThat(referrers.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Void> disabled = rest.exchange(
                "/api/v1/orgs/" + orgId + "/urls/" + alias + "/disable",
                HttpMethod.POST,
                new HttpEntity<>(userHeaders("owner-1")),
                Void.class
        );
        assertThat(disabled.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        mockMvc.perform(get("/" + alias)).andExpect(status().isNotFound());
    }

    @Test
    void viewerCannotCreateLinks() {
        UUID orgId = createOrg("view-" + UUID.randomUUID().toString().substring(0, 8));

        rest.exchange(
                "/api/v1/orgs/" + orgId + "/members",
                HttpMethod.POST,
                new HttpEntity<>(
                        Map.of("userSub", "viewer-1", "role", "VIEWER"),
                        userHeaders("owner-1")
                ),
                Map.class
        );

        ResponseEntity<Map> denied = rest.exchange(
                "/api/v1/orgs/" + orgId + "/urls",
                HttpMethod.POST,
                new HttpEntity<>(
                        Map.of("originalUrl", "https://example.com/denied"),
                        userHeaders("viewer-1")
                ),
                Map.class
        );
        assertThat(denied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void nonMemberCannotReadOrgAnalytics() throws Exception {
        UUID orgId = createOrg("sec-" + UUID.randomUUID().toString().substring(0, 8));
        String alias = "sec-" + UUID.randomUUID().toString().substring(0, 8);

        rest.exchange(
                "/api/v1/orgs/" + orgId + "/urls",
                HttpMethod.POST,
                new HttpEntity<>(
                        Map.of("originalUrl", "https://example.com/a", "customAlias", alias),
                        userHeaders("owner-1")
                ),
                Map.class
        );

        mockMvc.perform(get("/api/v1/orgs/" + orgId + "/urls/" + alias + "/analytics")
                        .header("X-Test-User-Sub", "stranger"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }
}
