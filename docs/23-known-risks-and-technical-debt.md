# Known risks and technical debt

| Priority | Confirmed risk | Evidence summary | Required direction |
|---|---|---|---|
| Critical | Production secrets outside Compose may retain unsafe defaults | base configuration has development fallbacks | profile-wide fail-fast secret policy |
| High | Transitional tenant isolation | `TenantDefaults` is explicitly a bridge | request-bound context and scoped repositories |
| High | Tokens in `localStorage` | frontend token storage | cookie/CSRF security decision |
| High | Scheduler timeout may not stop work | future cancellation only | cooperative cancellation or isolated execution |
| High | No general durable event delivery | Spring process-local events | outbox/broker ADR for critical integrations |
| High | No PostgreSQL/Flyway integration tests | test/dependency inventory | Testcontainers CI suite |
| High | Legacy/new file ownership overlaps | `common.storage` and `files` | migration and persistent-volume plan |
| Medium | Communication/OTP providers absent | zero handler implementations | approved adapters and secret contracts |
| Medium | Frontend lint/test infrastructure absent | package scripts/config inventory | select and add quality toolchain |
| Medium | Observability incomplete | health/info only | metrics, tracing, logging and alert ownership |
| Medium | Dev demo seed defaults on | dev profile | explicit team policy |

The development CORS default was aligned with Vite port `7474` in the current
worktree. Production Compose now requires a distinct portal JWT secret.
