package com.example.shortener.domain;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class UrlPolicy {

    private static final Set<String> SCHEMES = Set.of("http", "https");
    private static final Set<String> RESERVED_ALIASES = Set.of(
            "api",
            "actuator",
            "swagger-ui",
            "swagger-ui.html",
            "v3",
            "favicon.ico",
            "error"
    );
    private static final Set<String> BLOCKED_HOSTS = Set.of(
            "localhost",
            "metadata.google.internal",
            "metadata"
    );
    private static final Pattern ALIAS = Pattern.compile("^[A-Za-z0-9_-]+$");
    private static final int MIN_ALIAS_LENGTH = 3;
    private static final int MAX_ALIAS_LENGTH = 32;

    public String validateUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidRequestException("URL must not be blank");
        }
        if (raw.length() > 2048) {
            throw new InvalidRequestException("URL exceeds maximum length");
        }

        try {
            URI uri = new URI(raw.trim());
            String scheme = uri.getScheme() == null
                    ? null
                    : uri.getScheme().toLowerCase(Locale.ROOT);

            if (!SCHEMES.contains(scheme)) {
                throw new InvalidRequestException("Only HTTP and HTTPS are supported");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new InvalidRequestException("URL must contain a host");
            }
            if (uri.getUserInfo() != null) {
                throw new InvalidRequestException("URL credentials are not supported");
            }
            assertHostAllowed(uri.getHost());
            return uri.normalize().toString();
        } catch (URISyntaxException exception) {
            throw new InvalidRequestException("Malformed URL");
        }
    }

    public String validateAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            return null;
        }

        String value = alias.trim();
        if (value.length() < MIN_ALIAS_LENGTH || value.length() > MAX_ALIAS_LENGTH) {
            throw new InvalidRequestException("Custom alias must contain 3 to 32 characters");
        }
        if (!ALIAS.matcher(value).matches()) {
            throw new InvalidRequestException(
                    "Custom alias may contain only letters, digits, underscores, and hyphens"
            );
        }
        if (RESERVED_ALIASES.contains(value.toLowerCase(Locale.ROOT))) {
            throw new InvalidRequestException("Custom alias is reserved");
        }
        return value;
    }

    public void assertHostAllowed(String host) {
        String normalized = host.toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (BLOCKED_HOSTS.contains(normalized) || normalized.endsWith(".localhost")) {
            throw new InvalidRequestException("URL host is not allowed");
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(normalized);
            for (InetAddress address : addresses) {
                if (isBlockedAddress(address)) {
                    throw new InvalidRequestException("URL host is not allowed");
                }
            }
        } catch (UnknownHostException exception) {
            throw new InvalidRequestException("URL host could not be resolved");
        }
    }

    /**
     * Anti-abuse destination policy (not SSRF prevention — this service never fetches destinations).
     * Every resolved A/AAAA address must be publicly routable.
     */
    private boolean isBlockedAddress(InetAddress address) {
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()
                || isUniqueLocalIpv6(address)
                || isCarrierGradeNat(address);
    }

    /**
     * IPv6 unique-local addresses (fc00::/7). {@link InetAddress#isSiteLocalAddress()}
     * does not cover this range on modern JDKs.
     */
    private boolean isUniqueLocalIpv6(InetAddress address) {
        if (!(address instanceof Inet6Address inet6)) {
            return false;
        }
        byte[] bytes = inet6.getAddress();
        return (bytes[0] & 0xfe) == 0xfc;
    }

    /**
     * Shared address space for carrier-grade NAT (100.64.0.0/10).
     */
    private boolean isCarrierGradeNat(InetAddress address) {
        if (!(address instanceof Inet4Address)) {
            return false;
        }
        byte[] bytes = address.getAddress();
        int first = bytes[0] & 0xff;
        int second = bytes[1] & 0xff;
        return first == 100 && second >= 64 && second <= 127;
    }
}
