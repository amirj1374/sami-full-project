# SAMI ERP — reusable development context

Last repository inspection: 2026-09-05. Source/configuration is authoritative; current operational state is in `docs/HANDOFF_NEXT_SESSION.md`.

## Repository and stack

- Monorepo; `sami-backend` and `sami-frontend` are tracked directories. Integration branch: `development`.
- Backend: Java 21, Spring Boot 3.5.3, PostgreSQL 16 target, Flyway, Maven 3.9+. Schema is contiguous through V50.
- Frontend: Node 22 image, Vue 3.5, TypeScript 5.7, Vite 8, Vuetify 3, Pinia, Vue Router, Axios and Vue I18n.
- Containers: PostgreSQL 16, JDK/JRE 21 backend, Node 22/nginx frontend; production Compose is under `sami-backend`.

## Canonical owners

- Backend services own business/transactions; controllers are transport; `ApiResponse`/global exception handling own API errors.
- `TenantContext` is the trusted tenant owner. Backend RBAC is authoritative; route/button checks are presentation only.
- Flyway owns schema. Inventory owns balances/movements/reservations/serials; integrations use `InventoryStockOperations`.
- Frontend typed clients live in `src/api`; shared types in `src/types`; route permissions plus backend menu own reachability; English/Persian and RTL/LTR move together.
- Design system: `src/plugins/vuetify.ts`, `src/styles/global.css`, `AppFormSection.vue`; PWA: `usePwa`, service worker and Settings.
- Authenticated-page keyboard operation is owned globally by `AppKeyboardShortcuts.vue` and `DefaultLayout.vue`: buttons, links, menu actions and tabs receive direct shortcuts, route transitions restore main-content focus, and a skip control bypasses navigation. Per-user enablement and badge visibility are persisted through `UserExperiencePreference` and the Profile settings UI.
- Real browser business-journey validation is owned by the repository-scoped `sami-ui-workflow-tester` skill; it covers isolated test data, persisted cross-module flows, RBAC login, keyboard operation, RTL/LTR, responsive widths, network/console evidence and cleanup reporting.

## Current capability state

- Complete core: Authentication/RBAC/Users, Dashboard, CRM, Suppliers, Products basic CRUD, Inventory, supplier Purchasing, Sales, Automation, Licensing, Scheduler and PWA/Settings.
- Customer Purchase/Trade-in: implemented in V38 with CRM Customer seller, inspection/valuation/settlement, linked-Sale integrity, CRM history, Inventory receipt guard and IMEI validation.
- Data Quality: active/routed in V39 at `/data-quality` with `data-quality:view`.
- Legacy Asan: V40 secure staging/comparison plus V45 mapping metadata and V46 accounting migration groups/reconciliation/acceptance evidence. It supports RAR/Jet, manifest ZIP, and individually uploaded Asan accounting XLSX reports with tenant isolation, idempotency, conservative matching, raw provenance, and zero canonical writes. The bounded SAX/JDBC staging checkpoint passes a generated 34,000-row journal under a 256 MB test heap; the supplied real-file PostgreSQL smoke remains pending. Canonical accounting promotion remains intentionally blocked pending an approved owner.
- HAMTA: V41 Inventory-owned activation-code custody with Purchasing/Sales/reporting/audit; implementation complete, infrastructure/live bilingual validation pending.
- Market Sync: V42 source/pricing/inventory/sale/rules/history/health UI implemented. Authorized public HTTPS JSON feeds can use the bounded `STRUCTURED_JSON_V1` adapter with environment-referenced credentials. Production remains partial until official Rond contracts and a real website publication connector exist; publication fails closed.
- Treasury: V49 owns tenant-scoped financial accounts, transactions, immutable movements, reversals, cheques and daily closings; `/treasury` is permission-gated and bilingual.
- Purchase Payment Requests: V50 owns employee requests, decisions, partial/multiple receipts and daily capacity. Payments post only through Treasury; reminders use Scheduler and Notification Center; `/purchase-payments` is permission-gated and bilingual.
- Automation notifications: the existing `notify` action now uses Notification Center with tenant/user validation and deterministic idempotency rather than a log-only side effect.
- Employees & Attendance: V43 introduces the canonical tenant-scoped employee identity, optional user linkage, company/branch assignment, manual clock-in/out, correction/report contracts, audit trail, permissions, bilingual UI and mobile record cards. Payroll, leave workflows and biometric/device ingestion are not part of this phase; fresh PostgreSQL runtime and live-browser validation remain pending.
- 0912 SIM Investment: V47 provides tenant-scoped, idempotent CSV/XLSX market-snapshot staging, normalized 0912 identities, price/listing history, rond-pattern classification, confidence-aware market valuation, opportunity ranking, audit evidence, bilingual responsive UI and read-only links to canonical purchasing/sales prices. Imports never write Inventory, Purchasing or Sales records; zero prices remain evidence but are excluded from valuation.
- Keyboard-only operation: V44 persists a default-on per-user shortcut preference; authenticated actions receive direct scoped Shift+key shortcuts, including active-dialog isolation, typing-field protection, Persian/English physical-key support, semantic Enter/Space activation and fast command-palette navigation.
- Partial/unrouted: Files/Media, Metadata, Appointments/Resources, Knowledge, Portal and Communication. Organization now provides tenant-scoped company list/create/edit with RBAC/audit; branch management, active-company selection and cross-company policy remain separately scoped. Reports remain partial.
- Web Push: browser foundation/in-app inbox only. Backend subscriptions, VAPID secret and provider delivery are not implemented; approval proposal is in `FULL_NEXT_PHASE_IMPLEMENTATION_REPORT.md`.
- Planned: Repairs, Warranty, Installments and canonical Accounting.

## Validation and priorities

- Local Java 21/Maven full backend gates and frontend tests/type-check/build pass for the end-of-day revision; exact evidence is in `END_OF_DAY_HANDOFF_REPORT.md`.
- `scripts/deploy.ps1 -Mode Validate` is the reusable local release gate: it runs backend/frontend checks, builds exact `linux/amd64` images, and smoke-tests a generated-credential, disposable PostgreSQL/backend/nginx Compose stack before cleaning only its own resources. `Full` invokes it before artifact export or any VPS connection.
- Next priority: repeat authenticated exact-viewport browser QA on a runner without the local API-blocking limitation, then obtain official Market Sync contracts. Do not start Web Push or speculative business modules without the required approval.

## Commands

- Backend: `mvn test`, `mvn clean verify`.
- Frontend: `npm test`, `npm run type-check`, `npm run build`.
- Docs: `node scripts/validate-documentation.mjs` and `node --test scripts/validate-documentation.test.mjs`.
- Release preflight: `.\scripts\deploy.ps1 -Mode Validate` (or `-Mode Full` to validate before export/upload/deploy).
- Setup/testing owners: `docs/NEW_WORKSTATION_SETUP.md`, `docs/15-testing-and-quality.md`.
