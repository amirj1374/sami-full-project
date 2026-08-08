# SAMI ERP implementation backlog

This is the canonical prioritized list of incomplete work. It complements the
current status in `docs/05-module-catalog.md`; historical backlog and release
reports remain evidence, not current scheduling authority.

## P0 — release/user-blocking

### Recover customer-origin Purchasing source

- **Objective:** recover the previously developed customer/sale counterparty work without reconstruction or history loss.
- **Current state:** handoff names commit `744ca38`, but it is absent from all fetched refs, object storage, worktrees and stashes in this clone.
- **Remaining scope:** obtain an approved Git bundle/remote branch, inspect its files/history, reconcile against current `development`, and resolve its consumed V37 migration proposal with V38+.
- **Dependencies:** old workstation/backup/remote; repository reconciler; migration guardian.
- **Affected modules:** Purchasing, CRM, Inventory, Sales, Data Quality IMEI, reports.
- **Recommended branch:** `codex/feature-customer-purchases` after recovery.
- **Read first:** `docs/HANDOFF_NEXT_SESSION.md`, `PURCHASING_RELEASE_REPORT.md`, `SALES_DOMAIN_DESIGN.md`.
- **Validation required:** containment/diff audit, backend full/focused tests, frontend tests/type-check/build, fresh and upgrade Flyway, Docker/Compose, API/browser/mobile/RTL.

### Restore workstation release tooling

- **Objective:** make current revisions genuinely testable before release decisions.
- **Current state:** Git/Node/npm work; Java is 17; Maven, Docker/Compose/Buildx and OpenSSH are unavailable.
- **Remaining scope:** install/configure approved Java 21, Maven 3.9+, Docker Desktop and OpenSSH; create ignored local environment files.
- **Dependencies:** workstation administrator access and official installers.
- **Affected modules:** all.
- **Recommended branch:** none; workstation-only setup.
- **Read first:** `docs/NEW_WORKSTATION_SETUP.md`, `docs/15-testing-and-quality.md`.
- **Validation required:** all version checks, Maven verify, PostgreSQL/Flyway startup, Docker build/health and frontend/browser gate.

## P1 — important functional completion

### Complete customer-origin Purchasing and trade-in

- **Objective:** support purchasing used/new/sealed devices from CRM customers and an explicitly approved same-customer sale linkage.
- **Current state:** supplier Purchasing is complete; customer-origin behavior is not present on `development`.
- **Remaining scope:** counterparty invariant, inspection/declaration/valuation snapshots, settlement/reversal/accounting boundary, CRM timeline, inventory receipt guard, IMEI validation, permissions, UI/reports and migration V38+.
- **Dependencies:** recovered source; approved payment/offset decision; current Sales/Inventory/CRM contracts.
- **Affected modules:** Purchasing, CRM, Sales, Inventory, Data Quality, reports.
- **Recommended branch:** `codex/feature-customer-purchases`.
- **Read first:** handoff, Purchasing/Sales reports and relevant module/migration/contract skills.
- **Validation required:** full cross-stack and database release gate; no sale offset may be simulated as a discount.

### Complete Data Quality UI integration

- **Objective:** release an end-to-end Data Quality workspace rather than merely route an existing partial view.
- **Current state:** backend API, permissions, typed client, localization and `DataQualityView.vue` exist; V35 disables the module and the release-contract test excludes its route.
- **Remaining scope:** verify every view operation/contract, workflow coverage, tenant/security behavior, responsive/browser states, lifecycle enablement through a forward migration and release evidence.
- **Dependencies:** Java/PostgreSQL/Docker/browser tooling.
- **Affected modules:** Data Quality and consumers of validation rules.
- **Recommended branch:** `codex/complete-data-quality`.
- **Read first:** module catalog, V13/V25/V35, Data Quality controller/view, contract/frontend/migration skills.
- **Validation required:** focused/backend suite, frontend gates, fresh/upgrade Flyway, API/browser/mobile/RTL.

### Complete Files/Media UI integration

- **Objective:** release managed files with one clear storage owner.
- **Current state:** large backend API, typed client, localization and `FilesView.vue` exist; V35 disables navigation; legacy and managed storage overlap.
- **Remaining scope:** resolve ownership boundary, verify upload/download/version/folder/delete contracts, security/retention behavior, browser UX, lifecycle migration and operational storage validation.
- **Dependencies:** approved storage ownership; writable isolated storage; full tooling.
- **Affected modules:** Files, Suppliers and every file consumer.
- **Recommended branch:** `codex/complete-files-media`.
- **Read first:** `docs/12-files-and-communications.md`, V18/V25/V35, FileController/FileService/FilesView.
- **Validation required:** security/size/type tests, backend/frontend gates, storage persistence, Docker/browser/mobile/RTL.

### Complete Appointments routing and workflows

- **Objective:** release booking/resource workflows through a supported navigation path.
- **Current state:** scheduling backend, permissions, typed client, localization and focused `AppointmentsView.vue` exist; V35 disables it.
- **Remaining scope:** reconcile booking payloads and customer selection, complete management/cancel/reschedule/wait-list/resource workflows as approved, add release tests, then enable through a forward migration/static route.
- **Dependencies:** calendar/resource contracts and full tooling.
- **Affected modules:** Appointments, Resources, CRM, Calendar, Scheduler.
- **Recommended branch:** `codex/complete-appointments`.
- **Read first:** scheduling/event architecture, V23–V25/V35, SchedulingController and AppointmentsView.
- **Validation required:** concurrency/overlap tests, contract/frontend gates, Flyway, browser/mobile/RTL.

### Reporting completeness

