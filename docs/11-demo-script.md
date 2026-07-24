# Demo Script

## Opening (engineer-led AI-assisted)

1. Explain that AI accelerated analysis and boilerplate, while you owned architecture, security trade-offs, and test evidence.
2. Show the modular layout: API DTOs → application commands/results → domain policy/exceptions → PostgreSQL.

## Talk tracks (use these three bullets)

1. **Uniqueness / retry:** PostgreSQL `UNIQUE(short_code)` is the source of truth; generated aliases retry in `REQUIRES_NEW` transactions after SQLState `23505`.
2. **Analytics isolation:** click updates run in a separate transaction so analytics failure never blocks a valid HTTP `302`.
3. **Observability + honest scope:** request-id correlation, INFO audits without destination URLs, low-cardinality Prometheus meters; auth and rate limiting are intentionally out of scope for the assessment and documented as next production steps.

## Live walkthrough

1. Start with Docker Compose (`SPRING_PROFILES_ACTIVE=local`):

```bash
docker compose up --build
```

2. Show create → follow `Location` → redirect → analytics.
3. Show duplicate alias `409`, expired link `410`, reserved/private host `400`.
4. Show `/actuator/health`, optional `/actuator/prometheus` (local profile), and Swagger UI.
5. Contrast profiles: local enables Swagger/Prometheus for demos; prod disables docs UI and exposes only health.

## Evidence

1. Run `mvn clean verify` (Testcontainers + JaCoCo gate).
2. Point to accepted / modified / rejected AI outcomes in `docs/04-ai-development-log.md`.
3. Close with limitations (`README` production section) and next steps: auth, rate limits, abuse controls.
