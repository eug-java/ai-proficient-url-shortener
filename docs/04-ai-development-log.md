# AI Development Log

Evidence of engineer-led AI-assisted delivery. Each entry includes tool, prompt intent, AI suggestion, human decision, files, validation, and result.

**Tooling used across the project:** Cursor IDE Agent (chat + codebase tools). AI output was treated as untrusted draft; no secrets or production data were sent to AI.

**Traceability note:** This assessment archive was finalized as a working tree. Validation evidence: `./mvnw clean verify` (see `docs/evidence/`). Baseline commit for this remediation pack: `3508457` (working tree may include later polish).

---

## AI-001 — Requirement normalization

| Field | Value |
|---|---|
| Date / stage | Kickoff — before coding |
| Tool | Cursor Agent |
| Task | Normalize functional/NFR requirements, ambiguities, MVP vs out-of-scope |

**Prompt (abridged):**
> Act as a senior software architect. Do not write code. Return functional requirements, non-functional requirements, ambiguities, assumptions, risks, out of scope, and backlog for a production-oriented URL shortener assessment.

**AI output (summary):** Proposed Redis cache, Kafka analytics, OAuth, rate limiting, multi-region as MVP.

**Engineer review:** Rejected as MVP. Kept create/redirect/analytics + durability + observability. Deferred auth/rate limits/cache/broker to production evolution.

**Final change:** `docs/01-requirements.md`, `docs/03-backlog.md`

**Validation:** Cross-check against assignment rubric (scope honesty).

**Result:** Accepted with modification.

---

## AI-002 — Architecture alternatives

| Field | Value |
|---|---|
| Date / stage | Architecture |
| Tool | Cursor Agent |
| Task | Compare modular monolith vs microservices vs serverless |

**Prompt (abridged):**
> Compare modular monolith, microservices, and serverless for this URL shortener by delivery risk, scalability, testability, and operational complexity within a timeboxed assessment.

**AI output:** Preferred microservices “for production-grade scale”.

**Engineer review:** Rejected. Microservices add distributed complexity without assessment benefit. Modular monolith with clear packages preserves extraction options.

**Final change:** `docs/02-architecture.md`, package layout under `com.example.shortener.*`

**Validation:** Architecture review against timebox and testability.

**Result:** Rejected AI preference; selected modular monolith.

---

## AI-003 — Duplicate alias concurrency

| Field | Value |
|---|---|
| Date / stage | Core persistence design |
| Tool | Cursor Agent |
| Task | Correct uniqueness under concurrent create |

**Prompt (abridged):**
> Review duplicate alias handling across multiple instances. What is the authoritative control?

**AI output:** Suggested `existsByShortCode()` then insert.

**Engineer review:** Rejected as authoritative control (TOCTOU race). Database unique constraint is the source of truth; map violation to HTTP 409 for custom aliases.

**Final change:**
- `V1__create_url_mapping.sql` — `CONSTRAINT uk_url_mapping_short_code UNIQUE (short_code)`
- `UrlService` — detect SQLState `23505` + constraint name
- `UrlMappingWriter` — `REQUIRES_NEW` inserts for generated-code retries

**Validation:** `RandomAliasCollisionIntegrationTest`, duplicate-alias IT → 409

**Result:** Accepted unique constraint; rejected exists-then-insert.

---

## AI-004 — URL security / destination fetch

| Field | Value |
|---|---|
| Date / stage | Security design |
| Tool | Cursor Agent |
| Task | Validate destination URLs safely |

**Prompt (abridged):**
> Review URL validation, redirect abuse, SSRF boundaries, and sensitive logging for a shortener that issues HTTP redirects.

**AI output:** Fetch destination to verify availability / status.

**Engineer review:** Rejected — introduces SSRF and latency. Service never fetches destinations. Validate scheme/host/credentials/length; apply anti-abuse private-address policy via DNS resolution of **all** A/AAAA records.

**Final change:** `UrlPolicy.java` (`getAllByName` + address checks), `docs/06-security-review.md`

**Validation:** DomainTests for private/ULA/CGNAT/unresolved/credentials hosts

**Result:** Rejected fetch; accepted local validation + anti-abuse policy.

---

## AI-005 — Analytics reliability vs redirect availability

