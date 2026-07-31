# SAMI ERP Full-Stack Coverage and Test Report

**Audit date:** 2026-07-31
**Branch:** `development`
**Revision:** `dc97b764ffc67cb7cb6b3c887f5a8de8bc3d1c66`
**Verdict:** **FAIL — substantial backend capabilities have no production frontend, and release-critical backend/runtime verification was unavailable**

## 1. Executive summary

The repository contains 29 production Spring controllers exposing 378 mapped
operations and 22 Vue views. Authentication, RBAC administration, users,
products, customers, suppliers, purchasing, dashboards, automation, and the
master scheduler have meaningful executable frontend paths. This does not mean
that all operations in those modules are surfaced: configuration mutation APIs
and several advanced operations remain absent from their UIs.

Data Quality, managed Files, Knowledge/SOP, Licensing, Metadata/Dynamic Forms,
and Appointments are verified backend-only modules. Their production APIs exist,
but no typed client, static route, or usable page exists. Files, Knowledge,
Licensing, Metadata, and Appointments have production menu paths that currently
fall through the dynamic `PlaceholderView` mechanism. Communication, Portal,
Calendar, and Organization do not expose complete standalone management
contracts and are classified separately from missing frontend work.

Frontend type-check, production build, localization parity, Compose
interpolation/configuration, and whitespace validation passed. Java and Maven
were unavailable on the host and Docker Desktop's Linux daemon was stopped, so
backend compilation/tests, Flyway/PostgreSQL validation, container builds, and
real integration smoke tests could not run. This is release-critical; skipped
checks are not passes.

The existing uncommitted frontend work was preserved. One unambiguous low-risk
shell-title defect was fixed for the completed Automation and Scheduler routes.

## 2. Repository state

- `development` equals `origin/development` (`0` ahead, `0` behind).
- HEAD: `dc97b764ffc67cb7cb6b3c887f5a8de8bc3d1c66`.
- Worktree was dirty before this audit with the mobile/authentication redesign,
  Automation, Scheduler, and untracked Docker export artifacts.
- `deployment-artifacts/` was not read, changed, staged, or removed.
- No commit or push was performed.
- Application source is a monorepo: `sami-backend/` and `sami-frontend/` are
  tracked directories, not submodules.

## 3. Commands executed and results

| Check | Result | Evidence |
|---|---|---|
| Branch, revision, upstream, status | PASS | `git branch --show-current`, `git rev-parse HEAD`, `git rev-list --left-right --count` |
| Frontend type-check | PASS | `npm.cmd run type-check` |
| Frontend production build | PASS | `npm.cmd run build`; Vite 8.1.5, 916 modules |
| English/Persian key parity | PASS | 872 leaf keys in each locale before the report-only audit fix |
| Whitespace/conflict check | PASS | `git diff --check` |
| Production Compose parsing | PASS | `docker compose ... config --quiet` with temporary non-secret placeholder environment values |
| Java version / Maven | UNAVAILABLE | neither `java` nor `mvn` exists on PATH; bundled workspace runtime has no JDK/Maven |
| Backend compile/test/verify | UNAVAILABLE | no Java/Maven runtime |
| Docker daemon | UNAVAILABLE | Docker client 29.6.2 exists; Linux daemon pipe was unavailable |
| Container image builds | UNAVAILABLE | daemon stopped |
| PostgreSQL/Flyway runtime validation | UNAVAILABLE | daemon stopped; no local PostgreSQL test runtime |
| Full-stack runtime smoke | UNAVAILABLE | backend/database could not start |
| Frontend lint | UNAVAILABLE | no lint script/configuration |
| Frontend unit/component/E2E tests | UNAVAILABLE | no test script/framework |

The production Compose file correctly fails interpolation when required secrets
are absent. Supplying temporary audit placeholders allowed a read-only
configuration parse; those values were process-local and were not written.

## 4. Backend → frontend coverage matrix

Endpoint counts are controller method counts from executable controller source.
“CRUD” records usable UI coverage, not merely the existence of a client method.

