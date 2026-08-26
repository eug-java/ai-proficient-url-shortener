package com.example.shortener.customdomain;

import com.example.shortener.audit.AuditService;
import com.example.shortener.domain.InvalidRequestException;
import com.example.shortener.org.OrgAccessService;
import java.net.IDN;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomDomainService {

    public record DomainView(
            UUID id,
            String hostname,
            boolean verified,
            String txtName,
            String txtValue,
            java.time.Instant createdAt,
            java.time.Instant verifiedAt
    ) {}

    private final OrgCustomDomainRepository domains;
    private final OrgAccessService access;
    private final AuditService audit;
    private final Clock clock;
    private final String defaultHost;
    private final SecureRandom random = new SecureRandom();

    public CustomDomainService(
            OrgCustomDomainRepository domains,
            OrgAccessService access,
            AuditService audit,
            Clock clock,
            @Value("${app.base-url}") String baseUrl
    ) {
        this.domains = domains;
        this.access = access;
        this.audit = audit;
        this.clock = clock;
        this.defaultHost = hostFromBaseUrl(baseUrl);
    }

    @Transactional
    public DomainView add(UUID orgId, String actor, String hostname) {
        access.requireManage(orgId, actor);
        String normalized = normalizeHostname(hostname);
        if (normalized.equals(defaultHost) || normalized.endsWith(".localhost") || "localhost".equals(normalized)) {
            throw new InvalidRequestException("Cannot register the default application host as a custom domain");
        }
        if (domains.findByHostnameIgnoreCase(normalized).isPresent()) {
            throw new InvalidRequestException("Hostname is already registered");
        }
        String token = "shortener-verify-" + HexFormat.of().formatHex(random.generateSeed(16));
        OrgCustomDomain saved = domains.save(new OrgCustomDomain(
                UUID.randomUUID(),
                orgId,
                normalized,
                token,
                clock.instant()
        ));
        audit.record(orgId, actor, "DOMAIN_ADDED", "CustomDomain", saved.getId().toString(),
                Map.of("hostname", normalized));
        return toView(saved);
    }

    @Transactional
    public DomainView verify(UUID orgId, String actor, UUID domainId, boolean skipDnsCheck) {
        access.requireManage(orgId, actor);
        OrgCustomDomain domain = domains.findByIdAndOrganizationId(domainId, orgId)
                .orElseThrow(() -> new InvalidRequestException("Custom domain not found"));
        if (!domain.isVerified()) {
            if (!skipDnsCheck && !txtRecordMatches(domain.getHostname(), domain.getVerificationToken())) {
                throw new InvalidRequestException(
                        "TXT record _shortener-verify." + domain.getHostname()
                                + " does not contain " + domain.getVerificationToken()
                );
            }
            domain.markVerified(clock.instant());
            audit.record(orgId, actor, "DOMAIN_VERIFIED", "CustomDomain", domain.getId().toString(),
                    Map.of("hostname", domain.getHostname()));
        }
        return toView(domain);
    }

    @Transactional(readOnly = true)
    public List<DomainView> list(UUID orgId, String actor) {
        access.requireMember(orgId, actor);
        return domains.findAllByOrganizationIdOrderByCreatedAtDesc(orgId).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public Optional<UUID> resolveVerifiedOrgId(String hostHeader) {
        if (hostHeader == null || hostHeader.isBlank()) {
            return Optional.empty();
        }
        String host = normalizeHostname(hostHeader.split(":", 2)[0]);
        if (host.equals(defaultHost)) {
            return Optional.empty();
        }
        return domains.findByHostnameIgnoreCase(host)
                .filter(OrgCustomDomain::isVerified)
                .map(OrgCustomDomain::getOrganizationId);
    }

    public boolean isDefaultHost(String hostHeader) {
        if (hostHeader == null || hostHeader.isBlank()) {
            return true;
        }
        String host = normalizeHostname(hostHeader.split(":", 2)[0]);
        return host.equals(defaultHost) || "localhost".equals(host) || host.endsWith(".localhost");
    }

    private DomainView toView(OrgCustomDomain d) {
        return new DomainView(
                d.getId(),
                d.getHostname(),
                d.isVerified(),
                "_shortener-verify." + d.getHostname(),
                d.getVerificationToken(),
                d.getCreatedAt(),
                d.getVerifiedAt()
        );
    }

    private static String normalizeHostname(String raw) {
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.endsWith(".")) {
            value = value.substring(0, value.length() - 1);
        }
        try {
            value = IDN.toASCII(value);
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Invalid hostname");
        }
        if (!value.matches("^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)+$")
                && !"localhost".equals(value)) {
            throw new InvalidRequestException("Invalid hostname");
        }
        return value;
    }

    private static String hostFromBaseUrl(String baseUrl) {
        try {
            return normalizeHostname(java.net.URI.create(baseUrl).getHost());
        } catch (Exception ex) {
            return "localhost";
        }
    }

    /**
     * DNS TXT lookup. Failures return false so callers can surface a clear verification error.
     */
    boolean txtRecordMatches(String hostname, String expected) {
        try {
            javax.naming.directory.Attributes attrs = new javax.naming.directory.InitialDirContext()
                    .getAttributes("dns:/_shortener-verify." + hostname, new String[] {"TXT"});
            javax.naming.directory.Attribute txt = attrs.get("TXT");
            if (txt == null) {
                return false;
            }
            for (int i = 0; i < txt.size(); i++) {
                Object value = txt.get(i);
                if (value != null && value.toString().replace("\"", "").contains(expected)) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }
}