| Field | Value |
|---|---|
| Date / stage | Ambiguous scenario |
| Tool | Cursor Agent |
| Task | Clarify “make redirects more reliable” |

**Prompt (abridged):**
> Requirement is ambiguous: “make redirects more reliable.” Propose interpretations and designs.

**AI output:** Synchronous exact counting inside redirect transaction.

**Engineer review:** Modified. Availability wins for prototype: best-effort analytics; production evolution = async events.

**Final change:** Design in `docs/08-scenarios.md` (ambiguous), ADR best-effort analytics

**Validation:** Later proven by `AnalyticsFailureIntegrationTest`

**Result:** Modified.

---

## AI-006 — Test discovery

| Field | Value |
|---|---|
| Date / stage | Testing |
| Tool | Cursor Agent |
| Task | Propose automated tests |

**AI output:** Included live-network HTTP calls to public sites as primary integration tests.

**Engineer review:** Removed live-network dependency as gate. Kept Testcontainers PostgreSQL + MockMvc for 302/`Location`.

**Final change:** `UrlShortenerIntegrationTest`, `DomainTests`

**Validation:** `./mvnw clean verify`

**Result:** Modified.

---

## AI-007 — Staff / architecture review

| Field | Value |
|---|---|
| Date / stage | Mid implementation |
| Tool | Cursor Agent |
| Task | Staff-engineer style review |

**Accepted:** Inject `Clock`, keep DTO/entity separation, add metrics, document technical debt.

**Final change:** `ApplicationConfig` Clock bean; Micrometer `ShortenerMetrics`; ADRs

**Validation:** Unit + integration tests; metrics registration tests

**Result:** Accepted selected findings.

---

## AI-009 — Transaction boundary defect (analytics)

| Field | Value |
|---|---|
| Date / stage | Brownfield defect fix |
| Tool | Cursor Agent |
| Task | Fix redirect 500 when recording analytics |

**Prompt (abridged):**
> Valid redirect returns 500. PostgreSQL: cannot execute UPDATE in a read-only transaction. Also UnexpectedRollbackException. Diagnose and fix with Spring transactions.

**AI output:** Put `@Transactional(propagation = REQUIRES_NEW)` on `UrlService.recordClick()` in the same class.

**Engineer review:** **Rejected** — self-invocation bypasses Spring proxy; new transaction never starts.

**Final change:**
- Created `AnalyticsWriter.java` (separate bean, `REQUIRES_NEW`)
- Updated `UrlService.resolve`
- Added `AnalyticsFailureIntegrationTest`

**Files:**
- `application/AnalyticsWriter.java`
- `application/UrlService.java`
- `AnalyticsFailureIntegrationTest.java`

**Validation:**
```bash
./mvnw -Dtest=AnalyticsFailureIntegrationTest,UrlShortenerIntegrationTest#redirectShouldUpdateAnalyticsWithoutFailingReadTransaction test
```

**Result:** Passed. Documented in ADR-007.

---

## AI-010 — Redirect assertion pitfall

| Field | Value |
|---|---|
| Date / stage | Testing |
| Tool | Cursor Agent |
| Task | Assert original 302 + Location |

**Observed:** `TestRestTemplate` followed redirects and hid the service 302.

**Engineer decision:** MockMvc for redirect assertions; Testcontainers for DB.

**Validation:** IT asserts `302`, `Location`, analytics increment.

**Result:** Accepted engineer correction over “just use RestTemplate”.

---

## AI-011 — OpenAPI, ProblemDetail, metrics, CI

| Field | Value |
|---|---|
| Date / stage | Production-readiness slice |
| Tool | Cursor Agent |
| Task | Improve discoverability, errors, observability, CI |

**AI suggestions:** springdoc, ProblemDetail, business metrics, Docker in CI.

**Engineer decisions:** Accepted with review (ProblemDetail `instance` from request URI; low-cardinality metric tags; Actuator lockdown by profile).

**Files:** `GlobalExceptionHandler`, `OpenApiConfig`, `ShortenerMetrics`, `.github/workflows/ci.yml`

**Validation:** `./mvnw clean verify`; Swagger on `local` profile; prometheus scrape name unit test

**Result:** Accepted with modification.

---

## AI-012 — Metrics naming / Prometheus mangling

| Field | Value |
|---|---|
| Date / stage | Observability hardening |
| Tool | Cursor Agent |
| Task | Align Micrometer names with Prometheus scrape output |

