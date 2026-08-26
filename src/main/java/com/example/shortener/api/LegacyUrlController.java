package com.example.shortener.api;

import com.example.shortener.application.AnalyticsView;
import com.example.shortener.application.CreateUrlCommand;
import com.example.shortener.application.CreateUrlResult;
import com.example.shortener.application.UrlService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unscoped demo APIs. Enabled only when {@code app.legacy-unscoped-api.enabled=true}
 * (tests / local demos). Disabled in prod-like where every link must belong to an org.
 */
@RestController
@ConditionalOnProperty(name = "app.legacy-unscoped-api.enabled", havingValue = "true")
public class LegacyUrlController {

    private static final String SHORT_CODE = "[A-Za-z0-9_-]{3,32}";

    private final UrlService service;

    public LegacyUrlController(UrlService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/urls")
    public ResponseEntity<CreateUrlResponse> create(@Valid @RequestBody CreateUrlRequest request) {
        CreateUrlResult result = service.create(new CreateUrlCommand(
                request.originalUrl(),
                request.customAlias(),
                request.expiresAt()
        ));
        CreateUrlResponse response = new CreateUrlResponse(
                result.id(),
                result.shortCode(),
                result.shortUrl(),
                result.originalUrl(),
                result.expiresAt(),
                result.createdAt()
        );
        return ResponseEntity.created(URI.create("/api/v1/urls/" + response.shortCode())).body(response);
    }

    @GetMapping("/api/v1/urls/{shortCode:" + SHORT_CODE + "}")
    public UrlDetailsResponse get(@PathVariable String shortCode) {
        var details = service.get(shortCode);
        return new UrlDetailsResponse(
                details.id(),
                details.shortCode(),
                details.shortUrl(),
                details.originalUrl(),
                details.title(),
                details.status(),
                details.expiresAt(),
                details.createdAt(),
                details.totalClicks(),
                details.lastAccessedAt()
        );
    }

    @GetMapping("/api/v1/urls/{shortCode:" + SHORT_CODE + "}/analytics")
    public AnalyticsResponse analytics(@PathVariable String shortCode) {
        AnalyticsView view = service.analytics(shortCode);
        return new AnalyticsResponse(
                view.shortCode(),
                view.totalClicks(),
                view.lastAccessedAt(),
                view.expiresAt()
        );
    }
}
