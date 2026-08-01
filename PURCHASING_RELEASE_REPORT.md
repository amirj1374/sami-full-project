# Purchasing Release Report

## Release identity

- Feature commit: `514b20af64e13f83a9916c1307f18c7bb00a16c9`
- Release integration commit: `94b0f6f953a868bb2f62469b5fd28f39f9d0c6f4`
- Release branch: `codex/final-sami-release`
- Database migration: `V33__purchasing_completion.sql`

## Delivered workflows

- Tenant-scoped purchase creation, editing, submission, approval, rejection, cancellation, receiving and returns.
- Tenant-safe attachments, identifiers, audit logs, history, reference validation and uniqueness checks.
- Configurable purchase types, statuses, cancellation reasons, identifier types and approval rules with platform defaults and tenant overrides.
- Dashboard metrics, operational reports and UTF-8 BOM CSV export.
- Validated, idempotent CSV draft import with tenant-scoped supplier, warehouse and product resolution.
- Permission-aware responsive desktop/mobile frontend for orders, reporting and configuration.
- Synchronized English and Persian translations with RTL-compatible presentation.

## Validation executed on the release branch

- Backend: Maven test suite passed — 188 tests, 0 failures, 0 errors, 0 skipped.
- Frontend: `npm run type-check` passed.
- Frontend: `npm run build` passed with Vite 8.1.5 (1,014 modules transformed).
- Localization: English/Persian recursive key parity passed — 1,621 keys in each locale, no missing keys.
- Source integrity: `git diff --check` passed.
- Live API smoke test: authenticated login through the Vite `/api` proxy returned HTTP 200.
- Responsive visual check: Persian RTL authentication shell rendered at desktop and 390 × 844 mobile viewport without horizontal overflow; the temporary viewport override was reset afterward.

## Docker verification

- Backend image: `sami-backend:purchasing-release`
  - ID: `sha256:1671fcefd331a9cb31c187475b33094f3d6121f750bdf65e76a5cd36dcfe5864`
  - Platform: `linux/amd64`
  - Size: 161,411,637 bytes
- Frontend image: `sami-frontend:purchasing-release`
  - ID: `sha256:1c3bb1ade619a9618b4cf41edb0635ac409985b87d391d212781cd392e4475b7`
  - Platform: `linux/amd64`
  - Size: 23,997,761 bytes
- Both images were built from the repository's existing production Dockerfiles. The frontend build used `VITE_API_BASE_URL=/api`.

## Browser verification note

The in-app browser loaded and rendered the live application, including Persian RTL and the mobile breakpoint. Its localhost session reported a network error for Axios login even though the identical request through the same Vite proxy returned HTTP 200 from PowerShell and direct backend authentication also succeeded. Because the verifier would not retain an authenticated session, authenticated Purchasing-screen interaction could not be repeated in that browser surface. Backend/controller tests, the live proxy smoke test, TypeScript validation and both production image builds passed; no application change was made to work around this verifier-specific behavior.

## Known limitations

- Import intentionally accepts the documented Purchasing CSV structure and creates idempotent drafts; it does not infer undocumented spreadsheet layouts.
- The module does not introduce RFQ, accounts-payable or supplier-invoice aggregates because those are separate business domains and no approved repository contract currently assigns them to Purchasing.
