package com.example.shortener;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PrometheusMetricsIntegrationTest {

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
    MeterRegistry meterRegistry;

    @Test
    void createPathShouldIncrementCreateMeterWithAliasTypeTag() {
        ResponseEntity<Map> created = restTemplate.postForEntity(
                "/api/v1/urls",
                Map.of(
                        "originalUrl", "https://example.com/prometheus-meter",
                        "customAlias", "prom-meter-test"
                ),
                Map.class
        );
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        assertThat(
                meterRegistry.get("shortener.urls.creations")
                        .tag("alias_type", "custom")
                        .counter()
                        .count()
        ).isEqualTo(1.0);
    }
}
