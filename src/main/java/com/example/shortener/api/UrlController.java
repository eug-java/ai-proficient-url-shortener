package com.example.shortener.api;

import com.example.shortener.application.AnalyticsView;
import com.example.shortener.application.CreateUrlCommand;
import com.example.shortener.application.CreateUrlResult;
import com.example.shortener.application.UrlDetails;
import com.example.shortener.application.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "URL Shortener", description = "URL creation, redirection, and analytics")
public class UrlController {

    private static final String SHORT_CODE = "[A-Za-z0-9_-]{3,32}";

    private final UrlService service;

    public UrlController(UrlService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/urls")
    @Operation(summary = "Create a short URL")
    @ApiResponse(responseCode = "201", description = "Short URL created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "409", description = "Custom alias already exists")
    public ResponseEntity<CreateUrlResponse> create(@Valid @RequestBody CreateUrlRequest request) {
        CreateUrlResult result = service.create(new CreateUrlCommand(
                request.originalUrl(),
                request.customAlias(),
                request.expiresAt()
        ));
        CreateUrlResponse response = toCreateResponse(result);
        return ResponseEntity.created(URI.create("/api/v1/urls/" + response.shortCode())).body(response);
    }

    @GetMapping("/api/v1/urls/{shortCode:" + SHORT_CODE + "}")
    @Operation(summary = "Get short URL details")
    @ApiResponse(responseCode = "200", description = "Short URL details returned")
    @ApiResponse(responseCode = "404", description = "Short code not found")
    public UrlDetailsResponse get(@PathVariable String shortCode) {
        return toDetailsResponse(service.get(shortCode));
    }

    @GetMapping("/{shortCode:" + SHORT_CODE + "}")
    @Operation(summary = "Redirect to the original URL")
    @ApiResponse(responseCode = "302", description = "Redirect to original URL")
    @ApiResponse(responseCode = "404", description = "Short code not found")
    @ApiResponse(responseCode = "410", description = "Short URL expired")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(service.resolve(shortCode)))
                .build();
    }

    @GetMapping("/api/v1/urls/{shortCode:" + SHORT_CODE + "}/analytics")
    @Operation(summary = "Get click analytics")
    @ApiResponse(responseCode = "200", description = "Analytics returned")
    @ApiResponse(responseCode = "404", description = "Short code not found")
    public AnalyticsResponse analytics(@PathVariable String shortCode) {
        return toAnalyticsResponse(service.analytics(shortCode));
    }

    private static CreateUrlResponse toCreateResponse(CreateUrlResult result) {
        return new CreateUrlResponse(
                result.id(),
                result.shortCode(),
                result.shortUrl(),
                result.originalUrl(),
                result.expiresAt(),
                result.createdAt()
        );
    }

    private static UrlDetailsResponse toDetailsResponse(UrlDetails details) {
        return new UrlDetailsResponse(
                details.id(),
                details.shortCode(),
                details.shortUrl(),
                details.originalUrl(),
                details.expiresAt(),
                details.createdAt(),
                details.totalClicks(),
                details.lastAccessedAt()
        );
    }

    private static AnalyticsResponse toAnalyticsResponse(AnalyticsView view) {
        return new AnalyticsResponse(
                view.shortCode(),
                view.totalClicks(),
                view.lastAccessedAt(),
                view.expiresAt()
        );
    }
}
