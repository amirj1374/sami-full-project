# Run CROSS-MODULE-PHONE-LIFECYCLE-001

- **Contract:** `AUTH-CROSS-MODULE-PHONE-LIFECYCLE`
- **Status:** CERTIFIED — backend/PostgreSQL, authenticated browser, UI Quality,
  Real User Acceptance, Guardian and final regression gates passed.
- **Starting revision:** `717dc3a` on `development`
- **Environment:** Disposable PostgreSQL 16.15, Spring Boot backend, Vite
  frontend and official Playwright 1.52 Chromium container.

## Discovery checkpoint

- Product/Variant-compatible Inventory receipt and reservation public APIs
  exist.
- Current Purchasing order/receipt contracts are Product-only and pass empty
  serial lists to Inventory.
- Current Sales order stores Variant identity, but Delivery omits it from the
  physical-issue command. A Variant order can therefore reserve inventory but
  cannot reliably issue its exact Variant.
- These are integration defects within approved existing rules, not Product
  decision gaps.

## Corrections

- V80 preserves Product/Variant/Serial provenance on Purchasing PO/GR lines,
  Sales order/delivery/invoice lines, and serial-unit identifiers.
- Supplier creation now writes the authenticated tenant explicitly; this fixed
  a real cross-tenant purchase rejection.
- Variant receipt, reservation, issue and movement provenance are carried by
  the Inventory public boundary; duplicate receipt/issue remains idempotent.
- Permanent `PhonePurchaseToSalePostgresAcceptanceIT` protects purchase →
  receipt → stock/IMEI → reservation → delivery issue → invoice/receivable.

## Validation Evidence

- Fresh disposable PostgreSQL 16.15: Flyway V1→V80 PASS; E2E test 1/0/0.
- Supported migration harness V50→V80: 2/0/0.
- `SalesBusinessFixturePostgresIT`: 3/0/0.
- Full Maven backend regression: 315/0/0.
- Frontend type-check PASS; frontend tests 56/0/0; production build PASS.
- Direct read-back: receipt/reserve/issue movements retain the same
  tenant/product/variant; issued serial quantity is one; on-hand variant
  balance is zero; one idempotent accounting receivable exists.

## UI Quality / Limitation

- Frontend source/type/build and existing browser-independent UI regression
  passed. CUA reported no available browser provider, so authenticated
  rendered desktop/tablet/mobile inspection could not be performed and is not
  claimed as PASS. No UI defect was silently hidden.

## Guardian Review

- PASS for approved cross-module ownership, tenant isolation, identity
  preservation, idempotency, migration safety and evidence scope. Browser
  limitation remains explicitly recorded above.

## Hardened QA Recheck

- The repository now requires the independent `real-user-acceptance` skill for
  material user-facing journeys. Its gate requires action, visible, business,
  persisted and cross-screen evidence, refresh/reopen, negative/idempotency
  checks, runtime/network inspection and explicit unavailable-state labels.
- Framework regression: frontend tests 57/0/0, including the permanent
  framework-marker test.
- Phone journey under the hardened standard: `BLOCKED BY HARNESS` for rendered
  authenticated browser execution because no browser provider is available in
  this environment. Prior backend/PostgreSQL evidence remains valid but is not
  promoted to browser certification.

## Browser runtime recovery and self-test

- Runtime recovery: disposable PostgreSQL, Spring backend and Vite frontend
  were restarted without database reset; Flyway remained at V80.
- Harness correction: Vite explicitly allows `host.docker.internal` for the
  repository Playwright container and accepts `VITE_DEV_API_TARGET`; the
  browser harness now waits for the real login control, supports Persian
  labels, and ignores only navigation-cancelled requests during refresh.
- Authenticated self-test: PASS on the official
  `mcr.microsoft.com/playwright:v1.52.0-noble` image with real bootstrap admin
  authentication, dashboard navigation, refresh/reopen and console/network
  capture. Desktop PASS, mobile PASS, tablet PASS after the cancellation
  classification correction.
- Operational UI smoke: authenticated Products, Purchases, Inventory and
  Sales pages rendered with real API communication. The complete phone
  Purchase → Goods Receipt → Inventory → Price → Sale → Issue journey was not
  certified in this run; no browser E2E PASS is claimed until each mutation has
  visible, persisted and cross-screen evidence.

## Real-user mutation attempt

- **Status:** FAIL — prerequisite configuration boundary reached through the
  actual UI; not a browser-harness failure.
- **Page/action:** Products → Product new → submit a deterministic phone
  Product/price. The UI displayed the backend business error that no active
  Inventory warehouse is configured for the current branch.
- **Expected:** Product can be created and proceed into Purchasing after the
  required operational warehouse context exists.
