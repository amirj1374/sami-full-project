# RUN-EMPLOYER-DEMO-READINESS-001

- **Purpose:** bounded employer-demo / real-world readiness pass after P6-S2.
- **Revision:** `50cac57` plus browser-runtime correction evidence.
- **Primary E2E:** Phone Purchase → Sale remains CERTIFIED and was not replayed.
- **Phase 6 control:** implementation PAUSED; no P6-S3+ activation.

## Scenario matrix

| Scenario | Priority | Test Layer | Expected Result | Actual Result | Evidence | Status | Defect/Fix |
|---|---|---|---|---|---|---|---|
| Single serialized phone, single IMEI | P0 | Existing PostgreSQL acceptance | Receipt creates exact stock/IMEI and sale issues it once | Proven | `PhonePurchaseToSalePostgresAcceptanceIT` 1/0/0 on fresh V1→V80 | CERTIFIED | None |
| Multiple serialized phones / purchase lines | P0 | Backend/integration | Each IMEI remains independently traceable and stock-safe | Not separately exercised in this bounded pass | Existing serial/variant acceptance coverage | PARTIAL | No new defect established |
| Same Product, different Variants | P0 | Backend/PostgreSQL | Variant balances and IMEIs remain isolated | Proven by existing variant/serial acceptance evidence | `InventorySerialVariantPostgresAcceptanceIT`, prior certification | CERTIFIED | None |
| Different suppliers | P1 | Existing UI/read-back evidence | Supplier and purchase traceability preserved | Existing certified journey covers supplier traceability | Cross-module run | CERTIFIED | None |
| Duplicate/invalid/missing IMEI | P0 | Backend tests | Rejected; no inventory mutation | Existing purchase IMEI validation coverage | `PurchaseImeiValidationTest` | CERTIFIED | None |
| Repeated receipt / delivery submission | P0 | Backend/PostgreSQL | Idempotent; no duplicate stock or financial effect | Existing lifecycle acceptance and idempotency evidence | Cross-module run + acceptance suites | CERTIFIED | None |
| Partial receipt / rejection / cancellation | P1 | Backend | Existing supported behavior preserved | Not separately exercised | No new evidence in this pass | NOT TESTED | No owner decision raised |
| Available / reserved / issued lifecycle | P0 | PostgreSQL + existing browser evidence | State transitions exact and durable | Proven | Cross-module run; fresh acceptance 1/0/0 | CERTIFIED | None |
| Variant / warehouse / tenant isolation | P0 | Backend/PostgreSQL + browser | No cross-scope leakage or mixed stock | Existing tenant/branch/supplier evidence; report browser blocked | `SupplierTenantIsolationTest`, organization evidence | PARTIAL | Browser harness blocked |
| Product/variant and historical prices | P1 | Backend tests + existing UI evidence | Completed documents retain frozen values | Existing sales/purchase contract evidence | Prior accepted suites | CERTIFIED | None |
| Insufficient stock / wrong IMEI / issued IMEI resale | P0 | Backend/integration | Sale rejected; no negative stock | Existing sales/inventory regression evidence | Sales/inventory acceptance suites | CERTIFIED | None |
| Invoice/receivable exactly once | P0 | PostgreSQL | One invoice and one receivable; retries do not duplicate | Proven | Phone acceptance and accounting evidence | CERTIFIED | None |
| Rendered report surfaces | P1 | Real browser | Authenticated reports render without runtime/network errors | Fresh Playwright login now returns 200 and reaches `/`; auth setup and cold-auth pass. Full populated report run still requires the established lifecycle fixture identifiers. | Playwright request/response trace; `npm run run-real-user-acceptance` auth-setup + cold-auth PASS | PARTIAL | CORS runtime correction applied |
| Clean employer demo journey | P0 | Real browser | Employer can follow Product→Supplier→Purchase→Receipt→Inventory→Sale→Invoice | Not completed in this bounded continuation: browser authentication is fixed, but the runner has no durable `SAMI_E2E_*` lifecycle identifiers and the organization branch setup row timed out waiting for branch-types. | Playwright auth proof; runner output (4 passed, 8 failed, 14 skipped; data-dependent failures separated) | BLOCKED BY TEST/ENVIRONMENT | Requires populated fixture identifiers and focused branch investigation |

## Readiness gates

- Browser harness / login: **PASS**. The browser sent `POST http://127.0.0.1:7474/api/v1/auth/login` with Origin `http://127.0.0.1:7474`; the stale backend allow-list returned 403 CORS while curl (without Origin) returned 200. Adding `127.0.0.1:7474` to the durable backend defaults and recreating the disposable backend changed the browser response to 200 and navigated to `/`.
- Desktop: **PARTIAL**; fresh auth, product, and warehouse rendered checks pass; populated lifecycle/report rows remain blocked by missing runner fixture identifiers and branch setup timeout.
- Tablet: **NOT RE-RUN** in this continuation; prior certified evidence remains valid.
- Mobile: **NOT RE-RUN** in this continuation; prior certified evidence remains valid.
- Persian/RTL: **NOT RE-RUN** in this continuation; prior certified evidence remains valid.
- Tenant isolation: **PARTIAL**; backend evidence PASS, fresh browser check remains pending the populated fixture run.
- Inventory/IMEI integrity: **PASS** on fresh PostgreSQL acceptance and prior regression evidence.
- Financial/accounting integrity: **PASS** on existing acceptance evidence; no duplicate effects observed.
- Employer-demo journey: **BLOCKED BY HARNESS**.

## Defect counts

- P0/Critical remaining: **0 proven product defects**.
- Major remaining: **0 proven product defects**.
- Minor known: **0 newly introduced**; existing harness/runtime limitation is not a product defect.

## Verdict

**EMPLOYER-DEMO READINESS: NOT READY — browser authentication is corrected and PASS; remaining blocker is the bounded readiness runner environment (missing durable lifecycle identifiers plus the focused branch-types wait), not a proven product defect.**
The primary Phone Purchase → Sale E2E remains CERTIFIED. Phase 6 implementation remains paused at the clean P6-S2 boundary.
