# Architecture

A modular monolith was selected over microservices to maximize delivery quality and testability within the assignment timebox.

```text
API (controllers, HTTP DTOs, exception mapping)
  -> Application (UrlService, commands/results, writers)
  -> Domain (UrlPolicy, UrlMapping, AliasGenerator, domain exceptions)
  -> Persistence (Spring Data JPA)
  -> PostgreSQL
```

Key choices:
- PostgreSQL unique constraint is authoritative for aliases.
- HTTP DTOs stay in the API package; application uses its own command/result types.
- Domain exceptions are independent of the web layer.
- Random Base62 codes use `SecureRandom`.
- HTTP 302 avoids permanent client caching.
- Analytics is best effort; production evolution is asynchronous events.
- Module boundaries allow later extraction into services.
- Spring profiles separate concerns: `local` (demo), `test` (CI), `prod` (Swagger off, health-only Actuator, required DB env vars).

## Collision-safe write path

`UrlService` validates input and chooses either the custom alias or an alias from `AliasGenerator`. Persistence is delegated to `UrlMappingWriter`, which performs each insert in a `REQUIRES_NEW` transaction.

```text
UrlService
  -> AliasGenerator
  -> UrlMappingWriter (REQUIRES_NEW)
       -> UrlMappingRepository
            -> PostgreSQL UNIQUE(short_code)
```

A unique-constraint violation (`SQLState 23505`) for a custom alias becomes `DuplicateAliasException`. For a generated alias, the service obtains another value and starts a fresh insert transaction. Non-unique integrity failures are not treated as collisions. This avoids retrying inside a transaction already marked rollback-only.
