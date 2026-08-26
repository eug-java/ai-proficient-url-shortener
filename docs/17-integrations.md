# External integrations

This service is designed to plug into other products (billing, CRM, notification workers, edge CDNs).

## Auth for machines

| Mode | Header | Notes |
|---|---|---|
| User JWT | `Authorization: Bearer <access_token>` | Keycloak realm `shortener` |
| Org API key | `Authorization: ApiKey sk_live_...` | Created under `/api/v1/orgs/{orgId}/integrations/api-keys`; scoped to that org as OWNER-equivalent |

API keys are shown once at creation. Store secrets in your vault; revoke via `DELETE .../api-keys/{id}`.

## Outbound webhooks

Register endpoints:

`POST /api/v1/orgs/{orgId}/integrations/webhooks`

```json
{ "targetUrl": "https://hooks.example.com/shortener", "events": "LINK_CREATED,MEMBER_INVITED,*" }
```

Delivery payload (`Integration.Event` outbox → HTTP POST):

```json
{
  "organizationId": "...",
  "action": "LINK_CREATED",
  "entityType": "UrlMapping",
  "entityId": "...",
  "occurredAt": "2026-08-26T12:00:00Z",
  "details": { "shortCode": "abc" }
}
```

Headers:

- `Content-Type: application/json`
- `X-Shortener-Event-Id: <uuid>`
- `X-Shortener-Signature: sha256=<hmac_hex>`

Verify with HMAC-SHA256 of the raw body using the webhook secret (`whsec_...`).

Disable dispatch in tests with `app.webhooks.dispatch-enabled=false`.

## Custom domains

1. `POST /api/v1/orgs/{orgId}/domains` with `{ "hostname": "go.example.com" }`
2. Create TXT `_shortener-verify.go.example.com` = returned token
3. `POST .../domains/{id}/verify`
4. Point the hostname at the shortener; redirects use `Host` to resolve org-scoped codes

Local/dev can set `app.custom-domains.allow-skip-dns-verify=true`.

## Audit log

`GET /api/v1/orgs/{orgId}/audit` (OWNER) returns append-only admin actions that also fan out as integration events.

## OpenAPI

With `local` / `test` profiles Swagger UI is enabled. Security schemes document both Bearer JWT and ApiKey.

## GeoIP in CI

MaxMind GeoLite2 is license-gated. Compose mounts `./deploy/geoip`; CI does not download the DB. Country resolution falls back to `CF-IPCountry` / `X-Country-Code` when the MMDB file is absent.
