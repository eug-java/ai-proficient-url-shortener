# Required Scenarios

Each scenario shows decomposition, execution, and validation with engineer ownership of AI output.

---

## 1. Greenfield — Build the initial URL shortener

### Requirement
Deliver create, redirect, and analytics for a durable URL shortener suitable for assessment demonstration.

### Initial codebase state
Empty repository (no application code). Only assignment brief / rubric available.

### Ambiguities
- Same long URL → one code or many?
- Auth required for MVP?
- Exact vs eventually consistent click counts?

### Impact analysis
Touches API contract, persistence model, uniqueness strategy, redirect semantics, testing approach, and documentation pack.

### Task decomposition
1. Normalize requirements and out-of-scope list.
2. Choose architecture (modular monolith vs alternatives).
3. Implement create + persistence + uniqueness.
4. Implement redirect (302) + analytics.
5. Add Testcontainers integration tests and README.

### AI contribution
- Compared architecture options and API shapes.
- Drafted initial Spring Boot structure and test ideas.
- Proposed Redis/Kafka/auth as “production defaults”.

### Engineer decisions
- Modular monolith + PostgreSQL + Flyway.
- Random Base62 codes; DB unique constraint as source of truth.
- Deferred auth/rate limits/cache/broker (documented).
- Multiple codes per URL allowed.

### Files changed (representative)
- `UrlController`, `UrlService`, `UrlMapping`, `UrlMappingRepository`
- `V1__create_url_mapping.sql`
- `UrlShortenerIntegrationTest`
- `docs/01-requirements.md`, `docs/02-architecture.md`

### Rejected options
- Microservices MVP
- In-memory-only store
- Deterministic hash-of-URL as only alias strategy

### Validation
```bash
./mvnw clean verify
```
Assert create `201`, persistence, redirect `302` + `Location`, negative inputs.

### Risks and remaining limitations
No ownership model; open create endpoint; phishing residual inherent to shorteners.

---

## 2. Brownfield — Isolate analytics so redirects stay available

### Requirement
After the greenfield MVP existed, valid redirects sometimes returned HTTP 500 when analytics updates failed transactionally. Fix without weakening redirect availability.

### Initial codebase state
Working create/redirect/analytics shortener where analytics was invoked from the read-only redirect transaction path (same Spring bean). Expiration and core schema already present in `V1`.

### Ambiguities
Does “reliable redirect” mean exact click counts, or “never fail a valid redirect because of analytics”?

### Impact analysis
- `UrlService.resolve` transaction attributes
- Analytics write path
- Error handling / metrics for analytics failure
- Regression tests must prove 302 even when analytics throws

### Task decomposition
1. Reproduce failure (read-only TX + UPDATE).
2. Diagnose Spring proxy self-invocation.
3. Extract writer bean with `REQUIRES_NEW`.
4. Add failure-injection integration test.
5. Document ADR-007 and scenario evidence.

### AI contribution
Suggested adding `@Transactional(REQUIRES_NEW)` on a private/same-class `recordClick()` method.

### Engineer decisions
**Rejected** same-bean `REQUIRES_NEW` (proxy bypass). Created `AnalyticsWriter` as a separate Spring bean. Redirect remains `readOnly`; analytics is best-effort.

### Files changed
- `application/AnalyticsWriter.java` (added)
- `application/UrlService.java` (modified)
- `AnalyticsFailureIntegrationTest.java` (added)
- `docs/07-architecture-decisions.md` (ADR-007)
- `docs/04-ai-development-log.md` (AI-009)

### Rejected options
- Dropping analytics entirely
- Making the whole redirect writable and failing closed on analytics errors
- Same-class `REQUIRES_NEW`

### Validation
```bash
./mvnw -Dtest=AnalyticsFailureIntegrationTest,UrlShortenerIntegrationTest test
```
Expect HTTP `302` when analytics throws; happy path still increments clicks.

### Risks and remaining limitations
Best-effort counts can under-count under DB outage; production evolution is async event pipeline (see risk register).

---

## 3. Ambiguous — “Make redirects more reliable”

### Requirement
Stakeholder phrasing was ambiguous. Needed clarification before coding.

### Initial codebase state
Brownfield shortener with create/redirect/analytics and DB-backed uniqueness.

### Ambiguities
Possible interpretations:
1. Exact-once durable click accounting.
2. Redirect must succeed even if analytics fails.
3. Lower redirect latency via caching.
4. Stronger client caching (301).

### Impact analysis
Interpretation (2) touches transaction design and error containment. (1) and (3) imply broker/cache — out of assessment MVP. (4) conflicts with correct short-link semantics (302 chosen intentionally).

### Task decomposition
1. Write clarifying questions / assumptions.
2. Choose interpretation with rationale.
3. Compare sync exact vs best-effort vs event-driven.
4. Implement chosen prototype approach.
5. Document production evolution path.

### AI contribution
Compared synchronous exact counting, best-effort, and event-driven designs; initially leaned toward synchronous exactness “for correctness”.

### Engineer decisions
Clarified requirement as: **a valid redirect must succeed even when analytics cannot be recorded.** Chose best-effort for prototype; documented async events as production evolution. Kept HTTP 302 (not 301).

### Files changed
- `AnalyticsWriter` + `UrlService` (availability path)
- `docs/08-scenarios.md` (this section)
- `docs/09-final-summary.md`, `docs/12-production-risk-register.md`

### Rejected options
- Treating “reliable” as mandatory exact counts in-request
- Switching to 301 permanent redirects
- Adding Redis in the assessment MVP solely for this phrase

### Validation
`AnalyticsFailureIntegrationTest` + redirect happy-path IT; narrative validated in demo script talk track #2.

### Risks and remaining limitations
Under-counting; no outbox yet; operators must alert on `shortener_analytics_updates_total{result="failure"}`.
