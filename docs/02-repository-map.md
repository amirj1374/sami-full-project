# Repository map

| Path | Ownership and purpose |
|---|---|
| `AGENTS.md` | Highest-priority repository instructions for coding agents |
| `.agents/SAMI_PROJECT_CONTEXT.md` | Refreshable technical snapshot; source files remain authoritative |
| `.agents/skills/` | SAMI-specific audit, build, migration, contract, UX, and release workflows |
| `.github/workflows/ci.yml` | Backend and frontend CI gates |
| `sami-backend/` | Spring Boot application, Flyway migrations, tests, Docker and Compose |
| `sami-frontend/` | Vue SPA, nginx config, Docker build, API clients and UI |
| `docs/` | Canonical technical knowledge base |

## Backend

- `src/main/java/com/sami/app/<domain>`: package-by-domain modules.
- `common`: base entities, exceptions, storage, scheduler, audit/public-service
  infrastructure.
- `security`: filters, principals, authorization helper, JWT integration.
- `src/main/resources/application*.yml`: runtime profiles.
- `src/main/resources/db/migration`: immutable Flyway history.
- `src/test/java`: mostly pure unit tests; no verified PostgreSQL integration suite.

## Frontend

- `src/api`: centralized typed HTTP clients.
- `src/types`, `src/schemas`: transport/domain contracts and validation.
- `src/stores`: authentication and menu state.
- `src/router`: lazy routes and route permission metadata.
- `src/views`: feature screens; presence alone does not prove backend support.
- `src/locales`: paired English/Persian messages.
- `src/mocks`: optional MSW development mocks.

Ignored/generated locations such as `node_modules`, `dist`, and backend `target`
must not be documented as source or committed accidentally.
