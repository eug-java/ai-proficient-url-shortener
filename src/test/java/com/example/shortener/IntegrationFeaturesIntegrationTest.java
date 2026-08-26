package com.example.shortener;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.shortener.customdomain.OrgCustomDomain;
import com.example.shortener.customdomain.OrgCustomDomainRepository;
import com.example.shortener.integration.WebhookDispatcher;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationFeaturesIntegrationTest {

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
    OrgCustomDomainRepository domains;

    private HttpHeaders userHeaders(String sub) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Test-User-Sub", sub);
        return headers;
    }

    @Test
    void auditDomainWebhookAndApiKeyFlow() throws Exception {
        String slug = "int-" + UUID.randomUUID().toString().substring(0, 8);
        ResponseEntity<Map> org = rest.exchange(
                "/api/v1/orgs",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "Integrations", "slug", slug), userHeaders("owner-int")),
                Map.class
        );
        assertThat(org.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID orgId = UUID.fromString(org.getBody().get("id").toString());

        ResponseEntity<Map> domain = rest.exchange(
                "/api/v1/orgs/" + orgId + "/domains",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("hostname", "links." + slug + ".example"), userHeaders("owner-int")),
                Map.class
        );
        assertThat(domain.getStatusCode()).isEqualTo(HttpStatus.OK);
        UUID domainId = UUID.fromString(domain.getBody().get("id").toString());

        ResponseEntity<Map> verified = rest.exchange(
                "/api/v1/orgs/" + orgId + "/domains/" + domainId + "/verify",
                HttpMethod.POST,
                new HttpEntity<>(userHeaders("owner-int")),
                Map.class
        );
        assertThat(verified.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(verified.getBody().get("verified")).isEqualTo(true);

        ResponseEntity<Map> webhook = rest.exchange(
                "/api/v1/orgs/" + orgId + "/integrations/webhooks",
                HttpMethod.POST,
                new HttpEntity<>(
                        Map.of("targetUrl", "http://127.0.0.1:9/hook", "events", "LINK_CREATED,*"),
                        userHeaders("owner-int")
                ),
                Map.class
        );
        assertThat(webhook.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(webhook.getBody().get("secret")).asString().startsWith("whsec_");

        ResponseEntity<Map> apiKey = rest.exchange(
                "/api/v1/orgs/" + orgId + "/integrations/api-keys",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "ci-bot"), userHeaders("owner-int")),
                Map.class
        );
        assertThat(apiKey.getStatusCode()).isEqualTo(HttpStatus.OK);
        String rawKey = apiKey.getBody().get("apiKey").toString();
        assertThat(rawKey).startsWith("sk_live_");

        HttpHeaders keyHeaders = new HttpHeaders();
        keyHeaders.setContentType(MediaType.APPLICATION_JSON);
        keyHeaders.set("Authorization", "ApiKey " + rawKey);
        ResponseEntity<Map> link = rest.exchange(
                "/api/v1/orgs/" + orgId + "/urls",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("originalUrl", "https://example.com/via-key"), keyHeaders),
                Map.class
        );
        assertThat(link.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<List> audit = rest.exchange(
                "/api/v1/orgs/" + orgId + "/audit",
                HttpMethod.GET,
                new HttpEntity<>(userHeaders("owner-int")),
                List.class
        );
        assertThat(audit.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(audit.getBody()).isNotEmpty();

        assertThat(WebhookDispatcher.hmac("secret", "{\"a\":1}")).hasSize(64);
        assertThat(domains.findByHostnameIgnoreCase("links." + slug + ".example"))
                .map(OrgCustomDomain::isVerified)
                .contains(true);
    }
}
