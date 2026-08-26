# AI-Proficient URL Shortener

Production URL shortener (Java 21 / Spring Boot 3.4) with Keycloak auth, org RBAC, Redis, Kafka click pipeline, rich analytics, and a management dashboard.

## Product features

- Organizations + roles (`OWNER` / `ADMIN` / `MEMBER` / `VIEWER`)
- Keycloak OIDC (resource server JWT)
- Org-scoped link lifecycle (create / list / update / disable / delete)
- Public `302` redirect with transactional outbox → Kafka (or inline mode for tests)
- Analytics: summary, timeseries, referrer/UA/geo/device breakdowns, CSV export
- Redis redirect cache + API rate limiting
- Dashboard SPA (`dashboard/`)
- PostgreSQL + Flyway, Actuator, Prometheus, ProblemDetail errors

## Quick start (prod-like Compose)

```bash
docker compose up --build
```

| Service | URL |
|---|---|
| API | http://localhost:8088 (mapped; container listens on 8080) |
| Dashboard | http://localhost:3001 |
| Keycloak | http://localhost:8081 (admin/admin) |
| Demo user | `demo` / `demo` (realm `shortener`) |

> If host ports 8080/3000 are free, change compose mappings back to `8080:8080` / `3000:80` and rebuild the dashboard image.

Docs: `docs/13-production-scope-lock.md`, `docs/15-api-reference.md`, `docs/05-testing.md`.

## Local API without full Compose

```bash
docker compose up -d postgres redis kafka keycloak
./mvnw spring-boot:run -Dspring-boot.run.profiles=prodlike
cd dashboard && npm install && npm run dev
```

## Tests

```bash
./mvnw clean verify
```

---

## API examples

Create a short URL:

```bash
curl -i -X POST http://localhost:8080/api/v1/urls \
  -H 'Content-Type: application/json' \
  -d '{
    "originalUrl": "https://example.com/article",
    "customAlias": "article1",
    "expiresAt": "2027-01-01T00:00:00Z"
  }'
```

Follow the redirect without automatically requesting the destination:

```bash
curl -i http://localhost:8080/article1
```

Fetch the created resource via the `Location` header:

```bash
curl http://localhost:8080/api/v1/urls/article1
```

Read analytics:

```bash
curl http://localhost:8080/api/v1/urls/article1/analytics
```

Example RFC 9457 error:

```json
{
  "type": "urn:problem-type:short-url-not-found",
  "title": "Not Found",
  "status": 404,
  "detail": "Short URL was not found",
  "instance": "/unknown-alias",
  "code": "SHORT_URL_NOT_FOUND"
}
```

## Metrics and Actuator

By default the application exposes only `/actuator/health` (and Kubernetes probes). Prometheus and other management endpoints stay disabled unless explicitly enabled:

```bash
export MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,prometheus
```

Docker Compose sets this for local demos. Custom business metrics:

| Metric | Tags | Meaning |
|---|---|---|
| `shortener_urls_creations_total` | `alias_type=custom\|generated` | Successful creates |
| `shortener_alias_collisions_total` | `alias_type` | Generated-code collisions before retry |
| `shortener_redirect_total` | `result=success\|not_found\|expired` | Redirect outcomes |
| `shortener_redirect_duration_seconds` | `result=...` | Redirect latency histogram |
| `shortener_analytics_updates_total` | `result=success\|failure` | Click counter writes |
| `shortener_errors_total` | `code=...` | Mapped API error codes |

Micrometer meter names avoid reserved Prometheus suffixes such as `.created.total` so scrape names stay predictable.

Inspect them with:

```bash
curl -s http://localhost:8080/actuator/prometheus | grep shortener_
```

Every HTTP response includes `X-Request-Id`. Client-provided values are accepted only when they match `[A-Za-z0-9._-]{1,64}`; otherwise a UUID is generated. Logs include `requestId` in the console pattern. Application INFO logs record create and redirect outcomes with `shortCode` only — never the destination URL.

## Testing

```bash
./mvnw clean verify
```

(If the wrapper is unavailable, `mvn clean verify` with JDK 21 also works.)

The integration tests start a real PostgreSQL container and verify:

- URL creation
- original `302` response and `Location` header
- analytics update in an independent transaction
- RFC 9457 error output

Coverage report: `target/site/jacoco/index.html`

`./mvnw clean verify` also enforces a JaCoCo line-coverage gate (minimum 80%).

Optional local OWASP dependency scan:

```bash
./mvnw org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=9
```

### Evidence to attach before submission

