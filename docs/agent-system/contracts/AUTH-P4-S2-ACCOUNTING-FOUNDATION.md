# Contract AUTH-P4-S2-ACCOUNTING-FOUNDATION

- **Contract ID:** `AUTH-P4-S2-ACCOUNTING-FOUNDATION`
- **Authorization ID:** `AUTH-P4-PHASE4-ACCOUNTING`
- **Objective:** Add the minimal tenant/company/branch-scoped Accounting
  foundation: Chart of Accounts master data and configurable fiscal periods.
- **Business Outcome:** Authorized accounting users have durable account and
  open/closed period foundations without inventing journal, tax, provider or
  return behavior.
- **Scope:** Additive migration V73, constraints, indexes and audit events.
- **Out of Scope:** Journal/GL posting, tax rates, payment providers, returns,
  statutory integrations, Phase 5, release/deploy.
- **Authoritative Sources:** Phase 4 roadmap, BR-ACC-001–004,
  `DEC-P4-001-accounting-boundary.md`, architecture constitution.
- **Primary Agent:** Data & Integrity / Backend
- **Supporting Agents:** QA & Scenario, Guardian
- **Dependencies:** P4-S1 complete.
- **Validation Required:** SQL/migration review, compile, fresh and supported
  PostgreSQL migration checks when the approved environment is available.
- **Guardian Required:** Yes.
- **Completion Conditions:** Migration applies without checksum/schema errors;
  scope is tenant-safe and auditable; no legacy accounting evidence is
  promoted or rewritten.
- **Status:** ACTIVE — implementation present, runtime validation pending
