# Remaining production work

Updated 2026-08-26 end of ordered pass.

## Done
- **P0** compose smoke (API `:8088`, UI `:3001`, Keycloak `:8081`), issuer/JWKS, Kafka pipeline E2E, CSV Bearer, legacy API gated
- **P1 product** quotas, ownership transfer, DNS re-check on redirect, retention purge, dashboard edit/delete/roles, geo resolver hook
- **P1 invite** `POST /api/v1/orgs/{id}/invites` via Keycloak Admin service account (`shortener-admin`); stub directory in tests
- **P1 geo** MaxMind Country DB via `app.geo.maxmind-db` / compose `deploy/geoip` mount; headers still preferred
- **P1 tests** concurrent alias race IT, Vitest slugify, Redis+EmbeddedKafka pipeline IT (cache/rate-limit/outbox)
- **P2 starter** Prometheus alerts, k6 script, `.env.example`

Gate: `./mvnw clean verify` green (includes race IT + pipeline IT).

## Still open (next ordered slice)
1. Mixed public+private multi-A DNS fixture
2. Security-on JWT MockMvc tests
3. Broader Vitest coverage for auth/org UI
4. Grafana dashboard JSON, Helm/k8s, custom domains, audit log, versioning CI, assessment-doc refresh