- **Objective:** define and deliver the missing general reporting surface without duplicating module reports.
- **Current state:** Dashboard and several modules provide reports/CSV exports; no canonical general reporting owner exists.
- **Remaining scope:** inventory existing reports, approve ownership/permissions/scope, define cross-module queries and UI, and avoid creating a second analytics owner.
- **Dependencies:** business KPI/report specification and accounting decisions where financial reporting is involved.
- **Affected modules:** Dashboard and all reporting producers.
- **Recommended branch:** `codex/complete-reporting` after architecture approval.
- **Read first:** Dashboard architecture, module release reports, roadmap/open decisions.
- **Validation required:** tenant/permission correctness, query performance, exports, frontend/browser/mobile/RTL.

## P2 — UX/product completeness

### Mobile font verification and correction

- **Objective:** prove and correct Persian/English font rendering on mobile and installed PWA surfaces.
- **Current state:** Vazirmatn Variable is bundled/imported and global/Vuetify font ownership exists; real-device loading/computed/offline behavior is unverified.
- **Remaining scope:** network/font-file audit, computed styles, production MIME/cache, service-worker/offline behavior, iOS/Android and specified mobile widths; fix only proven defects.
- **Dependencies:** browser/physical-device access and production-like build.
- **Affected modules:** global frontend/PWA.
- **Recommended branch:** `codex/fix-mobile-font-system`.
- **Read first:** frontend experience and PWA reports; global CSS, Vuetify, `main.ts`, nginx/service worker.
- **Validation required:** test/type-check/build, font request/glyph/computed-style evidence, offline and mobile/RTL screenshots.

### Real closed-app Web Push

- **Objective:** add secure multi-device push delivery behind Notification Center.
- **Current state:** frontend Push API foundation exists; no subscription persistence, VAPID configuration or backend delivery provider exists.
- **Remaining scope:** approved subscription API/entity, secret/config lifecycle, provider, delivery/retry/cleanup, privacy/redaction, user/device management and notification event integration.
- **Dependencies:** architecture/security approval and provider dependency decision.
- **Affected modules:** Notifications, Communication, PWA, Settings.
- **Recommended branch:** `codex/feature-web-push-delivery`.
- **Read first:** `PUSH_NOTIFICATION_FOUNDATION.md`, Settings report, security/configuration docs.
- **Validation required:** migration/security/contract tests, browser permission/subscription tests, VAPID delivery on supported browsers, fallback and revocation behavior.

## P3 — future/platform

### Repairs

- **Objective:** define and implement repair intake, diagnosis, work, parts and lifecycle ownership.
- **Current state:** planned references/integration seams only.
- **Remaining scope:** full approved business/domain specification before implementation.
- **Dependencies:** Inventory, Appointments, Files, Notifications and Accounting decisions.
- **Affected modules:** new Repairs plus dependencies.
- **Recommended branch:** `codex/feature-repairs` only after architecture approval.
- **Read first:** roadmap, scheduling/files architecture, module-builder workflow.
- **Validation required:** full module release gate.

### Warranty

- **Objective:** define warranty eligibility, claims, evidence and resolution ownership.
- **Current state:** planned references only.
- **Remaining scope:** policy/configuration model and links to Sales, Products, Repairs and Files.
- **Dependencies:** approved Repairs/product identity and accounting boundaries.
- **Affected modules:** new Warranty plus Sales/Products/Repairs.
- **Recommended branch:** `codex/feature-warranty` after architecture approval.
- **Read first:** roadmap/domain model and relevant release reports.
- **Validation required:** full module release gate.

### Installments

- **Objective:** define installment plans, schedules, collections, delinquency and settlement boundaries.
- **Current state:** portal/workflow references only; no canonical owner.
- **Remaining scope:** financial policy, payment/accounting ownership and regulatory/security decisions.
- **Dependencies:** Payments and Accounting architecture.
- **Affected modules:** Sales, Portal, future Payments/Accounting.
- **Recommended branch:** `codex/feature-installments` after decisions.
- **Read first:** roadmap/open decisions, Sales domain design, portal architecture.
- **Validation required:** financial precision, lifecycle, idempotency, permissions, audit and full release gate.

### Canonical Accounting

- **Objective:** establish General Ledger ownership and posting/reconciliation contracts.
- **Current state:** Sales has local accounting-entry records; no chart of accounts or canonical ledger exists.
- **Remaining scope:** approved chart/fiscal periods/journals/posting/reversal/reconciliation/reporting architecture and migration plan.
- **Dependencies:** executive/accounting decisions; Payments, Sales, Purchasing and Installments boundaries.
- **Affected modules:** cross-cutting financial domains.
- **Recommended branch:** architecture task first; implementation branch only after approval.
- **Read first:** roadmap/open decisions, Sales/Purchasing design and architecture-auditor workflow.
- **Validation required:** data-integrity/concurrency/audit/migration/full financial release gate.
# Current priority update — 2026-08-08

- Completed: Customer Purchase / Trade-in purchasing model, CRM history, inventory receipt guard, settlement snapshot, linked-sale validation, and IMEI checksum validation (V38).
- Completed: Data Quality end-to-end route/lifecycle activation (V39).
- Validated: centralized Vazirmatn production/mobile font pipeline; no font change required.
- Approval required: real Web Push canonical owner, persistence, VAPID secret and retry contract (proposal in `FULL_NEXT_PHASE_IMPLEMENTATION_REPORT.md`).
- Environment blocker: install Docker Desktop and Windows OpenSSH Client with administrator elevation, then run fresh PostgreSQL/Flyway and Linux image/Compose gates.
