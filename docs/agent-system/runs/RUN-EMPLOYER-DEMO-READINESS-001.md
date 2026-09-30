# RUN-EMPLOYER-DEMO-READINESS-001

- **Purpose:** bounded employer-demo / real-world readiness pass after P6-S2.
- **Revision:** `acf4275` plus invariant-test implementation progress.
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
| Clean employer demo journey | P0 | Real browser | Employer can follow Product→Supplier→Purchase→Receipt→Inventory→Sale→Invoice | Adversarial trace proved the running `/api/v1/sales-orders/5` response omits the persisted IMEI/serial fields; the populated UI therefore cannot prove serialized traceability. Branch setup remains unproven. | PostgreSQL row `ORD-2026-000005`/IMEI `351790594941501`; fresh Playwright request returned 200 but line omitted `imei` and `serialNumber`; rendered assertion failed | FAIL / BLOCKED BY RUNTIME | Running backend is stale relative to source DTO; rebuild/restart required, then fresh UI proof |

## Readiness gates

- Browser harness / login: **PASS**. The browser sent `POST http://127.0.0.1:7474/api/v1/auth/login` with Origin `http://127.0.0.1:7474`; the stale backend allow-list returned 403 CORS while curl (without Origin) returned 200. Adding `127.0.0.1:7474` to the durable backend defaults and recreating the disposable backend changed the browser response to 200 and navigated to `/`.
- Durable E2E fixture handoff: **PASS**. Playwright config now loads the repository-owned `tests/browser/readiness-fixtures.env`; the values are stable business identifiers from the certified tenant/context and the populated runner resolved the existing product, variant, purchase, receipt, customer, order, delivery, invoice and warehouse without mutation.
- Organization setup harness: **NOT COMPLETE**. The branch-types wait now uses URL-path matching rather than a brittle raw URL suffix; a deterministic successful branch setup has not yet been proven.
- Sales detail serial/IMEI traceability: **PASS** after backend runtime correction and independent QA retest. With company 5 / branch 12 selected through the rendered organization context menu, `ORD-2026-000005` showed Product `Browser Phone MUJTB60C`, Variant `Browser Phone 256GB MUJTB60C`, and IMEI `351790594941501`; refresh/reopen and dashboard→Sales navigation/reopen preserved the same values.
- Backend runtime correction: rebuilt the backend jar from current source and restarted only `sami-e2e-backend` against the preserved PostgreSQL database. Fresh authenticated requests now return Sales Order line `imei: 351790594941501` and Branch Types `200` with six options. No business data was mutated.
- Organization setup / branch-types selection: **PASS** after backend correction and independent QA retest. Fresh browser received HTTP 200 with six options; `Retail Store` was visible/selectable, visibly reflected in the form, and validation kept Save disabled until required fields were complete. A separate post-create row assertion found the new branch outside the first paginated page; this is not a branch-type or selection failure and is recorded as a non-blocking pagination/QA assertion issue.
- Responsive/Persian lane: **DESKTOP PASS** with branch 12 selected, certified order/detail/IMEI visible, no horizontal overflow and no console errors. Permanent diagnostic `responsive-context-diagnostic.spec.ts` now proves both **TABLET PASS** and **MOBILE PASS**: rendered context selection, PUT context, Sales list request with company 5/branch 12, certified order, Product/Variant/IMEI, refresh/reopen, no overflow and no console errors.
- Tenant isolation lane: **PARTIAL**. Customer, Purchase, Company and Branch backend boundaries PASS; Supplier browser PASS; Inventory/serial/variant integrity PASS. Product, Warehouse, and several Sales/Inventory/IMEI/Invoice browser known-ID/export checks remain NOT TESTED. No unauthorized leakage was evidenced.
- Scenario review: high-risk core integrity is PASS for duplicate serial, wrong Variant/IMEI, issued reuse, insufficient stock, backorder, release/cancellation, repeated invoice, Variant and Warehouse isolation. Missing/invalid IMEI, repeated receipt, duplicate delivery, multi-serialized/multi-line documents, partial receipt, returns, cost-change stability and minimum-margin behavior are explicitly DEFERRED LOW-RISK or NOT TESTED because no supported/approved behavior-specific evidence was found.
- Cross-screen contradiction review: **PASS** on the preserved certified transaction; tenant/company/branch, Product/Variant, Supplier, PO-3, GR-3, warehouse, IMEI, Sales Order, Delivery, Invoice, Customer, stock state and exactly one receivable reconcile.
- UI Quality: **PASS** for the populated Persian Sales Order detail after `9ba8cc3`: localized `شماره`, `مشتری`, `وضعیت` labels, Product/Variant/IMEI visible, no overflow, no console errors. The prior MAJOR defect was independently retested and closed.
- Final isolation matrix: Product **PARTIAL**; Variant **PASS**; Warehouse **NOT TESTED** for dedicated tenant/company/branch known-ID/export; Sales **PARTIAL**; Inventory **PARTIAL**; Serial/IMEI **PASS**; Invoice **PARTIAL**; Company/Branch **PASS**. No leakage was discovered, but aggregate isolation is not PASS.
- Final integrity classifications: serialized receipt without IMEI **NOT TESTED**; malformed IMEI **NOT TESTED**; repeated Goods Receipt **NOT TESTED**; duplicate delivery **PARTIAL**; multi-serialized/multi-line/multi-Variant document **PARTIAL**; historical cost/price mutation stability **PARTIAL**; partial receipt **N/A — no approved behavior-specific workflow found**; returns/reversals **N/A — no supported serialized-phone return workflow found**; minimum-margin **NOT TESTED**. High-risk rows remain open and are not downgraded to low risk.
- New permanent invariant coverage: `InventoryWarehouseTenantIsolationTest` asserts trusted tenant scoping for warehouse listing. Focused Maven execution was attempted in a disposable Maven container but produced no result after several minutes and was terminated; no PASS/FAIL is claimed. Exact continuation: execute this test and add/run the remaining Product/Sales/Inventory/Invoice isolation and replay/idempotency tests.
- Guardian: **NOT READY**. Independent review confirms core integrity, responsive evidence and cross-screen PASS, but tenant isolation remains PARTIAL, several scenario rows remain NOT TESTED/DEFERRED, and a fresh final Persian employer demo has not been proven in this closure wave.
- Context finding: fresh sessions may default to another branch; the certified order is correctly absent there. Selecting company 5 / branch 12 through normal UI restores the certified records; no cross-scope leakage was observed.
- Desktop: **PASS**; Tablet: **PASS**; Mobile: **PASS**; Persian/RTL: **PASS** from completed responsive diagnostic evidence.
- Tenant isolation: **PARTIAL**; high-risk backend boundaries and Supplier browser PASS, but Product/Warehouse/Sales/Inventory/IMEI/Invoice rendered known-ID/export checks remain not independently closed.
- Inventory/IMEI integrity: **PASS** on fresh PostgreSQL acceptance and prior regression evidence.
- Financial/accounting integrity: **PASS** on existing acceptance evidence; no duplicate effects observed.
- Employer-demo journey: **BLOCKED BY HARNESS**.

## Defect counts

- P0/Critical remaining: **0 proven product defects**.
- Major remaining: **0 proven product defects**.
- Minor known: **0 newly introduced**; existing harness/runtime limitation is not a product defect.

## Verdict

**EMPLOYER-DEMO READINESS: NOT READY — UI Quality and cross-screen consistency PASS, but tenant isolation is PARTIAL, high-risk receipt/IMEI/idempotency gaps remain NOT TESTED/PARTIAL, and the final Persian employer demo plus Guardian closure are incomplete.**
The primary Phone Purchase → Sale E2E remains CERTIFIED. Phase 6 implementation remains paused at the clean P6-S2 boundary.
