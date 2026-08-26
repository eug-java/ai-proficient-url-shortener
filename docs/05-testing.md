# Testing (production uplift)

## Profiles

| Profile | Security | Messaging | Cache / rate limit |
|---|---|---|---|
| `test` | off (`X-Test-User-Sub`) | `inline` (no Kafka) | off, Redis/Kafka autoconfig excluded |
| `local` / `prodlike` | Keycloak JWT | Kafka outbox | Redis |

## Quality gate

```bash
./mvnw clean verify
```

## Coverage expectations

- Domain / policy unit tests
- Legacy happy-path + negative ITs (still valid with security off)
- **Org product IT**: create org → create link → redirect → inline analytics → RBAC denial
- Analytics failure IT (redirect must not fail)
- Collision / exhaustion ITs

## Manual product check

```bash
docker compose up -d postgres redis kafka keycloak
./mvnw spring-boot:run -Dspring-boot.run.profiles=prodlike
cd dashboard && npm install && npm run dev
```

Login: Keycloak user `demo` / `demo` (realm `shortener`).
