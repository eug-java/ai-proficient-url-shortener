package com.example.shortener.application;

import com.example.shortener.audit.AuditService;
import com.example.shortener.cache.RedirectCacheService;
import com.example.shortener.domain.AliasGenerator;
import com.example.shortener.domain.DuplicateAliasException;
import com.example.shortener.domain.ExpiredException;
import com.example.shortener.domain.InvalidRequestException;
import com.example.shortener.domain.NotFoundException;
import com.example.shortener.domain.UrlMapping;
import com.example.shortener.domain.UrlPolicy;
import com.example.shortener.messaging.ClickOutboxService;
import com.example.shortener.observability.AliasType;
import com.example.shortener.observability.AnalyticsResult;
import com.example.shortener.observability.RedirectResult;
import com.example.shortener.observability.ShortenerMetrics;
import com.example.shortener.org.OrgAccessService;
import com.example.shortener.org.OrgQuotaService;
import com.example.shortener.persistence.UrlMappingRepository;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UrlService {

    private static final Logger log = LoggerFactory.getLogger(UrlService.class);
    private static final String UNIQUE_VIOLATION = "23505";
    private static final String SHORT_CODE_UNIQUE_CONSTRAINT = "uk_url_mapping_short_code";

    private final UrlMappingRepository repository;
    private final UrlPolicy policy;
    private final AliasGenerator generator;
    private final Clock clock;
    private final String baseUrl;
    private final ShortenerMetrics metrics;
    private final AnalyticsWriter analyticsWriter;
    private final UrlMappingWriter mappingWriter;
    private final int generationAttempts;
    private final OrgAccessService orgAccess;
    private final RedirectCacheService redirectCache;
    private final ClickOutboxService clickOutbox;
    private final OrgQuotaService orgQuota;
    private final AuditService audit;

    public UrlService(
            UrlMappingRepository repository,
            UrlPolicy policy,
            AliasGenerator generator,
            Clock clock,
            @Value("${app.base-url}") String baseUrl,
            ShortenerMetrics metrics,
            AnalyticsWriter analyticsWriter,
            UrlMappingWriter mappingWriter,
            @Value("${app.alias-generation-attempts:5}") int generationAttempts,
            OrgAccessService orgAccess,
            RedirectCacheService redirectCache,
            ClickOutboxService clickOutbox,
            OrgQuotaService orgQuota,
            AuditService audit
    ) {
        this.repository = repository;
        this.policy = policy;
        this.generator = generator;
        this.clock = clock;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.metrics = metrics;
        this.analyticsWriter = analyticsWriter;
        this.mappingWriter = mappingWriter;
        this.generationAttempts = generationAttempts;
        this.orgAccess = orgAccess;
        this.redirectCache = redirectCache;
        this.clickOutbox = clickOutbox;
        this.orgQuota = orgQuota;
        this.audit = audit;
    }

    public CreateUrlResult create(CreateUrlCommand command) {
        return createInternal(null, null, null, command);
    }

    public CreateUrlResult create(UUID orgId, String sub, String title, CreateUrlCommand command) {
        orgAccess.requireWrite(orgId, sub);
        return createInternal(orgId, sub, title, command);
    }

    private CreateUrlResult createInternal(UUID orgId, String sub, String title, CreateUrlCommand command) {
        if (orgId != null) {
            orgQuota.assertCanCreateLink(orgId);
        }
        String url = policy.validateUrl(command.originalUrl());
        String alias = policy.validateAlias(command.customAlias());
        Instant now = clock.instant();
        AliasType aliasType = alias == null ? AliasType.GENERATED : AliasType.CUSTOM;

        if (command.expiresAt() != null && !command.expiresAt().isAfter(now)) {
            throw new InvalidRequestException("Expiration must be in the future");
        }

        int attempts = alias == null ? generationAttempts : 1;
        for (int attempt = 0; attempt < attempts; attempt++) {
            String code = alias == null ? generator.generate() : alias;
            try {
                UrlMapping saved = mappingWriter.insert(
                        orgId == null
                                ? new UrlMapping(UUID.randomUUID(), code, url, command.expiresAt(), now)
                                : new UrlMapping(UUID.randomUUID(), orgId, sub, code, url, title, command.expiresAt(), now)
                );
                if (orgId != null) {
                    orgQuota.recordCreated(orgId);
                    audit.record(orgId, sub, "LINK_CREATED", "UrlMapping", saved.getId().toString(),
                            Map.of("shortCode", code));
                }
                metrics.urlCreated(aliasType);
                log.info(
                        "Created short URL shortCode={} aliasType={}",
                        code,
                        aliasType.name().toLowerCase()
                );
                return new CreateUrlResult(
                        saved.getId(),
                        code,
                        baseUrl + "/" + code,
                        url,
                        saved.getExpiresAt(),
                        saved.getCreatedAt()
                );
            } catch (DataIntegrityViolationException exception) {
                if (!isUniqueViolation(exception)) {
                    throw exception;
                }
                if (alias != null) {
                    throw new DuplicateAliasException();
                }
                metrics.aliasCollision(AliasType.GENERATED);
                log.debug("Generated alias collision on attempt {}", attempt + 1);
            }
        }

        throw new IllegalStateException(
                "Unable to generate a unique alias after " + generationAttempts + " attempts"
        );
    }

    @Transactional
    public String resolve(String code) {
        return resolve(code, null);
    }

    @Transactional
    public String resolve(String code, ClickOutboxService.Context context) {
        return resolve(code, context, null);
    }

    @Transactional
    public String resolve(String code, ClickOutboxService.Context context, UUID hostOrganizationId) {
        long start = System.nanoTime();
        RedirectResult result = RedirectResult.NOT_FOUND;
        try {
            UrlMapping mapping = hostOrganizationId == null
                    ? redirectCache.find(code).orElse(null)
                    : repository.findByOrganizationIdAndShortCode(hostOrganizationId, code).orElse(null);
            if (mapping == null) {
                result = RedirectResult.NOT_FOUND;
                throw new NotFoundException();
            }
            if (hostOrganizationId != null && mapping.getOrganizationId() != null
                    && !hostOrganizationId.equals(mapping.getOrganizationId())) {
                throw new NotFoundException();
            }

            if (mapping.isExpired(clock.instant())) {
                result = RedirectResult.EXPIRED;
                throw new ExpiredException();
            }
            if (mapping.getStatus() != UrlMapping.Status.ACTIVE) throw new NotFoundException();

            // Re-validate destination host at redirect time (DNS rebinding residual mitigation).
            try {
                policy.validateUrl(mapping.getOriginalUrl());
            } catch (InvalidRequestException ex) {
                redirectCache.evict(code);
                throw new NotFoundException();
            }

            try {
                if (mapping.getOrganizationId() != null && context != null) clickOutbox.record(mapping, context);
                else analyticsWriter.recordClick(code);
                metrics.analyticsUpdate(AnalyticsResult.SUCCESS);
            } catch (RuntimeException exception) {
                metrics.analyticsUpdate(AnalyticsResult.FAILURE);
                log.warn("Analytics update failed for shortCode={}", code, exception);
            }

            result = RedirectResult.SUCCESS;
            return mapping.getOriginalUrl();
        } finally {
            log.info("Redirect shortCode={} result={}", code, result.name().toLowerCase());
            metrics.redirect(result);
            metrics.redirectDuration(result, System.nanoTime() - start);
        }
    }

    @Transactional(readOnly = true)
    public UrlDetails get(String code) {
        UrlMapping mapping = repository.findByShortCode(code)
                .orElseThrow(NotFoundException::new);
        return toDetails(mapping);
    }

    @Transactional(readOnly=true)
    public UrlDetails get(UUID orgId,String sub,String code){
        orgAccess.requireMember(orgId,sub);
        return toDetails(repository.findByOrganizationIdAndShortCode(orgId,code).orElseThrow(NotFoundException::new));
    }
    @Transactional(readOnly=true)
    public java.util.List<UrlDetails> list(UUID orgId,String sub){
        orgAccess.requireMember(orgId,sub);
        return repository.findAllByOrganizationIdAndStatusNotOrderByCreatedAtDesc(orgId,UrlMapping.Status.DELETED)
                .stream().map(this::toDetails).toList();
    }
    @Transactional
    public UrlDetails update(UUID orgId,String sub,String code,String originalUrl,String title,Instant expiresAt){
        orgAccess.requireWrite(orgId,sub);var m=repository.findByOrganizationIdAndShortCode(orgId,code).orElseThrow(NotFoundException::new);
        m.update(policy.validateUrl(originalUrl),title,expiresAt,clock.instant());redirectCache.evict(code);
        audit.record(orgId, sub, "LINK_UPDATED", "UrlMapping", m.getId().toString(), Map.of("shortCode", code));
        return toDetails(m);
    }
    @Transactional public void disable(UUID orgId,String sub,String code){
        orgAccess.requireWrite(orgId,sub);var m=repository.findByOrganizationIdAndShortCode(orgId,code).orElseThrow(NotFoundException::new);
        m.disable(clock.instant());redirectCache.evict(code);
        audit.record(orgId, sub, "LINK_DISABLED", "UrlMapping", m.getId().toString(), Map.of("shortCode", code));
    }
    @Transactional public void delete(UUID orgId,String sub,String code){
        orgAccess.requireDelete(orgId,sub);var m=repository.findByOrganizationIdAndShortCode(orgId,code).orElseThrow(NotFoundException::new);
        m.delete(clock.instant());redirectCache.evict(code);
        audit.record(orgId, sub, "LINK_DELETED", "UrlMapping", m.getId().toString(), Map.of("shortCode", code));
    }

    @Transactional(readOnly = true)
    public AnalyticsView analytics(String code) {
        UrlMapping mapping = repository.findByShortCode(code)
                .orElseThrow(NotFoundException::new);
        return new AnalyticsView(
                code,
                mapping.getTotalClicks(),
                mapping.getLastAccessedAt(),
                mapping.getExpiresAt()
        );
    }

    private UrlDetails toDetails(UrlMapping mapping) {
        return new UrlDetails(
                mapping.getId(),
                mapping.getShortCode(),
                baseUrl + "/" + mapping.getShortCode(),
                mapping.getOriginalUrl(),
                mapping.getTitle(),
                mapping.getStatus().name(),
                mapping.getExpiresAt(),
                mapping.getCreatedAt(),
                mapping.getTotalClicks(),
                mapping.getLastAccessedAt()
        );
    }

    private boolean isUniqueViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception.getMostSpecificCause();
        if (!(cause instanceof SQLException sqlException)) {
            return false;
        }
        if (!UNIQUE_VIOLATION.equals(sqlException.getSQLState())) {
            return false;
        }
        String message = sqlException.getMessage();
        // Require named unique constraint so unrelated 23505 errors are not mapped to DUPLICATE_ALIAS.
        // PostgreSQL includes the constraint name in the driver message; avoid compile-time driver coupling.
        return message != null
                && message.toLowerCase(Locale.ROOT).contains(SHORT_CODE_UNIQUE_CONSTRAINT);
    }
}