| Module / controller | Endpoints | Client / route / page | CRUD and special actions | Classification | Exact missing work |
|---|---:|---|---|---|---|
| Authentication — `AuthController` | 7 | `api/auth.ts`; four guest routes; change-password dialog | Login, registration, refresh, logout, forgot/reset/change password | **COMPLETE** | Real refresh/logout runtime test unavailable |
| Menu — `MenuController` | 1 | `api/menu.ts`; Pinia menu store | Permission-filtered retrieval and dynamic registration | **COMPLETE** | Production menu runtime unavailable |
| Modules — `ModuleController` | 7 | client + `/modules` | CRUD, enabled status, lifecycle | **COMPLETE** | None found statically |
| Permissions — `PermissionController` | 5 | client + `/permissions` | CRUD and grouping | **COMPLETE** | None found statically |
| Roles — `RoleController` | 7 | client + `/roles` | CRUD, duplicate, permission assignment | **COMPLETE** | None found statically |
| Users — `UserController` | 19 | client + `/users` | CRUD, status, archive/restore/purge, bulk actions, audit, export, avatar | **COMPLETE** | Runtime upload/download not verified |
| Profile fields — `ProfileFieldController` | 4 | client; consumed by user forms | Read and definition CRUD client | **PARTIAL** | No reachable definition-administration UI |
| User statuses — `UserStatusController` | 4 | client; consumed by user UI | Read and status CRUD client | **PARTIAL** | No reachable status-administration UI |
| Products — `ProductController` | 5 | client + `/products` | CRUD | **COMPLETE** | Mobile table interactions only partially verified |
| Customers — `CustomerController` | 17 | client + `/customers` | lifecycle, duplicate check/merge, notes, timeline, avatar, export | **COMPLETE** | Runtime import/upload and error mapping unverified |
| Customer extensions — `CustomerExtensionsController` | 10 | customer client/profile UI | relations, preferences, blacklist, import | **COMPLETE** | Runtime multipart path unverified |
| CRM configuration — two controllers | 28 | `api/crmConfig.ts`; operational forms consume lookups | Read catalogs; only some mutations exposed | **PARTIAL** | Dedicated type/status/source/tag/relation/preference configuration UI |
| Suppliers — `SupplierController` | 16 | client + `/suppliers` | lifecycle, ratings, documents, logs, export | **COMPLETE** | Runtime file transfer unverified |
| Supplier configuration — `SupplierConfigController` | 21 | supplier client consumes catalogs | Read lookups only in production UI | **PARTIAL** | Type/category/tag/payment-term/rating-criterion management |
| Purchases — `PurchaseController` | 18 | client + `/purchases` | CRUD, submit/approve/reject/cancel/receive/return, attachments, logs | **COMPLETE** | Runtime lifecycle and multipart testing unavailable |
| Purchasing configuration — `PurchasingConfigController` | 12 | purchase client consumes catalogs | Reads types/statuses/reasons/warehouses/rules | **PARTIAL** | Type and approval-rule management UI |
| Dashboards — `DashboardController` | 17 | client + list/viewer/report routes | CRUD, refresh, share, favorite, import/export, filters, audit | **COMPLETE** | Runtime provider execution unavailable |
| KPIs — `KpiController` | 9 | dashboard client + `/kpis` | CRUD, validate, calculate, history, export | **COMPLETE** | None found statically |
| Widgets — `WidgetController` | 5 | dashboard client/components | CRUD, layout, data | **COMPLETE** | None found statically |
| Dashboard configuration — `DashboardConfigController` | 19 | dashboard client consumes catalogs | Read catalogs only | **PARTIAL** | Administration for mutable configuration catalogs |
| Automation — `AutomationController` | 12 | new typed client + `/automations` | CRUD, status, run, history/logs, plugin catalogs | **COMPLETE** | Browser/API runtime unavailable; action JSON editor validation is limited |
| Scheduler — `SchedulerController` | 11 | new typed client + `/scheduler` | CRUD, status, run, per-job history, catalogs | **PARTIAL** | Global `GET /scheduler/executions` has a client but no visible UI |
| Data Quality — `QualityController` | 14 | none | None | **MISSING** | Dashboard, rules/configuration, validation, duplicate check, issues, resolution/corrections, trends |
| Files — `FileController` | 26 | none; menu path `/files` | None | **MISSING** | Browser/folders, upload/download, versions, metadata, restore/copy, references, tags, retention, reports |
| Knowledge/SOP — `KnowledgeController` | 26 | none; menu path `/knowledge` | None | **MISSING** | Library/editor, versions, submit/publish/deprecate/archive, approvals, SOP, relations, reports |
| Licensing — `LicensingController` | 28 | none; menu path `/licensing` | None | **MISSING** | Editions/licenses/features/flags/limits/activation/report administration |
| Metadata — `MetadataController` | 19 | none; menu path `/metadata` | None | **MISSING** | Definition/entity/field/form/assignment UI |
| Appointments — `SchedulingController` | 11 | none; menu path `/appointments` | None | **MISSING** | Calendar/list, create/reschedule/cancel/check-in/out/no-show, availability, waiting list |
| Calendar | no standalone controller | none | Backend support services | **SUPPORTING API — NO STANDALONE PAGE REQUIRED** | Add UI only through a consuming workflow |
| Communication | no production controller | none | Durable backend foundation only | **BLOCKED BY INCOMPLETE BACKEND CONTRACT** | Public API/provider contract required first |
| Portal | no complete public controller | none | Domain/security foundation | **BLOCKED BY INCOMPLETE BACKEND CONTRACT** | Public authentication/workflow contracts |
| Organization administration | no owning management controller | none | Schema/shared mappings | **BLOCKED BY INCOMPLETE BACKEND CONTRACT** | Scope/ownership and management API |

