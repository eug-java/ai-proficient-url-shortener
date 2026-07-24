# Production Risk Register & Evolution

This assessment prototype is **production-oriented in engineering discipline**, not a full production SaaS. Risks below are acknowledged deliberately.

## Risk register

| ID | Risk | Likelihood | Impact | Current mitigation | Production evolution |
|---|---|---|---|---|---|
| R1 | Unauthenticated create / analytics read | High | High | Documented out of scope; local/prod profile separation | AuthN/Z + link ownership |
| R2 | Alias spam / abuse | High | Medium | Input validation; collision retries capped | Rate limiting, CAPTCHA, quotas |
| R3 | Phishing via shortened links | High | High | Scheme allowlist; private/ULA/CGNAT host anti-abuse policy | Reputation/malware scoring, reporting, blocklists |
| R4 | DNS rebinding after create | Medium | Medium | Resolve **all** addresses at create; fail closed on NXDOMAIN; documented residual | Re-validate at redirect or pin resolved IPs |
| R5 | Analytics under-count | Medium | Low | Best-effort writer; failure metrics/logs | Outbox / queue consumers |
| R6 | Actuator / Swagger exposure | Medium | Medium | `prod` disables Swagger; Actuator health-only by default | Network policy + auth on management port |
| R7 | Expired link retention growth | Medium | Medium | Partial index on `expires_at` | Scheduled cleanup / TTL jobs |
| R8 | Dependency vulnerabilities | Medium | High | Trivy in CI; optional OWASP plugin | Continuous SCA + patch SLAs |
| R9 | No load evidence | High | Medium | Documented limitation | k6/Gatling + capacity model |
| R10 | Stale entity vs analytics (historical) | Low | Medium | Analytics JPQL bumps `version` (ADR-011) | Optional separate click store |

## Production evolution diagram

```text
Client
  │
  ├─ POST /api/v1/urls ──► API (authz) ──► App ──► PostgreSQL
  │                              │
  │                              └─► rate limiter / abuse scoring
  │
  └─ GET /{code} ──► API ──► Redis cache? ──► App read
                              │
                              ├─ 302 Location
                              └─ async click event ──► queue ──► analytics workers
                                                         │
                                                         └─ metrics / alerts / dashboards
```

## Documented SLOs (targets — not load-tested)

| Path | Target | Signal |
|---|---|---|
| Redirect success latency | p95 &lt; 100ms (app+DB, warm) | `shortener_redirect_duration_seconds` |
| Redirect availability | ≥ 99.9% excluding client errors | `shortener_redirect_total{result="success"}` vs 5xx |
| Analytics pipeline (prod) | ≥ 99% eventual click durability | queue lag + DLQ |

These are **design targets**. No load-test results are claimed for this assessment.

## Alert thresholds (suggested)

| Alert | Condition |
|---|---|
| Redirect 5xx | `rate(shortener_errors_total{code="INTERNAL_ERROR"}[5m]) > 0.1` |
| Analytics failure spike | `rate(shortener_analytics_updates_total{result="failure"}[5m]) > 1` |
| Alias exhaustion | `rate(shortener_errors_total{code="INTERNAL_ERROR"}[5m])` after create storms |
| Health down | `up{job="url-shortener"} == 0` |
