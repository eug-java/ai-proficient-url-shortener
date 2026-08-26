# Remaining production work

Updated 2026-08-26 end of ordered pass.

## Done
- **P0** compose smoke (API `:8088`, UI `:3001`, Keycloak `:8081`), issuer/JWKS, Kafka pipeline E2E, CSV Bearer, legacy API gated
- **P1 product** quotas, ownership transfer, DNS re-check on redirect, retention purge, dashboard edit/delete/roles, geo resolver hook
- **P1 tests** concurrent alias race IT, Vitest slugify; deps added for Kafka/Redis Testcontainers + spring-security-test
- **P2 starter** Prometheus alerts, k6 script, `.env.example`

Gate: `./mvnw clean verify` green (includes race IT).

## Still open (next ordered slice)
1. Invite-by-email via Keycloak Admin API
2. MaxMind GeoIP file wiring (`app.geo.maxmind-db`)
3. Redis + Kafka Testcontainers IT (exercise cache/rate-limit/outbox; drop JaCoCo excludes)
4. Mixed public+private multi-A DNS fixture
5. Security-on JWT MockMvc tests
6. Broader Vitest coverage for auth/org UI
7. Grafana dashboard JSON, Helm/k8s, custom domains, audit log, versioning CI, assessment-doc refresh
