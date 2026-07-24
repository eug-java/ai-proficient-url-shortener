# Backlog

| ID | Task | Dependency | Acceptance criteria | AI role |
|---|---|---|---|---|
| T1 | Requirements | None | Scope and ambiguities documented | Draft/challenge |
| T2 | Architecture | T1 | Boundaries and trade-offs | Alternatives |
| T3 | API/schema | T2 | Status codes, migration, constraints | Review |
| T4 | Domain validation | T3 | URL, alias, expiration rules | Test discovery |
| T5 | Persistence | T3 | Flyway and uniqueness | Concurrency review |
| T6 | Creation/redirect | T4,T5 | 201,302,404,410,409 | Draft/review |
| T7 | Analytics | T6 | Count and failure boundary | Compare options |
| T8 | Tests | T6,T7 | Unit/integration/negative | Test matrix |
| T9 | Security/observability | T6 | Controls and metrics | Review |
| T10 | Documentation | All | Defensible evidence pack | Draft/edit |
