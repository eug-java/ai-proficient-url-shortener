# Final Engineering Summary

## Plan and rationale

The assessment goal was a **production-oriented URL shortener** that demonstrates engineer-led AI-assisted delivery: clear requirements, deliberate architecture trade-offs, correct concurrency, safe defaults, automated validation, and honest limitations.

Plan:
1. Normalize requirements and out-of-scope items (`docs/01-requirements.md`).
2. Choose a modular monolith with PostgreSQL uniqueness (`docs/02-architecture.md`, ADRs).
3. Implement create / redirect / analytics with Testcontainers evidence.
4. Fix brownfield transaction defect (analytics isolation).
5. Harden observability, security policy, profiles, and CI quality gates.
6. Strengthen AI traceability and scenario evidence for the rubric.

## Artifacts

| Artifact | Location |
|---|---|
| Runnable service | Spring Boot 3.5 / Java 21 |
| Migrations | `src/main/resources/db/migration/` |
| Tests | `src/test/java/...` |
| CI | `.github/workflows/ci.yml` (verify + Trivy + Docker) |
| Docker | multi-stage `Dockerfile`, `docker-compose.yml` |
| Profiles | `application.yml` + `local` / `test` / `prod` |
| Maven Wrapper | `./mvnw` |
| Requirements | `docs/01-requirements.md` |
| Architecture | `docs/02-architecture.md` |
| AI evidence | `docs/04-ai-development-log.md` |
| Testing | `docs/05-testing.md` |
| Security | `docs/06-security-review.md` |
| ADRs | `docs/07-architecture-decisions.md` |
| Scenarios | `docs/08-scenarios.md` |
| Risk register | `docs/12-production-risk-register.md` |
| Demo script | `docs/11-demo-script.md` |
| Verify evidence | `docs/evidence/` (`mvn-verify.log`, Surefire summary, JaCoCo HTML) |

## Production uplift (post-assessment)

The codebase now also includes multi-tenant orgs/roles (Keycloak), Redis/Kafka analytics,
React dashboard, org audit log, custom domains, outbound webhooks, and org API keys for
cross-service integration. See `docs/13-production-scope-lock.md`, `docs/15-api-reference.md`,
and `docs/17-integrations.md`. Early assessment notes that list “auth out of scope” describe
the original interview prototype, not the current production branch.

## Assumptions

- Assessment timebox originally favored a modular monolith over distributed services.
- Multiple short codes may map to the same original URL.
- Aliases are case-sensitive.
- Redirect availability outranks exact in-request click durability for the prototype.
- Auth and rate limiting are intentional omissions (documented), not forgotten requirements.
- Destination fetch is out of scope; host checks are **anti-abuse**, not SSRF prevention.

## Trade-offs

| Decision | Chosen | Rejected | Why |
|---|---|---|---|
| Architecture | Modular monolith | Microservices | Delivery risk / testability |
| Uniqueness | PostgreSQL unique constraint | exists-then-insert | Concurrency correctness |
| Redirect code | 302 | 301 | Avoid permanent caching |
| Analytics | Best-effort + separate TX | Sync exact in redirect TX | Availability |
| Destination checks | Validate all resolved IPs | Fetch URL / ignore private IPs | Abuse reduction without SSRF |
| Docs UI | Enabled on `local` only | Always-on Swagger | Safer defaults for `prod` |

## Validation

Primary gate:

```bash
./mvnw clean verify
```

This runs unit + Testcontainers integration tests and enforces JaCoCo line coverage ≥ 80%.

CI additionally runs Trivy (`CRITICAL`/`HIGH`) and builds the Docker image.

Representative regressions:
- Redirect + analytics happy path
- Analytics failure must not block 302
- Generated alias collision retry
- Alias exhaustion → 500
- Private/ULA/CGNAT/unresolved hosts
- Prometheus create meter name (`shortener_urls_creations_total`)
- Request-id sanitization
- Actuator lockdown on `test` profile

Evidence already captured under `docs/evidence/`: **33 tests passed**, JaCoCo line gate met (**~89%** instructions). Re-run `./mvnw clean verify` after any late change and refresh those files.

## Risks

See `docs/12-production-risk-register.md` for the full register. Highest residual product risks: unauthenticated create/analytics, phishing misuse, and lack of load-test evidence.

Technical debts called out in ADRs:
- DNS checked at create (rebinding residual) — ADR-012
- Analytics versioning strategy — ADR-011

## Limitations

Not implemented (by design for assessment scope):
- Authentication / authorization / ownership
- Rate limiting and malware/reputation scoring
- Redis caching and async analytics broker
- Load/chaos tests and formal SLO measurement
- Kubernetes manifests and managed secret stores
- Dashboards/alert routers (thresholds are documented only)

## AI-assisted process

AI accelerated analysis, alternatives, defect diagnosis, and documentation drafts. The engineer rejected unsafe or unsuitable suggestions (microservices MVP, exists-then-insert, destination fetch, same-bean `REQUIRES_NEW`) and validated accepted changes with automated tests. Full traceability entries: `docs/04-ai-development-log.md`.

## Next steps (production)

1. AuthN/Z + ownership for create/details/analytics.
2. Rate limits and abuse scoring.
3. Async click pipeline + Redis redirect cache.
4. Load test against documented SLO targets.
5. Management network isolation + secret manager.
