package com.example.shortener.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;

class ShortenerMetricsTest {

    @Test
    void shouldRecordLowCardinalityBusinessMetricsWithStablePrometheusNames() {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        ShortenerMetrics metrics = new ShortenerMetrics(registry);

        metrics.urlCreated(AliasType.GENERATED);
        metrics.urlCreated(AliasType.CUSTOM);
        metrics.aliasCollision(AliasType.CUSTOM);
        metrics.redirect(RedirectResult.NOT_FOUND);
        metrics.redirectDuration(RedirectResult.NOT_FOUND, 1_000_000L);
        metrics.analyticsUpdate(AnalyticsResult.FAILURE);
        metrics.error("DUPLICATE_ALIAS");

        String scrape = registry.scrape();

        assertThat(scrape)
                .contains("shortener_urls_creations_total")
                .contains("alias_type=\"generated\"")
                .contains("alias_type=\"custom\"")
                .contains("shortener_alias_collisions_total")
                .contains("shortener_redirect_total")
                .contains("result=\"not_found\"")
                .contains("shortener_redirect_duration_seconds")
                .contains("shortener_analytics_updates_total")
                .contains("shortener_errors_total")
                .contains("code=\"DUPLICATE_ALIAS\"")
                .doesNotContain("shortener_urls_created_total");

        assertThat(
                registry.get("shortener.urls.creations")
                        .tag("alias_type", "generated")
                        .counter()
                        .count()
        ).isEqualTo(1.0);
    }
}
