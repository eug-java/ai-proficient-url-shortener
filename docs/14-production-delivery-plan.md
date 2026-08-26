# Production uplift plan (locked)

Approved answers mapped to delivery:

1. Auth = Keycloak OIDC
2. Tenancy = organizations + roles
3. Analytics = full (summary, timeseries, UA/referrer/geo breakdowns, CSV)
4. Data plane = Postgres + Redis + Kafka (outbox + idempotent consumers)
5. UI = dashboard SPA (`dashboard/`)
6. Runtime = docker-compose prod-like
7. Scope = full P0–P2
8. API = break now on `/api/v1`; additive/`v2` later

## Workstreams

| Stream | Status |
|---|---|
| Scope lock + architecture docs | Done |
| Compose (postgres/redis/kafka/keycloak/dashboard) | Done |
| Flyway V2 schema | Done |
| Dashboard SPA scaffold | Done |
| Backend security/orgs/links/analytics/messaging | In progress (agent) |
| Tests green + README runbook | Pending |

## Local entrypoints (target)

```bash
docker compose up -d postgres redis kafka keycloak
./mvnw spring-boot:run -Dspring-boot.run.profiles=prodlike
# dashboard
cd dashboard && npm install && npm run dev
```

Or full stack: `docker compose up --build`.
