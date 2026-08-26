package com.example.shortener.analytics;

import com.example.shortener.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orgs/{orgId}/urls/{shortCode}/analytics")
public class AnalyticsController {

    private final AnalyticsService service;
    private final CurrentUser users;
    private final Clock clock;

    public AnalyticsController(AnalyticsService service, CurrentUser users, Clock clock) {
        this.service = service;
        this.users = users;
        this.clock = clock;
    }

    private String sub(Authentication auth, HttpServletRequest request) {
        return users.require(auth, request).sub();
    }

    private LocalDate[] range(Integer days, LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
        LocalDate start = from != null
                ? from
                : end.minusDays(days == null || days < 1 ? 30 : days - 1L);
        return new LocalDate[] {start, end};
    }

    @GetMapping
    Map<String, Object> summary(
            @PathVariable UUID orgId,
            @PathVariable String shortCode,
            Authentication auth,
            HttpServletRequest request
    ) {
        return service.summary(orgId, sub(auth, request), shortCode);
    }

    @GetMapping("/summary")
    Map<String, Object> summaryAlias(
            @PathVariable UUID orgId,
            @PathVariable String shortCode,
            Authentication auth,
            HttpServletRequest request
    ) {
        return summary(orgId, shortCode, auth, request);
    }

    @GetMapping("/timeseries")
    List<Map<String, Object>> timeseries(
            @PathVariable UUID orgId,
            @PathVariable String shortCode,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            Authentication auth,
            HttpServletRequest request
    ) {
        LocalDate[] range = range(days, from, to);
        return service.timeseries(orgId, sub(auth, request), shortCode, range[0], range[1]);
    }

    @GetMapping("/breakdowns/{dimension}")
    List<Map<String, Object>> breakdown(
            @PathVariable UUID orgId,
            @PathVariable String shortCode,
            @PathVariable String dimension,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            Authentication auth,
            HttpServletRequest request
    ) {
        LocalDate[] range = range(days, from, to);
        return service.breakdown(orgId, sub(auth, request), shortCode, dimension, range[0], range[1]);
    }

    @GetMapping(value = "/export.csv", produces = "text/csv")
    ResponseEntity<String> csv(
            @PathVariable UUID orgId,
            @PathVariable String shortCode,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            Authentication auth,
            HttpServletRequest request
    ) {
        LocalDate[] range = range(days, from, to);
        String body = service.csv(orgId, sub(auth, request), shortCode, range[0], range[1]);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + shortCode + "-analytics.csv\"")
                .body(body);
    }
}