```bash
mkdir -p docs/evidence
./mvnw clean verify | tee docs/evidence/mvn-verify.log
rg "Tests run:" target/surefire-reports || true
cp -R target/site/jacoco docs/evidence/jacoco || true
```

Also capture a screenshot of a green GitHub Actions run when CI is available.

## Continuous integration

`.github/workflows/ci.yml` runs on pushes and pull requests. It:

1. sets up Java 21;
2. runs `./mvnw clean verify` (tests + JaCoCo coverage gate);
3. runs a Trivy filesystem dependency/vulnerability scan (`CRITICAL`/`HIGH`);
4. builds the Docker image.

## Architecture

The implementation is a modular monolith with clear boundaries:

```text
API controllers (HTTP DTOs + mapping)
      |
Application services (commands/results, orchestration)
      |
Domain policy, model, and domain exceptions
      |
Spring Data repository
      |
PostgreSQL
```

The API layer owns request/response records and maps them to application commands/results. Controllers and the exception handler are the only packages that talk HTTP. Domain exceptions (`InvalidRequestException`, `NotFoundException`, `ExpiredException`, `DuplicateAliasException`) are mapped to RFC 9457 `ProblemDetail` responses, including `instance` set from the request URI.

Redirect reads use a read-only transaction. Analytics writes run through a separate Spring bean with `REQUIRES_NEW`, so analytics failure does not prevent a valid redirect.

## AI engineering evidence

See `docs/` for:

- requirements and assumptions;
- architecture and backlog;
- AI interaction log with prompts, decisions, files, and validation gates;
- testing and security reviews;
- architecture decision records;
- greenfield, brownfield, and ambiguous scenarios (decomposition → validation);
- production risk register and evolution diagram;
- final engineering summary;
- reusable prompt pack and demo script.

## Production limitations

This assessment implementation intentionally omits authentication, rate limiting, abuse and malware detection, Redis caching, asynchronous event processing, multi-region deployment, load testing, dashboards, alert rules, and deployment manifests. The documentation explains how these would be added in a production system.

## Alias generation and collision safety

A client may omit `customAlias`. In that case, the application generates a random Base62 code using `SecureRandom`.

```bash
curl -i -X POST http://localhost:8080/api/v1/urls \
  -H 'Content-Type: application/json' \
  -d '{
    "originalUrl": "https://example.com/generated"
  }'
```

Example response fields:

```json
{
  "shortCode": "A7x9Pq2",
  "shortUrl": "http://localhost:8080/A7x9Pq2"
}
```

Collision handling is enforced at two levels:

1. PostgreSQL has a unique constraint on `url_mapping.short_code`.
2. Generated-code inserts are retried up to `app.alias-generation-attempts` times.

Each insert attempt runs in a separate `REQUIRES_NEW` transaction. This is important because a uniqueness violation marks the failed transaction for rollback; retrying in the same transaction would not be reliable.

A duplicate client-provided alias returns:

```text
409 Conflict
code: DUPLICATE_ALIAS
```

Configuration:

```yaml
app:
  short-code-length: 7
  alias-generation-attempts: 5
```

## Input validation and reserved aliases

Original URLs must:

- use only `http` or `https`;
- contain a host;
- be no longer than 2048 characters;
- not contain embedded credentials;
- not target loopback, link-local, site-local, multicast, IPv6 unique-local
  (`fc00::/7`), CGNAT (`100.64.0.0/10`), or metadata hosts
  (for example `localhost`, `127.0.0.1`, `10.0.0.0/8`, `169.254.169.254`,
  `[fc00::1]`).

Custom aliases must:

- contain 3 to 32 characters;
- use only letters, digits, `_`, or `-`;
- not conflict with application routes.

Reserved aliases include:

```text
api
actuator
swagger-ui
swagger-ui.html
v3
favicon.ico
error
```

Expiration must be strictly later than the current UTC time. A link is considered expired when `expiresAt <= now`.

## Additional verification scenarios

The automated suite also verifies:

- a duplicate custom alias returns `409 Conflict`;
- creation without an alias returns a generated seven-character Base62 code;
- a generated-code collision is retried successfully in a new transaction;
- reserved aliases are rejected;
- unsupported URL schemes are rejected;
- private/loopback destination hosts are rejected;
- expiration in the past is rejected;
- `Location` from create is retrievable via `GET /api/v1/urls/{shortCode}`;
- analytics write failure does not block HTTP `302`;
- `/actuator/health` is available;
- `/actuator/prometheus` is not exposed by default;
- `/v3/api-docs` is available;
- `X-Request-Id` is echoed on responses.
