# Production Architecture

See also: [13-production-scope-lock.md](13-production-scope-lock.md).

## Components

| Component | Role |
|---|---|
| `app` | Spring Boot 3.4 API + redirect + outbox publisher + Kafka consumer |
| `postgres` | Source of truth: orgs, memberships, links, outbox, click_events, rollups |
| `redis` | Redirect hot cache + rate-limit counters |
| `kafka` | Durable click stream between redirect path and analytics writers |
| `keycloak` | OIDC IdP (realm `shortener`) |
| `dashboard` | SPA management UI (Vite/React) |

## Request paths

### Public
- `GET /{shortCode}` → cache miss → Postgres → 302; enqueue click via **transactional outbox**

### Authenticated (`Authorization: Bearer <access_token>`)
- Org lifecycle and membership
- Link CRUD under `/api/v1/orgs/{orgId}/urls`
- Analytics under `/api/v1/orgs/{orgId}/urls/{code}/analytics*`

## Messaging & idempotency

1. Redirect TX writes `outbox_event` (same TX as optional cache invalidation metadata).
2. `OutboxPublisher` polls/`FOR UPDATE SKIP LOCKED` and publishes to Kafka topic `shortener.clicks.v1`.
3. Message key = `shortCode`; headers include `eventId` (UUID).
4. Consumer writes `consumer_inbox(event_id)` unique; on conflict → skip (at-least-once safe).
5. Inserts `click_event` and updates rollup tables (`link_stats_daily`, `link_stats_total`).

## Caching

- Redis key `redirect:{shortCode}` → `{originalUrl, expiresAt, orgId, status}` TTL short (e.g. 60s) + explicit invalidate on update/disable.
- Negative caching optional later.

## Rate limiting

- Redis token bucket / fixed window:
  - per authenticated principal on mutating APIs
  - per IP on public redirect (soft) and unauthenticated probes

## Security

- Spring Security OAuth2 Resource Server validates JWT from Keycloak.
- Org RBAC resolved from `organization_member` using JWT `sub`.
- Actuator remains health-focused in prod; management endpoints not public.

## API evolution

Current `/api/v1` is the production contract after this uplift (breaking vs assessment API is intentional). Future breaks → `/api/v2`.
