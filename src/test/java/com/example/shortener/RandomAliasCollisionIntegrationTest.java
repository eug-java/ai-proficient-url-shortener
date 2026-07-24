package com.example.shortener;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.shortener.domain.AliasGenerator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
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
@Import(RandomAliasCollisionIntegrationTest.CollisionGeneratorConfiguration.class)
class RandomAliasCollisionIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.base-url", () -> "http://localhost");
        registry.add("app.alias-generation-attempts", () -> 3);
    }

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void generatedAliasCollisionShouldRetryInANewTransaction() {
        ResponseEntity<Map> occupied = restTemplate.postForEntity(
                "/api/v1/urls",
                Map.of(
                        "originalUrl", "https://example.com/occupied",
                        "customAlias", "COLLIDE"
                ),
                Map.class
        );
        assertThat(occupied.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<Map> generated = restTemplate.postForEntity(
                "/api/v1/urls",
                Map.of("originalUrl", "https://example.com/generated"),
                Map.class
        );

        assertThat(generated.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(generated.getBody()).isNotNull();
        assertThat(generated.getBody().get("shortCode")).isEqualTo("UNIQUE1");
        assertThat(generated.getBody().get("shortUrl"))
                .isEqualTo("http://localhost/UNIQUE1");
    }

    @TestConfiguration
    static class CollisionGeneratorConfiguration {

        @Bean
        @Primary
        AliasGenerator deterministicAliasGenerator() {
            AtomicInteger invocation = new AtomicInteger();
            return () -> invocation.getAndIncrement() == 0 ? "COLLIDE" : "UNIQUE1";
        }
    }
}
