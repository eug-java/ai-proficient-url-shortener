# Production API reference

Base URL: `http://localhost:8080`

Auth: `Authorization: Bearer <Keycloak access token>`  
Tests (security disabled): `X-Test-User-Sub: <subject>`

Public (no auth):

| Method | Path | Description |
|---|---|---|
| GET | `/{shortCode}` | 302 redirect; enqueues click analytics |

Authenticated management:

| Method | Path | Roles |
|---|---|---|
| POST | `/api/v1/orgs` | any authenticated → becomes OWNER |
| GET | `/api/v1/orgs` | memberships (includes `role`) |
| GET | `/api/v1/orgs/{orgId}/members` | MEMBER+ |
| POST | `/api/v1/orgs/{orgId}/members` | OWNER (add by known Keycloak `userSub`) |
| POST | `/api/v1/orgs/{orgId}/invites` | OWNER (email invite via Keycloak Admin; creates user if missing) |
| POST | `/api/v1/orgs/{orgId}/transfer-ownership` | OWNER → new OWNER |
| PATCH | `/api/v1/orgs/{orgId}/members/{memberId}` | OWNER (cannot demote last OWNER) |
| POST | `/api/v1/orgs/{orgId}/urls` | OWNER/ADMIN/MEMBER |
| GET | `/api/v1/orgs/{orgId}/urls` | MEMBER+ |
| GET | `/api/v1/orgs/{orgId}/urls/{code}` | MEMBER+ |
| PATCH | `/api/v1/orgs/{orgId}/urls/{code}` | OWNER/ADMIN/MEMBER |
| POST | `/api/v1/orgs/{orgId}/urls/{code}/disable` | OWNER/ADMIN/MEMBER |
| DELETE | `/api/v1/orgs/{orgId}/urls/{code}` | OWNER/ADMIN |
| GET | `/api/v1/orgs/{orgId}/urls/{code}/analytics` | MEMBER+ |
| GET | `.../analytics/timeseries?days=30` | MEMBER+ |
| GET | `.../analytics/breakdowns/{dimension}?days=30` | MEMBER+ (`referrer`,`browser`,`os`,`country`,`device`) |
| GET | `.../analytics/export.csv?days=30` | MEMBER+ |

Legacy unscoped `/api/v1/urls*` remains for demos when security is off; prefer org-scoped paths.

Geo: pass `CF-IPCountry` or `X-Country-Code` on redirect. IP stored only as SHA-256 hash with pepper.
