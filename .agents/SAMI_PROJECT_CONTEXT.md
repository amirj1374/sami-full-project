# SAMI ERP — reusable development context

Last repository inspection: 2026-07-28  
Workspace: `C:\Users\Amir-Js\Documents\sami`

This file is a fast-start snapshot for Codex and developers. It reduces repeated discovery but does not override current repository files. Re-check the named source files whenever a relevant area changes.

## Repository state

| Area | Location | Git state | Current evidence |
|---|---|---|---|
| Workspace | `.` | Full-project Git repository, branch `development` | Tracks shared guidance and repository composition |
| Frontend | `sami-frontend` | Independent Git repository and submodule | Source tree is present and builds |
| Backend | `sami-backend` | Incomplete Git metadata | No source, build descriptor, configuration or migrations |

Important consequences:

- Shared skills and this document are tracked by the full-project repository.
- The frontend is linked through `.gitmodules`; clone the full project with submodules.
- Backend framework, Java version, database, migrations and runtime contract are **unverified** until the real backend repository is restored.
- Do not duplicate shared workspace files inside the frontend repository.

## Frontend technology

Authoritative sources: `sami-frontend/package.json`, `package-lock.json`, `vite.config.ts`, `tsconfig*.json`, `Dockerfile`.

| Technology | Declared | Locked/observed |
|---|---:|---:|
| Package manager | npm | lockfile v3 |
| Container Node.js | `node:22-alpine` | Docker build image |
| Vite Node requirement | — | `^20.19.0 || >=22.12.0` |
| Vue | `^3.5.13` | `3.5.40` |
| TypeScript | `~5.7.3` | `5.7.3` |
| Vite | `^8.0.0` | `8.1.5` |
| Vuetify | `^3.7.0` | `3.12.11` |
| Pinia | `^3.0.1` | `3.0.4` |
| Vue Router | `^4.5.0` | `4.6.4` |
| Axios | `^1.7.9` | `1.18.1` |
| Vue I18n | `^9.14.5` | `9.14.5` |
| VeeValidate | `^4.15.0` | `4.15.1` |
| Zod | `^3.24.1` | `3.25.76` |
| VueUse | `^12.7.0` | `12.8.2` |
| MSW | `^2.15.0` | `2.15.0` |

No project-level `.nvmrc` or `package.json.engines` declaration was found. Use a Node version compatible with Vite's locked requirement; the production Dockerfile currently chooses Node 22.

## Frontend architecture

Primary structure:

- `src/api`: centralized typed Axios clients.
- `src/types`: shared API/domain TypeScript contracts.
- `src/schemas`: Zod and form validation.
- `src/stores`: Pinia authentication and menu state.
- `src/composables`: permissions, errors, formatting, theme and navigation helpers.
- `src/directives`: declarative permission presentation.
- `src/router`: lazy routes and permission metadata.
- `src/components`: shared forms, dialogs and application components.
- `src/components/widgets`: dashboard widget renderers and registry.
- `src/views`: feature pages.
- `src/locales/en.json` and `fa.json`: bilingual content.
- `src/plugins/vuetify.ts` and `src/styles/global.css`: theme/design foundation.
- `src/mocks`: optional MSW development mode.

Verified conventions:

- Vue 3 Composition API with strict TypeScript and `<script setup>`.
- Vuetify components and centralized theme configuration.
- API base URL through one Axios instance.
- Bearer access token plus refresh-token flow.
- Client permission codes and super-admin bypass for presentation/routing.
- Pinia for cross-page state; local refs/computed state inside views/components.
- Backend-driven menu loading.
- Vue Router route metadata for authentication and permissions.
- English/Persian localization with RTL support.
- Standard API envelope represented in `src/types/api.ts`.

Client-side permission checks are not a security boundary; the backend must enforce authorization.

## Frontend API and authentication

