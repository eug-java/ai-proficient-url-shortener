# Production Scope Lock

Status: **approved** (2026-08-26). Assessment assumptions are retired.

## Locked decisions

| Topic | Decision |
|---|---|
| Identity | **Keycloak** (OIDC / OAuth2 resource server) |
| Tenancy | **Organizations + roles** (`OWNER`, `ADMIN`, `MEMBER`, `VIEWER`) |
| Analytics | **Maximum**: totals, time series, UA, referrer, geo, CSV export |
| Data plane | **PostgreSQL** (source of truth) + **Redis** (cache, rate limits) + **Kafka** (click stream) |
| Messaging pattern | Transactional **outbox** → Kafka → idempotent consumers |
| UI | Functional **dashboard** (SPA) |
| Runtime | **docker-compose prod-like** (app, postgres, redis, kafka, keycloak, dashboard) |
| Scope | Full P0–P2 production uplift |
| API versioning | **Break now** on `/api/v1`; design for **backward-compatible** evolution afterward (`/api/v2` when needed, additive changes preferred) |

## Non-goals (still deferred)

- Multi-region active-active
- Custom vanity domains DNS automation
- Billing / metering UI
- Malware vendor integration (hook interface only)
- Mobile native apps

## Target architecture (logical)

```text
Browser / SDK
   │
   ├─ OIDC (Keycloak) ── JWT ──► API (Spring Resource Server)
   │                                │
   │                                ├─ RBAC (org membership)
   │                                ├─ Redis rate limit / redirect cache
   │                                └─ PostgreSQL (orgs, links, outbox, aggregates)
   │
   └─ GET /{code} (public) ──► resolve ──► 302
                                  │
                                  └─ outbox click ──► Kafka ──► analytics worker
                                                         │
                                                         └─ click_events + rollups
```

## Role matrix

| Action | OWNER | ADMIN | MEMBER | VIEWER |
|---|---|---|---|---|
| Manage org / members | ✓ | — | — | — |
| Create / edit / disable links | ✓ | ✓ | ✓ | — |
| Delete links | ✓ | ✓ | — | — |
| Read links + analytics + export | ✓ | ✓ | ✓ | ✓ |
| Public redirect | anyone (no auth) | | | |

## Delivery slices

1. Infra compose + Keycloak realm bootstrap  
2. Schema (orgs, membership, owned links, events, outbox, idempotency)  
3. Security filter chain + org RBAC  
4. Link lifecycle APIs (create/list/get/update/disable/delete)  
5. Redirect + outbox + Kafka consumer + Redis cache  
6. Rich analytics API + CSV  
7. Dashboard SPA  
8. Rate limits, cleanup jobs, hardening, tests  

## Compatibility policy (from now on)

- Additive fields and new endpoints are preferred.
- Breaking changes require `/api/vN+1` and a deprecation window.
- Public redirect path `GET /{shortCode}` remains stable.

## Geo attribution

The redirect edge may supply an ISO 3166-1 alpha-2 country in `CF-IPCountry` (preferred)
or `X-Country-Code`. The application does not perform IP geolocation itself; absent or
invalid headers are stored as unknown. Client IPs are stored only as peppered SHA-256 hashes.
