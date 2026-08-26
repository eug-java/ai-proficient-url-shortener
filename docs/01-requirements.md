# Requirements (production product)

## Functional
- Authenticated org management (Keycloak OIDC).
- Org roles: OWNER, ADMIN, MEMBER, VIEWER.
- Create / list / update / disable / delete short links within an org.
- Public redirect `GET /{shortCode}` without auth.
- Rich analytics: summary, daily timeseries, breakdowns (referrer, browser, OS, country, device), CSV export.
- Optional custom alias, title, expiration.

## Non-functional
- Duplicate-alias correctness via DB unique constraint.
- Redirect availability over exact in-request analytics (outbox / Kafka).
- Idempotent click consumers (`consumer_inbox`).
- Redis cache + rate limits in prod-like profile.
- Observability: logs with request id, Micrometer/Prometheus.
- Testability: Testcontainers + inline messaging mode.

## Security
- JWT resource server (Keycloak).
- Host anti-abuse policy on create (all resolved IPs).
- Hashed click IPs; no raw IP storage.
- CORS locked to dashboard origins.

## Out of scope (still)
- Multi-region active-active
- Custom vanity domain automation
- Billing
- Third-party malware vendor (hook later)
- Native mobile apps
