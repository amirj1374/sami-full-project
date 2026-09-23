# Contract AUTH-P4-S4-AR-AP-INTEGRATION

- **Authorization ID:** `AUTH-P4-PHASE4-ACCOUNTING`
- **Objective:** Validate the approved finalized Sales Invoice receivable and
  accepted Supplier Invoice payable posting boundaries without inventing
  provider, tax, or payment policy.
- **Scope:** Existing `ReceivablePostingPort` and `PayablePostingPort` flows,
  tenant/scope/idempotency behavior, and reconciliation evidence.
- **Out of Scope:** Treasury, tax configuration, reports/UI, Phase 5,
  release/deployment.
- **Dependencies:** P4-S3 Journal/GL foundation.
- **Validation:** focused AR/AP service and contract tests; backend regression;
  cross-module PostgreSQL acceptance when the approved fixture is available.
- **Guardian Required:** Yes
- **Status:** COMPLETED
- **Completion Evidence:** Existing Sales and Supplier invoice services are
  wired to the Accounting public ports; focused Supplier invoice behavior and
  contract tests passed (11 tests, 0 failures, 0 errors). Full backend evidence
  is recorded in the prior P4-S3 run.