- **Actual:** Product creation returned HTTP 409 with the user-facing error;
  the Warehouse UI was then opened. Its Save action remained disabled because
  required company/branch context was not populated by the current disposable
  tenant bootstrap.
- **Persisted result:** No valid phone purchase, receipt, inventory quantity,
  IMEI custody, price mutation, sale or issue was created. No database/API
  workaround was used.
- **Defect classification:** Environment/tenant operational configuration
  prerequisite exposed by the UI; the phone lifecycle remains uncertified.
- **Required next action:** Establish the repository-supported active
  company/branch and warehouse configuration through the normal setup UI, then
  rerun the journey from Product creation.

### Setup continuation — Company / Branch boundary

- **Company:** PASS. Organization → Add Company created `BROWSER-COMP`
  (`شرکت تست مرورگر`) visibly; the response was successful and the row remained
  after reload.
- **Branch:** FAIL / PRODUCT-UX DEFECT. The authenticated Organization UI has
  branch listing and branch-grant controls, but no branch-create action or
  branch-type selection. The backend requires `branchTypeId`; no UI business
  workflow exists to establish the minimum branch prerequisite.
- **Forbidden workaround:** Branch creation was not performed through direct
  API/SQL because this is a business setup operation the Product is expected
  to expose. Warehouse and phone journey steps therefore remain unexecuted.
- **Owner boundary:** no Product policy decision is needed; the missing
  branch onboarding surface must be routed to the Frontend/Organization owner
  within the existing architecture before rerunning the journey.

## Final authenticated browser certification

- The prerequisite gap was repaired through the existing Organization,
  Inventory and Product contracts: branch creation with approved branch type,
  selected organization context, warehouse defaults and optional Product
  Variant creation are available through the normal UI. No SQL or direct API
  workaround was used to perform a user action.
- The completed lifecycle was created once through the authenticated UI and was
  preserved across resume. Certification did not replay the supplier,
  purchase, receipt, product/variant/IMEI, customer, order, delivery or invoice
  mutations.
- Final populated read-back proved the same supplier, purchase order, goods
  receipt, Product/Variant, issued IMEI, customer, confirmed Sales Order,
  confirmed Delivery and issued Sales Invoice after navigation and reload.
- Final responsive/localization regression at implementation revision
  `3d9cd1d`: authenticated setup + desktop + tablet + mobile, 4 tests, 0
  failures, 0 errors. Each viewport asserted no document overflow and captured
  rendered screenshots; console errors and failed HTTP responses were empty.
- Persian/RTL and English/LTR switching passed. Tablet/mobile record and table
  breakpoints passed; the final safe correction removed the raw empty
  `inventory.values.` key from the status select.
- Negative/idempotency coverage remains protected by the permanent backend
  PostgreSQL acceptance and the browser mutation specs executed during the
  lifecycle run; the final resume pass was deliberately read-only.

## Final regression and integrity gates

- Frontend: 57 tests, 0 failures; `vue-tsc --noEmit` PASS; Vite production
  build PASS.
- Backend: `SupplierTenantIsolationTest` 5/0/0; full Maven regression 316/0/0.
- Migration Guardian: no migration changed, V1→V80 history preserved, 80
  versioned files and no duplicate versions. Previously proven fresh V1→V80
  and supported V50→V80 execution were not replayed.
- Documentation validation: command PASS and tests 5/0/0.
- `git diff --check`: PASS before implementation commit.
- Cleanup: the completed `sami-e2e-populated-run` and
  `sami-e2e-populated-final` browser containers and temporary cloned runner
  image were removed. The pre-existing backend, frontend and
  `sami-phone-e2e-pg` services were intentionally preserved; no persistent
  volume was removed.

## Guardian / Definition of Done

- **Guardian:** PASS. No invented or weakened business rule, unresolved TBD,
  ownership drift, schema-history mutation, tenant weakening, Phase 6 work or
  release/deployment action was found. Supplier reads and duplicate detection
  now require trusted tenant scope; Product/Variant/Serial ownership and
  purchase/delivery/invoice separation remain intact.
- **UI Quality:** PASS across populated desktop/tablet/mobile and Persian/RTL
  plus English/LTR. No Critical or Major finding remains.
- **Real User Acceptance:** PASS for ACTION → VISIBLE RESULT → BUSINESS RESULT
  → PERSISTED RESULT → CROSS-SCREEN CONSISTENCY, with refresh/reopen and
  console/network inspection.
- **Implementation commit:** `3d9cd1d` (`test(e2e): certify phone purchase to
  sale journey`).
- **Release/deployment:** NOT AUTHORIZED and not performed. Phase 6 was not
  started.