## 5. Frontend → backend connection matrix

| Frontend surface | Backend evidence | Classification | Notes |
|---|---|---|---|
| Login/register/forgot/reset/change password | Auth and users endpoints | **FULLY CONNECTED** | Shared envelope and refresh interceptor align statically |
| Dashboard home | products/customers/suppliers/purchases list endpoints | **PARTIALLY CONNECTED** | Route has no module permission; users missing any required read permission receive aggregate failures |
| Dashboard administration/viewer/reports/KPIs | dashboard/KPI/widget/config controllers | **FULLY CONNECTED** | Operation-level permissions are present |
| Products | product controller | **FULLY CONNECTED** | Five CRUD operations align |
| Customers | customer, extension, and CRM config controllers | **FULLY CONNECTED** for customer workflows | Configuration administration remains absent |
| Suppliers | supplier and supplier-config controllers | **FULLY CONNECTED** for supplier workflows | Configuration mutation APIs remain absent |
| Purchases | purchase and purchasing-config controllers | **FULLY CONNECTED** for operational workflow | Configuration mutation APIs remain absent |
| Users/roles/permissions/modules | matching controllers | **FULLY CONNECTED** | Route and action permissions align statically |
| Automation | automation controller | **FULLY CONNECTED** | Current worktree implementation; runtime not exercised |
| Scheduler | scheduler controller | **PARTIALLY CONNECTED** | Global execution client is dead/unreachable from UI |
| `PlaceholderView` dynamic modules | menu lifecycle metadata only | **DEAD / UNREACHABLE AS FEATURE UI** | Correctly communicates lifecycle, but is not implementation |
| Mock menu | MSW/mock data | **MOCK-ONLY** | Knowledge and Portal entries intentionally demonstrate placeholders |

No production page was found whose core feature has no backend owner. The
principal frontend-only surfaces are lifecycle placeholders and mock-mode data.

## 6. Contract and type findings

### HIGH — Dashboard home permission aggregation

- **Module:** Dashboard
- **Frontend:** `sami-frontend/src/router/index.ts` route `/`;
  `sami-frontend/src/views/DashboardView.vue`
- **Backend:** product, customer, supplier, and purchase list endpoints each
  require separate `*:view` permissions.
- **Conflict:** the route requires authentication only, while loading four
  separately protected APIs in one `Promise.all`.
- **Impact:** a valid restricted user can enter the page but receive a global
  error because one missing permission rejects the entire dashboard load.
- **Recommendation:** permission-filter individual metrics or use a dedicated
  backend dashboard-summary contract.
- **Size:** M.

### MEDIUM — Scheduler global executions client has no consumer

- **Frontend:** `sami-frontend/src/api/scheduler.ts`
- **Backend:** `GET /api/v1/scheduler/executions`
- **Conflict:** typed and correct client operation exists, but no page/component
  calls it.
- **Impact:** administrators cannot review executions across jobs.
- **Recommendation:** add an “All executions” tab using the existing endpoint.
- **Size:** S.

