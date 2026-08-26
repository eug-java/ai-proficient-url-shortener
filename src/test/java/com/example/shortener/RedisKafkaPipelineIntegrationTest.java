package com.example.shortener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.shortener.messaging.OutboxPublisher;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@EmbeddedKafka(
        partitions = 1,
        topics = "shortener.clicks.v1",
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=",
                "app.cache.enabled=true",
                "app.messaging.mode=kafka",
                "app.rate-limit.enabled=true",
                "app.rate-limit.requests=5",
                "app.rate-limit.window=PT1M",
                "app.kafka.outbox-poll-ms=250",
                "app.kafka.clicks-topic=shortener.clicks.v1",
                "spring.kafka.listener.auto-startup=true",
                "spring.kafka.consumer.auto-offset-reset=earliest",
                "app.legacy-unscoped-api.enabled=true",
                "app.security.enabled=false",
                "app.retention.enabled=false"
        }
)
@AutoConfigureMockMvc
class RedisKafkaPipelineIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379).toString());
        registry.add("app.base-url", () -> "http://localhost");
        registry.add("spring.kafka.consumer.group-id", () -> "pipeline-it-" + UUID.randomUUID());
    }

    @Autowired
    TestRestTemplate rest;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    StringRedisTemplate redis;

    @Autowired
    OutboxPublisher outboxPublisher;

    private HttpHeaders userHeaders(String sub) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Test-User-Sub", sub);
        return headers;
    }

    @Test
    void redisCachesRedirectAndKafkaDeliversAnalytics() throws Exception {
        String slug = "pipe-" + UUID.randomUUID().toString().substring(0, 8);
        ResponseEntity<Map> org = rest.exchange(
                "/api/v1/orgs",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "Pipe", "slug", slug), userHeaders("owner-pipe")),
                Map.class
        );
        assertThat(org.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID orgId = UUID.fromString(org.getBody().get("id").toString());
        String alias = "rk-" + UUID.randomUUID().toString().substring(0, 8);

        ResponseEntity<Map> created = rest.exchange(
                "/api/v1/orgs/" + orgId + "/urls",
                HttpMethod.POST,
                new HttpEntity<>(
                        Map.of(
                                "originalUrl", "https://example.com/pipeline",
                                "customAlias", alias
                        ),
                        userHeaders("owner-pipe")
                ),
                Map.class
        );
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        mockMvc.perform(get("/" + alias).header("X-Country-Code", "DE"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://example.com/pipeline"));

        assertThat(redis.opsForValue().get("redirect:" + alias)).isNotBlank();

        mockMvc.perform(get("/" + alias)).andExpect(status().isFound());

        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(400)).untilAsserted(() -> {
            outboxPublisher.publish();
            ResponseEntity<Map> summary = rest.exchange(
                    "/api/v1/orgs/" + orgId + "/urls/" + alias + "/analytics",
                    HttpMethod.GET,
                    new HttpEntity<>(userHeaders("owner-pipe")),
                    Map.class
            );
            assertThat(summary.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(((Number) summary.getBody().get("totalClicks")).longValue()).isEqualTo(2L);
        });
    }

    @Test
    void rateLimitReturns429AfterBurst() {
        int limited = 0;
        for (int i = 0; i < 12; i++) {
            ResponseEntity<Map> response = rest.exchange(
                    "/api/v1/orgs",
                    HttpMethod.POST,
                    new HttpEntity<>(
                            Map.of("name", "RL " + i, "slug", "rl-" + UUID.randomUUID().toString().substring(0, 8)),
                            userHeaders("rate-user")
                    ),
                    Map.class
            );
            if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                limited++;
            }
        }
        assertThat(limited).isGreaterThan(0);
    }
}
