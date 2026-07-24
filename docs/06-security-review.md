# Security Review

Implemented: HTTP/HTTPS allowlist, host requirement, credential rejection, length limits, private/loopback/link-local/site-local/multicast/IPv6-ULA/CGNAT/metadata host rejection, alias allowlist, environment-based DB credentials, non-root container, generic client errors with server-side ERROR logging for unexpected failures, sanitized request-id correlation, INFO audit logs for create/redirect that include `shortCode` only, default Actuator lockdown to `health`, JaCoCo coverage gate, CI dependency scanning (Trivy), and no routine logging of destination URLs.

The current service does not fetch user URLs, so redirect itself is not a direct SSRF path. Destination host checks are an **anti-abuse policy**: every resolved A/AAAA address from `InetAddress.getAllByName` must be publicly routable (loopback, link-local, site-local, multicast, IPv6 ULA, and CGNAT are rejected). Unresolvable hosts fail closed. DNS rebinding after a successful create remains a residual risk and is accepted for this assessment scope (see ADR-012 and `docs/12-production-risk-register.md`). The `prod` profile disables Swagger/OpenAPI and keeps Actuator limited to `health`.

Authentication and rate limiting are intentionally out of scope for this assessment. Production deployments would still need authorization, abuse detection, TLS, and secret management. See the production risk register for priorities.

## URL and route protection

- Only `http` and `https` schemes are accepted; `file`, `javascript`, `data`, and other schemes are rejected.
- URLs containing user-info credentials are rejected to reduce accidental credential exposure.
- Loopback, link-local, site-local, multicast, IPv6 unique-local (`fc00::/7`), CGNAT (`100.64.0.0/10`), `localhost`, and common metadata hosts are rejected for **every** resolved address.
- Reserved aliases prevent user-created redirects from shadowing operational and documentation routes such as `/actuator`, `/swagger-ui`, and `/v3`.
- Path short codes are constrained to 3–32 characters to match alias policy and the database column.
- Alias and URL values are never used as metric tags, preventing unbounded metric cardinality.
- `/actuator/prometheus` is disabled by default and must be enabled explicitly (Compose does this for local demos).
- `X-Request-Id` is accepted only when it matches `[A-Za-z0-9._-]{1,64}`; otherwise a UUID is generated.
