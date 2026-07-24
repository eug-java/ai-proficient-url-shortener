# Architecture Decisions

- Modular monolith over microservices: lower delivery and operational risk.
- PostgreSQL over in-memory storage: durability and authoritative uniqueness.
- Random Base62 over deterministic URL hash: supports multiple mappings and simple semantics.
- 302 over 301: avoids permanent client caching.
- Database uniqueness over application-only existence check: concurrency correctness.
- Best-effort analytics over synchronous exact counting: redirect availability.

## ADR-007 - Separate analytics transaction boundary

### Decision

Analytics writes are performed by a separate Spring-managed component using `REQUIRES_NEW`.

### Context

Redirect lookup is read-only, but analytics requires an update. A `REQUIRES_NEW` method inside the same service does not work because self-invocation bypasses Spring's transactional proxy.

### Consequences

- Analytics runs in an independent writable transaction.
- Analytics failure can be contained without rolling back redirect lookup.
- Transaction responsibilities are explicit.
- One additional application component is required.

## ADR-008 — RFC 9457 error responses

**Status:** Accepted

**Context:** API consumers need a consistent, machine-readable error contract without maintaining a proprietary response format.

**Decision:** Return Spring `ProblemDetail` responses with a stable application-specific `code` property and field-level validation errors where applicable.

**Consequences:** Clients receive standard fields (`type`, `title`, `status`, `detail`, `instance`) while preserving a stable `code` property for programmatic handling. `GlobalExceptionHandler` sets `instance` from the request URI.

## ADR-009 — Generated OpenAPI documentation

**Status:** Accepted

**Context:** Reviewers and API consumers need an immediately discoverable and executable API contract.

**Decision:** Generate OpenAPI 3 documentation from the Spring MVC application and expose Swagger UI.

**Consequences:** Documentation stays close to the controller contract. Endpoint descriptions still require deliberate review rather than assuming generated documentation is complete.

## ADR-010 — Business metrics alongside platform metrics

**Status:** Accepted

**Context:** JVM and HTTP metrics do not explain whether the shortening workflow itself is healthy.

**Decision:** Publish domain counters and redirect latency through Micrometer, including tagged error counts.

**Consequences:** Operators can distinguish creation, redirect, expiration, analytics, and validation behavior. Tags are restricted to bounded error codes to avoid high-cardinality metrics.

## ADR: Database uniqueness is the source of truth

**Decision:** Enforce alias uniqueness with PostgreSQL and treat pre-insert checks only as optional optimizations.

**Reason:** An `exists` check followed by an insert is vulnerable to race conditions. Two requests may observe the alias as free at the same time. The unique constraint provides the final atomic guarantee.

## ADR: Retry generated aliases in independent transactions

**Decision:** Execute each generated-alias insert through `UrlMappingWriter` with `REQUIRES_NEW`.

**Reason:** A failed flush caused by a unique constraint generally marks the current transaction rollback-only. Retrying in the same transaction is unsafe. Independent transactions make each attempt isolated and deterministic.

**Rejected alternative:** Return 409 for a random collision. A collision is an internal generation detail and should normally be retried rather than exposed as a client conflict.

## ADR-011 — Analytics bulk update bumps `@Version`

**Status:** Accepted (technical debt mitigated)

**Context:** Click analytics uses a bulk JPQL `UPDATE` for concurrency-safe increments. Leaving `@Version` unchanged would allow a future entity-based editor to overwrite rows without detecting concurrent analytics updates.

**Decision:** Increment `version` in the same JPQL update as `totalClicks` / `lastAccessedAt`.

**Consequences:** Optimistic locking stays coherent if edit APIs are added. Pure analytics path pays a trivial extra write. Long-term alternative remains a separate click store / event log.

## ADR-012 — Destination host anti-abuse policy (DNS at create)

**Status:** Accepted

**Context:** The service never fetches destination URLs (no server-side SSRF). Short links can still pivot browsers toward internal/metadata targets.

**Decision:** At create time, resolve **all** A/AAAA records via `InetAddress.getAllByName` and reject loopback, link-local, site-local, multicast, IPv6 ULA, and CGNAT. Unresolvable hosts fail closed. Document DNS rebinding after create as residual risk.

**Consequences:** Create latency depends on DNS; transient DNS failures reject otherwise valid URLs. This is an explicit anti-abuse trade-off for the assessment, not a claim of complete phishing prevention. Production may move checks off the synchronous path or re-validate at redirect.