### MEDIUM — Automation action configuration validation is incomplete

- **Frontend:** `sami-frontend/src/views/AutomationsView.vue`
- **Backend:** `AutomationDtos.ActionRequest.config` is an object map.
- **Conflict:** invalid action JSON is caught and ignored during field editing;
  the last valid value may be submitted without explaining the rejected text.
- **Impact:** operator may believe edited configuration was saved.
- **Recommendation:** retain action JSON text state and block submit with a
  localized field error.
- **Size:** S.

### MEDIUM — Frontend schemas do not cover every mutation

Automation has a Zod schema for core fields; Scheduler performs manual required
and JSON validation. Several established CRUD dialogs also use mixed schema and
manual validation. Backend Jakarta limits are therefore not mechanically
reconciled for every field. No confirmed field-name mismatch was found by the
static audit, but full limit/nullability proof remains incomplete without an
OpenAPI-generated comparison or contract tests.

### Confirmed common contracts

- API base `/api`, endpoint namespace `/v1`, and backend `/api/v1` align.
- `ApiResponse<T>` and `PageResponse<T>` shapes align.
- Frontend page indexes are converted to zero-based backend indexes.
- Dates are represented as ISO strings for Java `Instant`.
- Multipart calls use `FormData` and blob responses where the UI exposes files.
- Automation optimistic status updates send backend `expectedVersion`.
- Scheduler create/update/status payload names match `SchedulerDtos`.

## 7. Route, menu, permission, and localization audit

### Static routes

All 19 named leaf/static screens resolved during the mock-auth browser audit;
four guest routes and the catch-all also resolved. Route names are unique.

### Placeholder/menu mismatches

The following production menu paths lack static routes and therefore use
`PlaceholderView` when returned by `/v1/menu`:

- `/files`
- `/knowledge`
- `/licensing`
- `/metadata`
- `/appointments`

Data Quality is more severe: it has neither a static route nor an identified
production menu path in the inspected migration evidence.

`/portal` is present in mock navigation only and correctly represents an
incomplete backend/frontend lifecycle rather than a working feature.

### Permission behavior

- Static protected routes use backend permission codes.
- Automation and Scheduler action controls use their exact controller codes.
- Existing CRUD pages use `v-can`/`usePermission`; backend remains authoritative.
- Dynamic routes derive `${moduleCode}:view`. This is safe only when the module
  code and permission prefix are identical. Purchasing demonstrates that route
  path, module code, and permission prefix can differ; explicit static routes
  remain necessary.
- Real insufficient-role testing was unavailable because the backend could not
  start.

### Localization

- English/Persian key parity passed: 872 leaf keys each.
- Automation and Scheduler page strings exist in both languages.
- One low-risk defect was fixed: `DefaultLayout` now maps the `automations` and
  `scheduler` route names to localized page titles when no matching menu item is
  loaded.
- Placeholder modules use backend names and lifecycle localization.

## 8. Mobile responsiveness

The browser audit used the existing mock authentication/menu strategy at
360, 375, 390, 412, 430, 768, and 1366 px. No tested route shell produced
document-level horizontal overflow. This proves only outer-layout containment;
it does not prove every dialog, table state, mutation, keyboard interaction, or
backend-populated dataset.

| Pages | Classification | Evidence / limitation |
|---|---|---|
| Login, register, forgot/reset password | **VERIFIED MOBILE-READY** | Previously interaction-tested at 360/375/430, touch controls, password toggle, Persian RTL validation, no overflow |
| Dashboard home | **VERIFIED MOBILE-READY for loaded mock state** | Responsive KPI grid and mobile purchase cards; no page overflow |
| Automation, Scheduler | **PARTIAL MOBILE SUPPORT** | Card-first layout/fullscreen dialogs and no shell overflow; real API content and all dialogs unavailable |
| Dashboards, reports, KPI, products, customers, suppliers, purchases, users, roles, modules | **PARTIAL MOBILE SUPPORT** | Shell containment passed; dense tables/forms/actions not exhaustively exercised |
| Permissions | **NOT MOBILE-READY** | Permission matrix intentionally contains horizontal scrolling |
| Files, Knowledge, Licensing, Metadata, Appointments | **NOT TESTED — no production page** | Placeholder/404 behavior is not feature UI |
| Data Quality | **NOT TESTED — no route/page** | Backend only |

