# Remaining production work

Updated 2026-08-26 after ordered delivery + ops polish.

## Done
- **P0** compose smoke (API `:8088`, UI `:3001`, Keycloak `:8081`), issuer/JWKS, Kafka pipeline E2E, CSV Bearer, legacy API gated
- **P1 product** quotas, ownership transfer, DNS re-check on redirect, retention purge, dashboard edit/delete/roles, geo resolver hook
- **P1 invite** `POST /api/v1/orgs/{id}/invites` via Keycloak Admin service account (`shortener-admin`); stub directory in tests
- **P1 geo** MaxMind Country DB via `app.geo.maxmind-db` / compose `deploy/geoip` mount; headers still preferred
- **P1 tests** concurrent alias race IT, Vitest slugify+apiFetch, Redis+EmbeddedKafka pipeline IT, mixed multi-A DNS fixture, JWT MockMvc
- **P2 ops** Prometheus alerts, Grafana dashboard JSON, Helm chart starter, k6, `.env.example`, CI tags image by Maven version + dashboard tests

Gate: `./mvnw clean verify` + `cd dashboard && npm test`.

## Deferred (not blocking this PR)
1. Custom vanity domains / multi-host routing
2. Org audit log (immutable member/link admin actions)
3. Assessment-doc refresh for interview artifacts
4. Broader dashboard UI Vitest (auth/org pages with Keycloak mocks)
5. Wire MaxMind DB into CI (license-gated GeoLite2 file)
