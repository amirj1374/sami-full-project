# Treasury, Purchase Payments, Automation and Market Sync completion report

## Scope

This delivery completes the requested repository-supported work for Treasury, Purchase Payment Requests, Automation notifications, and authorized Market Sync external sources. Live Easy Accounting API synchronization is intentionally outside this delivery because no official API contract was supplied.

## Treasury

- Added tenant-scoped account types, accounts, transaction types/statuses, categories, transactions, immutable movements, cheques, daily closings, audit records, module registration, permissions, and admin grants in Flyway V49.
- Transaction completion locks affected accounts in a deterministic order, enforces negative-balance policy, and records immutable balance-after movements.
- Cancellation of a completed transaction creates a reversal instead of changing historical movements.
- Cheque identity is isolated by tenant, direction, normalized bank, and cheque number.
- Audit snapshots contain serialized values; dashboard counts and every lookup/query use the trusted tenant context.
- Added responsive Persian/English `/treasury` frontend with permission-controlled actions and Rial-default amounts.

## Purchase Payment Requests

- Added Flyway V50 with tenant-scoped requests, configurable manager-approval policy, daily account/method limits, receipts, audit history, scheduler job, permissions, and menu registration.
- Employees create and see their own requests. Manager/accountant operations require dedicated permissions and never accept a user or tenant identity as scope authority.
- Rejection requires a reason. Payments support multiple receipts and partial completion.
- Daily capacity is exposed without account balance/history and locked during payment processing to prevent concurrent over-consumption.
- Every payment is posted through Treasury, which remains the single owner of account movements.
- Shared Scheduler creates idempotent reminders, marks overdue requests, and writes through Notification Center.
- Added responsive Persian/English `/purchase-payments` frontend for request, decision, payment, and daily-limit workflows.

## Automation

- Existing rule CRUD, lifecycle, trigger/action registries, AND/OR conditions, execution policy, logs, retry/failure monitoring, manual execution, scheduler integration, configuration import/export, CSV report, permissions, audit, and responsive frontend were retained.
- Replaced the previous log-only `notify` action with the existing Notification Center service.
- Notification recipients can be the triggering actor or an explicit user id; tenant ownership and login eligibility are verified server-side.
- Notification writes use deterministic idempotency keys and do not deliver directly from business modules.

## Market Sync external sources

- Existing source/profile/rule configuration, scheduler, normalized product identity, inventory public API, pricing history, health/runs/errors, publication rules, sale lock, conflict resolution, and responsive frontend were retained.
- The two Rond sources remain seeded but disabled until an authorized structured contract is supplied. No scraping or access bypass was added.
- `STRUCTURED_JSON_V1` supports authorized HTTPS JSON endpoints, bounded response size, timeouts, and optional credential injection by environment-variable reference.
- Secrets are rejected from persisted source configuration and are not returned by source-list responses.
- Enabled endpoints must be public HTTPS endpoints; localhost/loopback endpoints are rejected.

## Validation evidence

- Backend `mvn clean verify`: **PASS**, 272 tests, 0 failures, 0 errors, 0 skipped.
- Frontend tests: **PASS**, 33 tests.
- Frontend type-check: **PASS**.
- Frontend production build: **PASS**, 1,076 modules transformed.
- Localization parity: **PASS** as part of frontend contract tests.
- PostgreSQL 16 / Flyway V1→V50 / Hibernate: **PASS** on a fresh disposable database.
- Production-like Docker health: **PASS** for database, backend and frontend; nginx `/` and authenticated API login/menu also passed.
- Real API smoke: **PASS** for Treasury account creation, Purchase Payment approval/payment, one persisted receipt and Treasury movement, Automation Notification Center delivery, and a disabled structured Market Sync source with hidden credentials.
- A runtime `Instant` JDBC binding failure found during the first payment attempt was fixed by binding a SQL timestamp; rollback left no partial Treasury movement, focused regression tests passed, and the same payment then completed successfully.
- Browser rendering: Persian RTL login rendered. Authenticated browser navigation is a **tooling follow-up** because the in-app browser blocked localhost `/api` with `ERR_BLOCKED_BY_CLIENT`, while curl through the same nginx endpoint passed. Exact 360/375/390/412/430 emulation was also unavailable because the tool enforced a 465 px minimum viewport.
- Deployment automation tests: **PASS**, 11/11.
- Sensitive-input scan and `git diff --check`: **PASS**; no customer spreadsheets, archives, environment files, credentials, dumps or image TARs are tracked by this delivery.

## Docker validation cleanup

- Created for validation: PostgreSQL, backend and frontend containers under the unique `sami-final-modules-validation` Compose project.
- Removed at completion: those three containers and the project network.
- Removed at completion: only the project's explicitly disposable database/config/upload/log volumes.
- Preserved: unrelated Docker resources and the two locally validated test images.

## Known external limitation

`moshtarekin123.rond.ir` and `samistore.rond.ir` cannot be enabled safely until their owner provides the official endpoint, response schema, authentication method, rate limits, and authorization to consume the data. The generic authorized adapter is ready; provider-specific scraping is deliberately not implemented.
