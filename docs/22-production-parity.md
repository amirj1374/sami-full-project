# Production parity and environment compatibility

This document is the repository-backed parity baseline for SAMI ERP. It records
what is known from source/configuration and deliberately separates that from
the runtime state of the VPS. It is not a claim that the current VPS has been
inspected or deployed.

## Environment inventory

| Environment | Protocol / host | Frontend and API | Backend / data | Evidence and status |
| --- | --- | --- | --- | --- |
| Local frontend dev | HTTP, Vite `localhost:7474` | Vite proxies `/api` to `VITE_DEV_API_TARGET` or `http://localhost:8080` | Standalone backend commonly runs on `localhost:8080` | `sami-frontend/vite.config.ts`, `.env.example`; dev-only topology |
| Local backend Compose | HTTP, host ports `5432` and `8080` | Frontend is not part of `sami-backend/docker-compose.yml` | PostgreSQL service and Spring default `dev` profile | `sami-backend/docker-compose.yml`, `application.yml` |
| Test | No fixed browser/server protocol; Node and Maven test processes | Frontend tests run against source/contracts; no browser is implied by `npm test` | Backend tests use Maven and Testcontainers dependencies where a test requires PostgreSQL | `sami-frontend/package.json`, `sami-backend/pom.xml`, `docs/15-testing-and-quality.md` |
| Production-like validation | HTTP on a disposable loopback port | Production Vite build is served by nginx; `/api/` is reverse-proxied to `backend:8080/api/` | PostgreSQL 16, Spring `prod` profile, generated secrets/admin, Linux containers | `scripts/deploy.ps1`, `sami-backend/docker-compose.prod.yml`, `sami-frontend/nginx.conf` |
| Production target | Repository expects HTTP port 80 and same-origin `/api`; actual host/runtime values are unknown | Actual image, env, active profile, migration level and proxy state require SSH/runtime evidence | No production read-only inspection was possible because SSH public-key authentication was rejected | `sami-backend/docker-compose.prod.yml`, `sami-frontend/nginx.conf`; SSH result is recorded as blocked, not inferred |

Known parity deltas are intentional unless marked as a defect:

- Dev defaults to Spring `dev`, verbose SQL logging and demo seeding; the
  production-like stack overrides `SPRING_PROFILES_ACTIVE=prod` and disables
  demo notification seeding.
- Dev may use a direct backend port; the full stack uses same-origin `/api` and
  nginx's internal `backend:8080` name. CORS is therefore relevant to the
  standalone/dev topology and must match the browser origin; the full stack
  still carries an explicit CORS setting.
- Backend timestamps and Hibernate JDBC operations use UTC. The browser uses
  the user's locale/time zone for display (`Intl`/`Date` formatting); API date
  values must remain ISO/UTC values.
- The host worktree is Windows, while the release images are Linux/amd64.
  Path case, file permissions and mounted storage therefore cannot be inferred
  from local filesystem behavior.
- Production-like storage is named Docker volumes (`uploads`, `managed-files`,
  `file-staging`); local defaults are relative filesystem paths.
- nginx provides SPA history fallback, gzip for text assets, one-year immutable
  caching for `/assets/`, no-cache rules for the manifest/service worker, and a
  21 MiB request limit aligned to Spring's 20/21 MiB multipart limits.
- No frontend WebSocket or EventSource usage was found. No cookie, SameSite or
  Secure-cookie authentication exists: staff authentication is stateless
  Bearer JWT in browser storage. CSRF is disabled in the backend because the
  staff API is designed as cookieless token authentication (`SecurityConfig`).

## Browser compatibility audit

The audit covered the requested browser API families in `sami-frontend/src`.

| API / capability | Usage | Secure context | HTTP production behavior | Form/API risk |
| --- | --- | --- | --- | --- |
| Web Crypto UUID/random values | `idempotencyKey.ts` | `randomUUID` may reject an insecure context; `getRandomValues`/fallback are guarded | Safe fallback key is generated | No longer runs as an unguarded pre-submit dependency |
| localStorage | Auth, locale, organization scope, preferences/navigation/theme/PWA | Not intrinsically HTTPS-only, but access can throw under privacy/storage policy | `browserStorage` catches access failures and uses tab-local fallback | No bootstrap/interceptor exception from storage access |
| sessionStorage / IndexedDB | No usage found | N/A | N/A | No risk found |
| File / FormData / Blob | Upload/import and response handling | Not HTTPS-only | Used only in upload/download flows; callers expose errors | Does not run during normal app bootstrap; upload failures are caught by form callers |
| URL.createObjectURL | CSV/download/avatar previews | Not HTTPS-only | Used after response/file selection and revoked in download paths | Affects file UI only, not ordinary JSON mutations |
| Notification / Web Push | Notification permission, service-worker push foundation | Requires secure context in the current browser model | HTTP returns `unsupported`; permission request is not attempted | Optional capability fails closed and cannot stop business forms |
| Service Worker | Production registration in `main.ts` | Requires secure context except browser-trusted localhost cases | Registration is skipped/rejected; an explicit compatibility warning is emitted | PWA update/push only; app remains mounted |
| matchMedia | PWA standalone/theme preference | No secure-context requirement | Guarded when unavailable | Optional UI capability only |
| MutationObserver | Global keyboard shortcut refresh | No secure-context requirement | Guarded when unavailable | Shortcuts degrade; mutations/API remain available |
| window/document/navigator | Print, focus, route/UI events, online state and PWA | No secure-context requirement for these uses | Used after mount or behind capability checks | No unguarded pre-API business dependency found |
| WebSocket / EventSource / BroadcastChannel / workers / geolocation / clipboard / intersection/resize observers | No usage found in application source | N/A | N/A | No risk found |

