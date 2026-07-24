# Testing

Unit coverage targets URL policy, short-code generation, exact expiration boundaries, exception mapping, and Micrometer meter registration.

Recommended integration coverage with PostgreSQL/Testcontainers:
- create and redirect;
- duplicate alias -> 409;
- expired URL -> 410;
- analytics increment;
- migration/JPA compatibility;
- analytics failure does not block redirect;
- create `Location` is retrievable via `GET /api/v1/urls/{shortCode}`;
- private/loopback destination hosts are rejected;
- `/actuator/prometheus` is not exposed by default in the `test` profile;
- Prometheus scrape name `shortener_urls_creations_total` is asserted when prometheus is enabled;
- exhausted generated-alias retries return HTTP 500;
- `X-Request-Id` is echoed when safe and replaced when unsafe.

Quality gate:

```bash
./mvnw clean verify
```

This runs tests, writes the JaCoCo report, and fails the build when bundle line coverage drops below 80%.

CI additionally runs a Trivy filesystem vulnerability scan for `CRITICAL`/`HIGH` findings.

Remaining gaps: load, chaos, multi-instance concurrency harness, and browser E2E tests.

## Regression: read-only redirect transaction

A Testcontainers integration test reproduces and prevents the transaction-boundary defect in which analytics attempted to update PostgreSQL inside the redirect method's read-only transaction. The test verifies both HTTP 302 and the incremented click counter.

## Regression: analytics failure does not block redirect

`AnalyticsFailureIntegrationTest` stubs `AnalyticsWriter` to throw and asserts that redirect still returns HTTP `302` with the expected `Location`.

## Alias and validation coverage

The test suite includes:

- duplicate custom alias -> HTTP 409;
- creation without `customAlias` -> generated Base62 code;
- deterministic generated-code collision -> retry and successful insert;
- reserved alias rejection;
- non-HTTP/HTTPS URL rejection;
- private/loopback destination host rejection;
- past expiration rejection;
- create `Location` follow-up GET;
- health and OpenAPI endpoint smoke tests;
- default Actuator lockdown for Prometheus;
- request-id echo;
- domain tests for expiration boundary and alias rules.

`RandomAliasCollisionIntegrationTest` replaces the production generator with a deterministic test generator. It first returns an occupied code and then a free code, proving that the retry occurs successfully after a database uniqueness violation.