Loading/error/empty states are strongest in the established CRUD pages and the
new Automation/Scheduler pages. Configuration-management surfaces and all
missing modules necessarily have no frontend states.

## 9. Docker and deployment consistency

- Production services are `db`, `backend`, and `frontend`.
- Compose builds backend from `sami-backend/` and frontend from the sibling
  `sami-frontend/`; tags are not hardcoded.
- Frontend `VITE_API_BASE_URL` defaults to `/api`.
- nginx serves the SPA and proxies the same-origin API to backend port 8080.
- Backend and frontend Dockerfiles use official Maven/Temurin, Node, and nginx
  images with Linux runtime stages.
- Production Compose requires database, staff JWT, portal JWT, and bootstrap
  password secrets and parsed successfully with temporary placeholders.
- Compose is build-oriented: it contains `build:` blocks and no `image:` names.
  A prebuilt-image VPS deployment therefore needs an override file or explicit
  image configuration; using this file alone will attempt local builds.
- Images were not built because the Docker daemon was unavailable.

## 10. Missing automated quality gates

- No frontend lint command.
- No frontend unit, component, accessibility, visual-regression, or E2E tests.
- No automated TypeScript ↔ OpenAPI DTO drift check.
- Backend has tests, but no host JDK/Maven was available to execute them.
- Existing documentation records the absence of PostgreSQL/Testcontainers
  Flyway integration coverage.
- Browser mock mode validates rendering but cannot prove real API integration,
  permissions, token refresh, transaction behavior, or persistence.

## 11. Prioritized remediation backlog

| Severity | Module | Evidence / impact | Recommended action | Size |
|---|---|---|---|---|
| **CRITICAL** | Release verification | Backend build/tests/Flyway/runtime could not run | Provide Java 21 + Maven or start Docker Desktop and run isolated full-stack verification | M |
| **HIGH** | Files | 26 endpoints; `/files` placeholder | Implement production file browser and lifecycle UI | XL |
| **HIGH** | Knowledge | 26 endpoints; `/knowledge` placeholder | Implement article/SOP/approval UI | XL |
| **HIGH** | Licensing | 28 endpoints; `/licensing` placeholder | Implement edition/license/flag/usage administration | XL |
| **HIGH** | Metadata | 19 endpoints; `/metadata` placeholder | Implement definition/form/assignment UI | XL |
| **HIGH** | Appointments | 11 endpoints; `/appointments` placeholder | Implement calendar/list and lifecycle UI | L |
| **HIGH** | Data Quality | 14 endpoints; no route/client/page | Implement dashboard, rules, issues, corrections, validation UI | XL |
| **HIGH** | Dashboard | Root route loads four independently protected APIs | Make metrics permission-aware or add backend summary endpoint | M |
| **MEDIUM** | CRM/Supplier/Purchasing/Dashboard configuration | Mutation endpoints lack reachable admin screens | Add permission-protected settings pages | L |
| **MEDIUM** | Profile fields/User statuses | CRUD clients have no administration UI | Add settings surfaces | M |
| **MEDIUM** | Scheduler | Global executions endpoint has no UI | Add all-executions tab | S |
| **MEDIUM** | Automation | Action JSON rejection is silent | Add explicit per-action JSON validation | S |
| **MEDIUM** | Frontend quality | No lint/unit/component/E2E gate | Approve and introduce repository-consistent test tooling | L |
| **LOW** | Shell titles | Automation/Scheduler fallback titles missing | **Fixed in this audit** | XS |

## 12. Blockers and confidence

Static coverage and route/client/controller mapping confidence is high. Runtime,
database, Flyway, authentication, role-denial, upload/download, and mutation
confidence is low because the backend stack could not run. Browser results used
the repository's mock mode and must not be interpreted as proof of backend
integration. No production/VPS endpoint or real data was accessed.

## 13. Files changed by this audit

- `sami-frontend/src/layouts/DefaultLayout.vue` — added localized fallback title
  mappings for the already-completed Automation and Scheduler routes.
- `FULL_STACK_COVERAGE_AND_TEST_REPORT.md` — this requested evidence report.

No other application defect was changed. In particular, no missing module,
backend business rule, permission model, migration, Docker topology, or testing
framework was introduced during the audit.
