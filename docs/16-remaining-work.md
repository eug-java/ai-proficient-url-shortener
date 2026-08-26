# Remaining production work

Updated after integrations polish (audit, domains, webhooks, API keys).

## Done (this stream)
- Org append-only **audit log** + fan-out to integration outbox
- **Custom vanity domains** with TXT verify + Host-based redirect scoping
- **Outbound webhooks** (HMAC) + **org API keys** for machine callers
- OpenAPI security schemes; `docs/17-integrations.md`
- Dashboard Vitest helpers for org/auth display selection
- Assessment summary note that production multi-tenant stack supersedes early “auth out of scope” narrative

## Deferred / optional
1. Broader React Testing Library coverage of Keycloak-backed pages (requires harness)
2. Commit a licensed GeoLite2 MMDB into private CI secrets
3. Full CDN/custom-domain DNS automation (ACM/Route53 style)
4. Multi-region active-active and per-tenant Kafka topics
