# Contract AUTH-P6-S2-REPORT-RECONCILIATION-BASELINE

- **Authorization ID:** `AUTH-P6-PHASE6-REPORTS-INTEGRATIONS-MIGRATION`
- **Parent Phase:** Phase 6 — Reports / Integrations / Migration.
- **Objective:** Reconcile and certify existing executable domain/module report
  totals, scope and permissions without creating new report or KPI semantics.
- **Scope:** Existing Sales, Purchasing, Inventory and Accounting report APIs;
  Dashboard/KPI provider plumbing where it exposes existing repository facts;
  focused backend tests and real PostgreSQL reconciliation where persistence
  semantics require it.
- **Out of Scope:** New unified report catalog, new KPI meanings, redesigned
  dashboards, Legacy Asan, Market Sync, statutory reports, release/deployment.
- **Authoritative Sources:** Roadmap Phase 6; Business Rules; Architecture
  Constitution; module catalog; existing API/UI contracts; P6-S1 plan/run.
- **Applicable Acceptance Criteria:** Existing totals equal owned source facts;
  tenant/company/branch filters cannot expose another scope; report permission
  guards remain enforced; dashboard provider failures remain contained.
- **Dependencies:** P6-S1 complete at `226dddf`.
- **Inputs / Existing Interfaces:** `/api/v1/sales/reports`,
  `/api/v1/purchases/reports`, `/api/v1/inventory/reports`,
  `/api/v1/accounting/reports/journals`, Dashboard `ReportingProvider` SPI.
- **Expected Outputs:** Focused regression protecting existing semantics,
  corrected deterministic scope defects if found, reconciliation evidence.
- **Allowed Change Area:** Existing report query/services/repositories,
  report-focused tests, Contract/run/state evidence.
- **Forbidden Changes:** New measures or definitions; cross-domain direct
  persistence; migration changes unless an executable schema gap is proven.
- **Supporting Agents:** Backend, Contract Validator, Data & Integrity, QA,
  Guardian; frontend/UI only if rendered behavior changes.
- **Dependency / Parallelism Plan:** Audit domain services and frontend
  contracts in parallel; serialize shared dashboard repository/service fixes.
- **Validation Required:** Focused unit/contract tests; real PostgreSQL report
  reconciliation and isolation where practical; relevant regression;
  documentation and `git diff --check`.
- **Guardian Required:** Yes.
- **Blocking TBDs:** None for existing-report certification. P6-D1 remains
  isolated to P6-S5.
- **Completion Conditions:** Existing report semantics reconcile and scope
  safely, focused/integration gates pass, Guardian passes, state/Git close.
- **Status:** COMPLETED WITH BROWSER-HARNESS LIMITATION.
