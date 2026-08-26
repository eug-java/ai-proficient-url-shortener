package com.example.shortener.api;

import com.example.shortener.application.CreateUrlCommand;
import com.example.shortener.application.CreateUrlResult;
import com.example.shortener.application.UrlDetails;
import com.example.shortener.application.UrlService;
import com.example.shortener.geo.GeoCountryResolver;
import com.example.shortener.messaging.ClickOutboxService;
import com.example.shortener.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "URL Shortener", description = "Org-scoped link management and public redirect")
public class UrlController {

    private static final String SHORT_CODE = "[A-Za-z0-9_-]{3,32}";

    private final UrlService service;
    private final CurrentUser currentUser;
    private final GeoCountryResolver geoCountryResolver;

    public UrlController(UrlService service, CurrentUser currentUser, GeoCountryResolver geoCountryResolver) {
        this.service = service;
        this.currentUser = currentUser;
        this.geoCountryResolver = geoCountryResolver;
    }

    public record UpdateUrlRequest(String originalUrl, String title, java.time.Instant expiresAt) {}

    @PostMapping("/api/v1/orgs/{orgId}/urls")
    @Operation(summary = "Create a short URL in an organization")
    public ResponseEntity<CreateUrlResponse> createOrg(
            @PathVariable UUID orgId,
            @Valid @RequestBody CreateUrlRequest request,
            Authentication auth,
            HttpServletRequest servletRequest
    ) {
        CurrentUser.User user = currentUser.require(auth, servletRequest);
        CreateUrlResult result = service.create(
                orgId,
                user.sub(),
                request.title(),
                new CreateUrlCommand(request.originalUrl(), request.customAlias(), request.expiresAt())
        );
        CreateUrlResponse response = toCreateResponse(result);
        return ResponseEntity.created(URI.create("/api/v1/orgs/" + orgId + "/urls/" + response.shortCode()))
                .body(response);
    }

    @GetMapping("/api/v1/orgs/{orgId}/urls")
    @Operation(summary = "List organization URLs")
    public List<UrlDetailsResponse> list(
            @PathVariable UUID orgId,
            Authentication auth,
            HttpServletRequest request
    ) {
        return service.list(orgId, currentUser.require(auth, request).sub()).stream()
                .map(UrlController::toDetailsResponse)
                .toList();
    }

    @GetMapping("/api/v1/orgs/{orgId}/urls/{shortCode:" + SHORT_CODE + "}")
    public UrlDetailsResponse getOrg(
            @PathVariable UUID orgId,
            @PathVariable String shortCode,
            Authentication auth,
            HttpServletRequest request
    ) {
        return toDetailsResponse(service.get(orgId, currentUser.require(auth, request).sub(), shortCode));
    }

    @PatchMapping("/api/v1/orgs/{orgId}/urls/{shortCode:" + SHORT_CODE + "}")
    public UrlDetailsResponse update(
            @PathVariable UUID orgId,
            @PathVariable String shortCode,
            @RequestBody UpdateUrlRequest body,
            Authentication auth,
            HttpServletRequest request
    ) {
        return toDetailsResponse(service.update(
                orgId,
                currentUser.require(auth, request).sub(),
                shortCode,
                body.originalUrl(),
                body.title(),
                body.expiresAt()
        ));
    }

    @PostMapping("/api/v1/orgs/{orgId}/urls/{shortCode:" + SHORT_CODE + "}/disable")
    public ResponseEntity<Void> disable(
            @PathVariable UUID orgId,
            @PathVariable String shortCode,
            Authentication auth,
            HttpServletRequest request
    ) {
        service.disable(orgId, currentUser.require(auth, request).sub(), shortCode);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/v1/orgs/{orgId}/urls/{shortCode:" + SHORT_CODE + "}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID orgId,
            @PathVariable String shortCode,
            Authentication auth,
            HttpServletRequest request
    ) {
        service.delete(orgId, currentUser.require(auth, request).sub(), shortCode);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{shortCode:" + SHORT_CODE + "}")
    @Operation(summary = "Public redirect")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode, HttpServletRequest request) {
        String country = geoCountryResolver.resolve(request).orElse(null);
        String destination = service.resolve(
                shortCode,
                new ClickOutboxService.Context(
                        clientIp(request),
                        request.getHeader("User-Agent"),
                        request.getHeader("Referer"),
                        country
                )
        );
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(destination)).build();
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null ? request.getRemoteAddr() : forwarded.split(",", 2)[0].trim();
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
                details.title(),
                details.status(),
                details.expiresAt(),
                details.createdAt(),
                details.totalClicks(),
                details.lastAccessedAt()
        );
    }
}