The formal frontend contract test prevents direct storage access outside the
adapter, direct `crypto.randomUUID()` outside the helper, insecure notification
requests, unguarded `matchMedia`/`MutationObserver`, and API clients that embed
the `/api` prefix twice.

## Mutation and contract audit

The following is the trace obtained from the actual clients/controllers/DTOs.
“Runtime gate” means the disposable HTTP gate exercises the path; “manual
pending” means the repository has a contract but this validation run did not
claim a full browser business journey.

| Area | UI → handler → client | Backend contract / persistence | Result |
| --- | --- | --- | --- |
| Sales core | `SalesView.vue` → `save()` → `salesApi.create/update()` → reload | `POST/PUT /api/v1/sales`, `SaleRequest` required company/branch/customer/type/items; database sale and audit rows | Runtime gate covers create, idempotency replay and fresh GET persistence |
| Sales quotation/order/delivery/invoice/receipt | Sales document/order/delivery/invoice/receipt panels → typed clients | Scoped DTOs require company/branch/customer/lines; order must be confirmed before delivery/invoice; receipt uses required `Idempotency-Key` | Static contract coverage; complete chained browser/runtime journey remains manual pending |
| Purchasing purchase | `PurchaseFormDialog`/`PurchasesView` → `purchasesApi` | `/api/v1/purchases` DTO and workflow endpoints | Static contract coverage; full chain pending |
| Purchasing PO/GR/supplier invoice | Purchasing panels → `purchaseOrdersApi`, `goodsReceiptsApi`, `supplierInvoicesApi` | Controllers are `/api/v1/...`; clients now use `/v1/...` relative to Axios `/api`; goods receipt body now matches backend `{lines,notes}` map contract | Path/body defect fixed and contract-locked; full chain pending |
| Customer / CRM | `CustomerFormDialog` → duplicate check/create/update → reload | `CustomerRequest` requires `displayName` and `typeId`; update supports `expectedVersion`; contacts/addresses are replacement lists | Runtime gate covers create, update and fresh GET persistence |
| Inventory | Inventory panels → `inventoryApi` adjustment/warehouse/transfer/count/serial/reservation clients | `InventoryController` owns `/api/v1/inventory`; validation and permission are server-authoritative | Static contract coverage; runtime domain flow pending |
| Treasury | `TreasuryView`/payment views → `treasuryApi` and payment APIs | Tenant-scoped account/transaction/cheque DTOs and workflow endpoints; backend owns posting/reversal | Static contract coverage; runtime domain flow pending |
| Accounting | Sales reads accounting entries; legacy accounting is staging/import UI | No canonical accounting mutation controller is exposed in the current frontend/backend surface; canonical promotion is intentionally phased/blocked | Not reported as implemented; no false PASS |
| Organization | `OrganizationView` and context store → organization clients | Company/branch/grant/context endpoints with RBAC and tenant scope | Static contract coverage; storage persistence is compatibility-safe |
| CRM configuration | CRM panels → config clients | `/api/v1/crm` configuration controllers and DTOs | Static contract coverage; runtime domain flow pending |

All reviewed mutation handlers either validate before calling the client or
surface a visible error through the shared error/notification path. Existing
“return while busy” guards are duplicate-submit protection, not validation
silence. The new gate additionally checks that a successful JSON mutation can
be read back from a new HTTP request.

## Official automated gate

Run the official pre-release gate from a clean `development` checkout:

```powershell
.\scripts\deploy.ps1 -Mode Validate -ApplicationVersion 0.5.0
```

The gate is production-parity aware and must pass before any export/upload or
deployment phase. It runs:

1. backend `clean verify`;
2. `npm ci` followed by the frontend dependency security audit (high severity
   findings fail the gate), tests, type-check and production build;
3. frontend/backend `linux/amd64` image verification;
4. a disposable PostgreSQL 16 + Spring prod-profile + nginx stack over HTTP;
5. SPA fallback, asset/manifest MIME, nginx health and `/api` proxy checks;
6. real authenticated login and a protected API request; and
7. `PRODUCTION_HTTP_COMPATIBILITY`: real authenticated organization-context
   selection, Customer create/update,
   Product create, Sales create, Sales idempotency replay, Sales fresh GET,
   Receipt create and Receipt idempotency replay through nginx.

The mutation smoke uses generated records through public APIs only; it never
inserts business rows directly. The disposable Compose project, network and
volumes are removed in `finally`, including failure paths. A failed gate stops
the release before artifact export/upload.

The gate is not a substitute for manual browser evidence. It does not pretend
that a Node contract test is Chromium. A release candidate still needs the
browser journey for UI state, console and network evidence, plus the complete
cross-module chains listed in the workflow tester catalog.

## Server parity

Production SSH read-only parity is **BLOCKED** for the current audit. The
configured SSH endpoint rejected the available public-key authentication, so
image IDs, runtime environment, nginx state, active Spring profile, Flyway
version, Java/Node runtime and container architecture on the VPS remain
unverified. No retry was converted into a deployment or a write, and no value
is inferred from repository defaults.

## Release decision for this audit

This document and the gate are part of the audit change. Real deployment,
version bump, production database writes and release execution are explicitly
out of scope. The final status must remain `PARTIAL` or `FAIL` until the
production-like gate and required browser/domain flows have evidence, and
until the server parity blocker is resolved or formally accepted.
