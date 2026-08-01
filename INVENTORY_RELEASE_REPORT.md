# Inventory Release Report

Date: 2026-08-01
Release branch: `codex/final-sami-release`

## Release commits

- Inventory implementation commit: `311e7a691e046f38ff91dab7f140313baf650c7c`
- Validated merge commit: `207beb8a8c0a1dd2542d2a27dd5894fada0abd1a`
- Feature branch: `codex/inventory-complete`

The implementation was merged with a non-fast-forward merge. The application tree at the merge commit is the tree used for the release-branch validation below. This report is committed separately after validation and changes documentation only.

## Delivered capability

### Backend

- Tenant-scoped warehouse and location management with default-location and default-warehouse invariants.
- Inventory balances with on-hand, reserved, available and moving weighted-average valuation.
- Append-only stock movement ledger for opening balance, adjustments, receipts, supplier returns, sales issues, customer returns, transfers and stock counts.
- Serial and IMEI tracking, status monitoring and receipt registration.
- Stock reservations with reserve, release and issue operations.
- Two-stage warehouse transfers with draft, shipped, received and cancelled lifecycle handling.
- Physical stock counts with snapshot, counted, posted and cancelled lifecycle handling.
- Dashboard, inventory valuation report, movement history, audit history, UTF-8 BOM CSV export and validated idempotent CSV import.
- Product compatibility projection and stock-change event publication.
- Purchasing receipt/return and Sales reservation/issue/return integration through the Inventory public service boundary.
- Trusted request tenant context on reads, writes, uniqueness checks, repository queries, audit entries and events; missing or mismatched tenant scope fails closed.
- Granular permissions: `inventory:view`, `inventory:manage-warehouses`, `inventory:adjust`, `inventory:transfer`, `inventory:count`, `inventory:reserve`, `inventory:issue`, `inventory:view-audit`, `inventory:report`, `inventory:import` and `inventory:export`.

### Database

- Flyway migration `V32__inventory.sql` promotes the existing Purchasing warehouse table instead of creating a duplicate warehouse owner.
- Adds locations, balances, movements, serial/IMEI units, reservations, document numbering, transfers, counts and Inventory audit storage with tenant-aware keys, constraints and indexes.
- Backfills existing product stock into balances and opening movements.
- Fresh PostgreSQL 16 validation applied migrations V1 through V32 successfully.
- Upgrade validation applied V32 over an isolated V31 schema containing legacy stock; the legacy quantity was preserved as one balance and one opening movement.

### Frontend

- Production Inventory route and permission-aware menu entry.
- Responsive balances, transfers, stock counts, warehouse/location management, serial monitoring, movements, reservations, reporting, audit and import/export experiences.
- Loading, error, empty, validation, confirmation and notification behavior follows existing SAMI components and conventions.
- Mobile layouts use cards for dense records and avoid horizontal page scrolling.
- Complete English and Persian labels with RTL/LTR and locale-aware number and Jalali date formatting.
- Inactive or unauthorized workflow panels are not mounted and therefore do not call protected APIs.

## Validation evidence

| Gate | Result |
|---|---|
| Backend full test suite | PASS — 184 tests, 0 failures, 0 errors, 0 skipped |
| Inventory focused tests | PASS — stock, tenant isolation, insufficient stock, transfer and count workflows |
| Frontend type-check | PASS — `vue-tsc --noEmit` through `npm run build` |
| Frontend production build | PASS — Vite 8.1.5, 1,005 modules transformed |
| Localization parity | PASS — 1,568 English keys and 1,568 Persian keys; no missing keys |
| OpenAPI exposure | PASS — 26 `/api/v1/inventory...` paths |
| Unauthenticated access | PASS — Inventory dashboard returns HTTP 401 directly and through the frontend proxy |
| Fresh database migration | PASS — PostgreSQL 16, V1 through V32, Hibernate schema validation and healthy startup |
| Existing database upgrade | PASS — V31 to V32 with legacy stock quantity preserved |
| API workflow smoke | PASS — adjustment, transfer ship/receive, count submit/post, reports, audit, BOM CSV export and CSV import |
| Browser desktop | PASS — authenticated Inventory UI, navigation and workflows rendered against real APIs |
| Browser mobile | PASS — 390 × 844 viewport, no horizontal overflow, mobile card presentation |
| RTL/localized browser | PASS — Persian labels, `dir=rtl`, Persian numerals and Jalali date rendering |
| Browser diagnostics | PASS — no warning or error entries |
| Backend Docker build/runtime | PASS — Linux/amd64, healthy container, actuator HTTP 200 |
| Frontend Docker build/runtime | PASS — Linux/amd64, healthy container, HTTP 200 and working `/api` proxy |
| Git whitespace check | PASS |

## Release images

- `sami-backend:inventory-release`
  - Image ID: `sha256:5fa1a9f39a8d82917fd066fef5a6479d2ce45874ba0543a8af38d385f0e18194`
  - Platform: `linux/amd64`
  - Embedded revision: `207beb8a8c0a1dd2542d2a27dd5894fada0abd1a`
- `sami-frontend:inventory-release`
  - Image ID: `sha256:3d5e9c42a514bcb7575dad5ffa7c9c8f1da00bdc0309d53aed006235af8a0e44`
  - Platform: `linux/amd64`

## Known limitations

- Inventory domain events use the repository's current in-process event pattern. Guaranteed cross-process delivery would require an approved durable outbox or messaging architecture.
- `products.stock` remains a backward-compatible projection for existing consumers; the tenant-scoped Inventory ledger and balances are authoritative for new Inventory behavior.
- Spreadsheet exchange is CSV rather than XLSX. Exports include a UTF-8 BOM so Persian text is detected correctly by Microsoft Excel and other compatible spreadsheet applications.

No placeholder Inventory page, mock API, default-tenant fallback or unresolved Inventory migration remains in this release.