**AI / review finding:** `shortener.urls.created.total` mangles to `shortener_urls_total`.

**Engineer decision:** Rename to `shortener.urls.creations`; assert scrape string in unit test.

**Files:** `ShortenerMetrics.java`, `ShortenerMetricsTest.java`, README metrics table

**Validation:** `./mvnw -Dtest=ShortenerMetricsTest,PrometheusMetricsIntegrationTest test`

**Result:** Passed.

---

## AI-013 — Layering cleanup (application must not depend on api)

| Field | Value |
|---|---|
| Date / stage | Clean architecture pass |
| Tool | Cursor Agent |
| Task | Remove application → api dependency |

**Final change:** `CreateUrlCommand` / `CreateUrlResult` / `UrlDetails` / `AnalyticsView`; domain exceptions; controller mapping.

**Validation:** `./mvnw clean verify`; package import check (no `application` → `api` imports)

**Result:** Accepted.

---

## AI-014 — Host policy & request-id polish

| Field | Value |
|---|---|
| Date / stage | Security polish |
| Tool | Cursor Agent |
| Task | Close ULA/CGNAT gaps; sanitize request IDs; profiles |

**Engineer decisions:**
- Block IPv6 ULA + CGNAT; resolve **all** addresses
- Fail closed on `UnknownHostException` (anti-abuse trade-off documented)
- Sanitize `X-Request-Id`
- Profiles `local` / `test` / `prod`

**Validation:** DomainTests + request-id IT + `./mvnw clean verify`

**Result:** Accepted.

---

## AI-015 — Assessment remediation (traceability, DNS, uniqueness, evidence)

| Field | Value |
|---|---|
| Date / stage | Pre-submission hardening |
| Tool | Cursor Agent (Grok / Cursor) |
| Task | Close interviewer gaps: AI traceability, scenarios, DNS all-addresses, named unique constraint, analytics `@Version`, Maven Wrapper, risk register, verify evidence |

**Prompt (abridged):**
> Address the assessment feedback: strengthen AI log with AI-NNN entries (tool/prompt/reject/files/validation/commit), expand three scenarios with decomposition/execution/validation, fix DNS to validate all resolved addresses as anti-abuse (not SSRF), detect unique violations via SQLState 23505 + named constraint, bump version on analytics JPQL, add Maven Wrapper, expand final summary and production risk register, run `./mvnw clean verify` and save evidence.

**AI output:** Proposed same-bean `REQUIRES_NEW` historically; for this pass proposed `getAllByName`, named `uk_url_mapping_short_code`, `u.version = u.version + 1`, and structured docs.

**Engineer review:** Accepted DNS/constraint/version/docs/wrapper. Kept auth/rate-limit out of scope; recorded residual DNS-rebinding and open-read risks in `docs/12-production-risk-register.md`.

**Final change:**
- `UrlPolicy.java`, `UrlService.java`, `UrlMappingRepository.java`, `V1__create_url_mapping.sql`
- `docs/04`, `08`, `09`, `12`, `evidence/`
- Maven Wrapper (`mvnw`, `.mvn/wrapper/`)
- CI uses `./mvnw`

**Validation:**
```bash
./mvnw clean verify
```
Evidence: `docs/evidence/mvn-verify.log`, `docs/evidence/surefire-summary.txt`, `docs/evidence/jacoco/` (line coverage gate met; instructions ~89%).

**Commit:** `3508457`

**Result:** Accepted. BUILD SUCCESS — 33 tests, 0 failures.

---

## Secure AI usage

- No secrets, credentials, customer data, or production datasets were pasted into prompts.
- AI suggestions that implied outbound fetches, exists-then-insert races, or self-invocation transactions were rejected with rationale.
- Every significant change was validated by automated tests and/or documented quality gates above.

---

## Index of rejected AI suggestions

| ID | Suggestion | Why rejected |
|---|---|---|
| AI-002 | Microservices MVP | Delivery risk vs assessment timebox |
| AI-003 | exists-then-insert uniqueness | Race under concurrency |
| AI-004 | Fetch destination URL | SSRF + latency |
| AI-009 | REQUIRES_NEW on same-bean method | Spring proxy self-invocation |
| AI-001 | Auth/Redis/Kafka as MVP | Out of assessment scope (documented evolution) |
