# SAMI ERP — reusable development context

Last repository inspection: 2026-08-02
Workspace: `C:\Users\Amir-Js\OneDrive\Documents\sami-full-project`

This file is a fast-start snapshot for Codex and developers. It reduces repeated discovery but does not override current repository files. Re-check the named source files whenever a relevant area changes.

## Repository state

| Area | Location | Git state | Current evidence |
|---|---|---|---|
| Workspace | `.` | Full-project monorepo, branch `development` | Tracks shared guidance plus backend and frontend source |
| Frontend | `sami-frontend` | Monorepo directory imported from former frontend `development` | Source tree is present and builds |
| Backend | `sami-backend` | Monorepo directory imported from former backend `development` | Java/Spring source, migrations, tests and deployment files are present |

Important consequences:

- Shared skills, documentation, backend and frontend are tracked directly by the full-project repository.
- No submodules or separate application repositories are required.
- Use the root `development` branch as the integration source of truth.

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
- `src/stores`: Pinia authentication, menu and authenticated user-experience preference state.
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
- Per-user mobile navigation preferences are stored by the backend; the shell
  always re-filters them through the current permission-filtered module menu.
- PWA install capability is centralized in `usePwa`; initial banner dismissal
  is device-local and separate from permanent install access in Profile/Settings.
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

The Vite development server listens on `7474`; backend development CORS defaults
and examples must use `http://localhost:7474`.

## Build and run

From `sami-frontend`:

```powershell
npm ci
npm test
npm run type-check
npm run build
npm run dev
```

Configured scripts:

- `npm run dev`: Vite development server.
- `npm test`: Node test runner for release, route, localization, permission and
  source-contract checks.
- `npm run type-check`: `vue-tsc --noEmit`.
- `npm run build`: type-check followed by production Vite build.
- `npm run preview`: local production-bundle preview.

There is currently no configured frontend lint script.

The last verified production build passed and transformed 1013 modules. Re-run it after any source or dependency change.

## Backend technology and architecture

Authoritative sources: `sami-backend/pom.xml`, `src/main/resources/application*.yml`, source packages, migrations and Docker files.

- Java 21; Maven 3.9+ documented.
- Spring Boot `3.5.3` parent with Web, Security, Data JPA, Validation and Actuator.
- PostgreSQL 16 in Compose; Hibernate/JPA schema validation with `ddl-auto: validate`.
- Flyway owns schema history under `src/main/resources/db/migration`.
- JWT access/refresh authentication through JJWT `0.12.6`.
- Database-driven permission RBAC enforced by `@PreAuthorize("@authz...")`.
- OpenAPI/Swagger through springdoc `2.8.9`.
- Lombok and Spring Boot test/Spring Security test.
- Standard `ApiResponse<T>` envelope and centralized `ApiException` handling.
- Audited `BaseEntity`, JPA auditing and public service/event patterns.
- New tenant-scoped code resolves ownership through request-scoped
  `common.tenancy.TenantContext`, backed by the authenticated database-loaded
  `SecurityUser`. It fails closed and does not trust client tenant identifiers.
  `TenantDefaults` remains transitional legacy infrastructure and must not be
  propagated.
- Modules are organized below `com.sami.app` by domain, including auth, authz, automation, calendar, communication, CRM, dashboard, data quality, files, inventory, knowledge, licensing, metadata, portal, products, purchasing, sales, scheduling, suppliers and users.
- Inventory owns the canonical tenant-scoped warehouse registry, location balances,
  append-only movements, serial/IMEI state, reservations, transfers, stock counts,
  value reporting and audit. Purchasing receipts/returns and Sales reservations,
  issues and customer returns integrate through `InventoryStockOperations` in the
  caller transaction. `products.stock_quantity` remains a compatibility projection.

Backend runtime:

- API base: `/api/v1`.
- Server port: `8080`.
- Health: `/actuator/health`.
- Swagger: `/swagger-ui.html`.
- Database defaults: PostgreSQL at `localhost:5432/sami`.
- Production secrets must be supplied through environment variables.

Migration range is `V1` through `V37`. `V31` completes Sales, `V32` introduces
Inventory, `V33` completes Purchasing, `V34` completes CRM, and `V35` applies
release hardening. `V36` preserves the transformed Sales module row and its
grants while aligning its registry code with the `sales:*` permission namespace.
`V37` adds tenant/user-scoped application preferences, the staff in-app
notification inbox, and the existing-scheduler definition for opt-in hourly
demo notifications.

## Container and deployment context

Full stack:

- Multi-stage Dockerfile: Node 22 build, nginx 1.27 runtime.
- Backend Dockerfile: Maven 3.9/JDK 21 build and non-root Temurin 21 JRE runtime.
- PostgreSQL `16-alpine`.
- nginx serves the SPA with history fallback.
- Fingerprinted assets receive long-lived cache headers.
- Frontend container health checks request `/`; backend container health checks
  request `/actuator/health`.
- nginx expects a network hostname `backend` on port `8080`.
- `sami-backend/docker-compose.yml` runs PostgreSQL plus the backend for development.
- `sami-backend/docker-compose.prod.yml` builds PostgreSQL, backend and the sibling frontend for production; explicit configurable backend/frontend image names also support verified prebuilt-image deployment without rebuilding on the server.
- `.env.example` documents database, staff/portal JWT, CORS and bootstrap-admin variables.
- `DEMO_HOURLY_NOTIFICATION_ENABLED` is the disabled-by-default deployment kill
  switch for hourly demo inbox messages; each user must additionally opt in.
- Production Compose requires distinct staff and portal JWT secrets and fails fast
  when either is absent.
- Production Compose waits for PostgreSQL and backend health, publishes only the
  frontend port, exposes backend health through nginx at `/health`, and persists
  legacy uploads, managed files and staging in named volumes.
- `scripts/deploy.ps1` is the canonical Windows release orchestrator for guarded
  `linux/amd64` build, image verification, atomic TAR export/upload, SHA256
  validation, backend/frontend-only recreation, health checks, and application-
  image rollback. SSH is key-based and non-interactive by default. See
  `DEPLOYMENT_AUTOMATION_GUIDE.md`; no runtime secret or private key belongs in
  repository configuration.

The production Compose file remains located under `sami-backend`; run it from that directory. Some README/Compose comments still mention the historical V1..V9 range and should not be treated as current migration inventory.

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

- repository composition or its default branch changes;
- runtime or dependency versions change;
- package/build/run/test commands change;
- API base paths, ports, proxy behavior or environment variables change;
- backend architecture becomes available;
- shared security, audit, event, file, report or tenant infrastructure changes;
- design-system, localization or routing conventions change;
- deployment topology changes.

When refreshing, cite current files, replace stale facts, update the inspection date and keep unknowns explicit.
