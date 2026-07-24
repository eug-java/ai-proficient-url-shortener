package com.example.shortener.observability;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ShortenerMetrics {

    private final MeterRegistry registry;

    public ShortenerMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void urlCreated(AliasType aliasType) {
        registry.counter(
                "shortener.urls.creations",
                "alias_type", tag(aliasType)
        ).increment();
    }

    public void aliasCollision(AliasType aliasType) {
        registry.counter(
                "shortener.alias.collisions.total",
                "alias_type", tag(aliasType)
        ).increment();
    }

    public void redirect(RedirectResult result) {
        registry.counter(
                "shortener.redirect.total",
                "result", tag(result)
        ).increment();
    }

    public void redirectDuration(RedirectResult result, long nanos) {
        registry.timer(
                "shortener.redirect.duration",
                "result", tag(result)
        ).record(Duration.ofNanos(nanos));
    }

    public void analyticsUpdate(AnalyticsResult result) {
        registry.counter(
                "shortener.analytics.updates.total",
                "result", tag(result)
        ).increment();
    }

    public void error(String code) {
        registry.counter(
                "shortener.errors.total",
                "code", code
        ).increment();
    }

    private String tag(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }
}
