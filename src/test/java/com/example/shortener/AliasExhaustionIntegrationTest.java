package com.example.shortener;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.shortener.domain.AliasGenerator;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(AliasExhaustionIntegrationTest.FixedAliasConfiguration.class)
class AliasExhaustionIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.base-url", () -> "http://localhost");
        registry.add("app.alias-generation-attempts", () -> 2);
    }

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void exhaustedGeneratedAliasRetriesShouldReturnInternalError() {
        ResponseEntity<Map> occupied = restTemplate.postForEntity(
                "/api/v1/urls",
                Map.of(
                        "originalUrl", "https://example.com/occupied-exhaust",
                        "customAlias", "EXHAUST1"
                ),
                Map.class
        );
        assertThat(occupied.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<Map> exhausted = restTemplate.postForEntity(
                "/api/v1/urls",
                Map.of("originalUrl", "https://example.com/exhaust"),
                Map.class
        );

        assertThat(exhausted.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(exhausted.getBody()).isNotNull();
        assertThat(exhausted.getBody().get("code")).isEqualTo("INTERNAL_ERROR");
    }

    @TestConfiguration
    static class FixedAliasConfiguration {

        @Bean
        @Primary
        AliasGenerator alwaysCollidingAliasGenerator() {
            return () -> "EXHAUST1";
        }
    }
}