- Default API base: `/api`.
- API paths: `/v1/...` below the configured base.
- Development proxy target: `VITE_DEV_API_TARGET`, default `http://localhost:8080`.
- Production nginx proxy: `/api/` → `http://backend:8080/api/`.
- Tokens are stored in browser `localStorage`.
- HTTP 401 triggers a single-flight refresh and request replay.
- Mock mode is optional and not evidence that all real backend APIs exist.

Environment variables:

| Variable | Required | Documented behavior |
|---|---|---|
| `VITE_API_BASE_URL` | No | Defaults to `/api`; baked into production build |
| `VITE_DEV_API_TARGET` | No | Defaults to `http://localhost:8080` |
| `VITE_ENABLE_MOCK_MODE` | No | Enables MSW when set to `true` |

Source: `sami-frontend/.env.example` and `env.d.ts`.

## Build and run

From `sami-frontend`:

```powershell
npm ci
npm run type-check
npm run build
npm run dev
```

Configured scripts:

- `npm run dev`: Vite development server.
- `npm run type-check`: `vue-tsc --noEmit`.
- `npm run build`: type-check followed by production Vite build.
- `npm run preview`: local production-bundle preview.

There are currently no configured frontend lint or automated test scripts.

The last verified production build passed and transformed 878 modules. Re-run it after any source or dependency change.

## Container and deployment context

Frontend:

- Multi-stage Dockerfile: Node 22 build, nginx 1.27 runtime.
- nginx serves the SPA with history fallback.
- Fingerprinted assets receive long-lived cache headers.
- Container health check requests `/`.
- nginx expects a network hostname `backend` on port `8080`.

Missing full-stack infrastructure:

- root `docker-compose.yml`;
- compose override;
- root environment contract;
- backend image/build/startup definition;
- database service and persistence configuration;
- deployment/startup scripts;
- CI/CD and operational documentation.

Do not create these until the backend repository establishes the database, ports, health endpoint and required variables.

## Backend context

Current status: **unavailable / unverified**.

The `sami-backend` directory contains no usable application files. Therefore none of these may be assumed:

- Java, Maven or Spring versions;
- PostgreSQL or Flyway configuration;
- entity, DTO, repository, service or controller patterns;
- authentication and authorization implementation;
- tenant/company/branch scope;
- audit and event infrastructure;
- API envelope or exception behavior;
- migrations, tests, Dockerfile or run commands.

Restore the actual backend repository before backend implementation, contract reconciliation, migrations, compose generation or release validation.

## Repository-scoped skills

Use the most specific skill:

- `sami-project-context`: refresh technical context.
- `sami-architecture-auditor`: read-only architecture analysis.
- `sami-module-builder`: complete cross-stack modules.
- `sami-backend-builder`: backend implementation/review.
- `sami-frontend-builder`: frontend implementation/review.
- `sami-ui-ux-designer`: UX, information architecture and design patterns.
- `sami-migration-guardian`: PostgreSQL/Flyway safety.
- `sami-contract-validator`: backend/frontend contract reconciliation.
- `sami-repository-reconciler`: safe branch comparison and merging.
- `sami-release-gate`: readiness verdict.
- `sami-project-reporter`: evidence-based progress reporting.

## Non-negotiable development rules

- Inspect existing patterns before creating abstractions.
- Preserve architecture and unrelated modules.
- Do not hardcode configurable statuses, types, providers, workflows, permissions or menus.
- Keep backend business rules out of frontend code.
- Enforce sensitive permissions and audit server-side.
- Never edit an applied Flyway migration.
- Keep API DTOs, TypeScript contracts, validation and mocks aligned.
- Add Persian and English localization together and verify RTL/LTR.
- Never claim completion from scaffolding or from checks that did not run.

## Refresh triggers

Update this document when any of these changes:

- a repository is added/restored or its default branch changes;
- runtime or dependency versions change;
- package/build/run/test commands change;
- API base paths, ports, proxy behavior or environment variables change;
- backend architecture becomes available;
- shared security, audit, event, file, report or tenant infrastructure changes;
- design-system, localization or routing conventions change;
- deployment topology changes.

When refreshing, cite current files, replace stale facts, update the inspection date and keep unknowns explicit.
