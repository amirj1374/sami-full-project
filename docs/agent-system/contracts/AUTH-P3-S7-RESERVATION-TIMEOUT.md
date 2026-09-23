# Contract: Phase 3 Step 7 Reservation Timeout

- **Contract ID:** `CONTRACT-P3-S7-RESERVATION-TIMEOUT`
- **Authorization ID:** `AUTH-P3-S7-RESERVATION-TIMEOUT`
- **Parent Phase/Wave/Feature:** Phase 3 / Inventory / P3-S7
- **Objective:** Complete the approved configurable reservation timeout and
  expiry lifecycle using the existing reservation model.
- **Business Outcome:** Reservations expire according to approved policy,
  release their reserved effect exactly once, and remain tenant-safe and
  auditable.
- **Scope:** `inventory_reservations.expires_at`, timeout configuration,
  expiry/release service path, scheduler integration if required, PostgreSQL
  acceptance and regression evidence.
- **Out of Scope:** New business rules, new reservation types, Step P3-S8,
  Phase 4, release/deployment, unrelated frontend/docs work.
- **Authoritative Sources:** `docs/IMPLEMENTATION_ROADMAP.md`, BR-INV-009 in
  `docs/SAMI_ERP_BUSINESS_RULES.md`, architecture constitution, governance,
  existing Inventory services and V66–V71 tests.
- **Acceptance Criteria:** 30-minute default; configurable timeout; expiry
  releases active reservation exactly once; physical stock is unchanged;
  reserved projection returns to zero; tenant and concurrency integrity hold;
  existing P3-S4–S6 acceptance remains green.
- **Dependencies:** P3-S5 reservation/backorder; existing scheduler and
  configuration conventions.
- **Allowed Change Area:** Inventory timeout implementation, directly related
  migration/configuration/tests and execution evidence.
- **Forbidden Changes:** Business-rule invention, weakening constraints,
  historical rewrite, Phase 4/P3-S8 work, release/deploy, unrelated changes.
- **Primary Agent:** Lead / Orchestrator
- **Supporting Agents:** Backend; Data & Integrity; QA & Scenario; Guardian
- **Validation Required:** compile, focused unit tests, clean external
  PostgreSQL acceptance, P3-S4–S6 regression, full backend regression.
- **Guardian Required:** Yes.
- **Completion Conditions:** All applicable acceptance and regression gates
  green, Guardian PASS, state/run updated, scoped commit/push complete.
- **Status:** `COMPLETED`
