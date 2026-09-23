# SAMI ERP Project State

This file is durable execution memory. It is not business truth, architecture
authority, a roadmap replacement, or a historical narrative. Reconcile it with
repository reality on every resume.

## State Metadata

- **State Version:** 1
- **Last Reconciled:** 2026-09-23
- **Repository Branch:** `development`
- **Recorded HEAD:** `d621e19`
- **State Status:** Phase 3 COMPLETE/ACCEPTED — Phase 4 AUTHORIZED; P4-S1 blocked by HIGH-009

## Authority References

- Business truth: `docs/SAMI_ERP_BUSINESS_RULES.md`
- Architecture: `docs/SAMI_ERP_ARCHITECTURE_CONSTITUTION.md`, `docs/adr/`
- Authority index: `docs/PROJECT_INDEX.md`
- Roadmap: `docs/IMPLEMENTATION_ROADMAP.md`
- Open decisions: `docs/24-roadmap-and-open-decisions.md`
- Agent governance: `docs/agent-system/GOVERNANCE.md`
- Agent roles: `docs/agent-system/AGENT-MODEL.md`
- Validation gates: `docs/15-testing-and-quality.md`

## Current Authorization

- **Authorization ID:** `AUTH-P4-PHASE4-ACCOUNTING`
- **Scope Type:** ROADMAP
- **Scope:** Phase 4 — Accounting, per `docs/IMPLEMENTATION_ROADMAP.md` and `docs/agent-system/plans/PHASE-4-EXECUTION-PLAN.md`
- **Objective:** Execute the approved Phase 4 Accounting plan through applicable
  DoD without inventing unresolved finance policy.
- **Status:** ACTIVE — first candidate P4-S1 BLOCKED by HIGH-009
- **Allowed Change Area:** Phase 4 planning, approved Accounting implementation, tests, migrations, contracts, evidence and state artifacts.
- **Forbidden Change Area:** Phase 5; unrelated
  frontend/documentation work;
  business-rule redesign; architecture redesign; release/deployment; changing
  accepted Step 5 behavior without regression evidence.
- **Commit Permission:** Authorized for Phase 4 artifacts only after applicable
  DoD gates and scope review pass.
- **Push Permission:** Authorized for accepted Phase 4 artifacts after
  repository-policy checks.
- **Release Permission:** NOT AUTHORIZED.
- **Stop Conditions:** DoD complete, genuine Owner decision, required approval,
  unsafe continuation, explicit interruption, or scope boundary.

The repository documentation/agent-system changes in this phase are separately
authorized and do not expand the Step 6 application scope.

## Current Phase / Wave / Feature

- **Phase:** Phase 4 — Accounting
- **Step:** P4-S1 — Accounting boundary and policy decision closure (blocked)
- **Feature:** Canonical Accounting foundation and posting contract

## Accepted Baseline

- Step 5 accepted commit: `ba0e317 feat(inventory): add variant reservations and backorders`
- `origin/development` matches the recorded HEAD (`0/0` at reconciliation).
- Phase 3 is accepted at `d621e19`; Phase 4 work is planning-only until
  HIGH-009 is resolved. Existing unrelated dirty changes are not part of the
  accepted baseline.

## Active Contracts

- `docs/agent-system/contracts/AUTH-P4-S1-ACCOUNTING-BOUNDARY.md` — BLOCKED
- Run: `docs/agent-system/runs/RUN-P4-S1-001.md`
- `docs/agent-system/contracts/AUTH-P3-S7-RESERVATION-TIMEOUT.md` — COMPLETED
- Run: `docs/agent-system/runs/RUN-P3-S7-001.md`
- `docs/agent-system/contracts/AUTH-P3-S9-PHASE3-CERTIFICATION.md` — COMPLETED

## Completed Contracts

- `CONTRACT-P3-S6-SERIAL-CUSTODY` — completed; run
  `docs/agent-system/runs/RUN-P3-S6-001.md` is green.
- `CONTRACT-P3-S7-RESERVATION-TIMEOUT` — completed; run
  `docs/agent-system/runs/RUN-P3-S7-001.md` is green.
- `CONTRACT-P3-S8-CROSS-MODULE-VARIANT-UOM` — completed; run
  `docs/agent-system/runs/RUN-P3-S8-001.md` is green.
- `CONTRACT-P3-S9-PHASE3-CERTIFICATION` — completed; run
  `docs/agent-system/runs/RUN-P3-S9-001.md` is green.

## Completed Foundations

- V66–V70 Variant, UOM, inventory provenance, identity, reservation, and
  backorder foundations are committed in the accepted baseline.
- Existing SAMI governance and Definition of Ready/Done documents are present.

## Work In Progress

- Phase 3 final certification is complete. Phase 4 plan and P4-S1 boundary
  Contract are established; P4-S1 is blocked pending HIGH-009.

## Working Tree Preservation

### Authorized active changes

- Phase 4 plan, P4-S1 Contract, and P4-S1 run artifact.

### Unrelated pre-existing changes to preserve

- `docs/business/SALES_USER_WORKFLOW_FA.md`
- `docs/business/SALES_WORKFLOW_TRACE.md`
- Existing Sales frontend components, services, locales, types, and tests
  shown by `git status`.

## Validation Status

- **PASS:** Step 5 accepted baseline; historical PostgreSQL acceptance and
  regression evidence recorded at the accepted revision.
- **PASS:** Backend compilation previously passed for current Step 6 work.
- **PASS:** Documentation validation before this state update.
- **PASS:** Step 6 acceptance on clean PostgreSQL: 5 tests, 0 failures,
  0 errors.
- **PASS:** Fresh V1→V71 and supported V50→V71 migration acceptance: 2 tests,
  0 failures, 0 errors.
- **PASS:** Step 4 + Step 5 regression on V71: 13 tests, 0 failures,
  0 errors.
- **PASS:** Full backend regression: 310 tests, 0 failures, 0 errors.
- **PASS:** Final fresh combined PostgreSQL acceptance: 18 tests, 0 failures,
  0 errors.
- **PASS:** P3-S7 final fresh combined PostgreSQL acceptance: 14 tests, 0
  failures, 0 errors (Step 4: 1; Step 5: 13).
- **PASS:** P3-S8 fresh V1→V72 PostgreSQL acceptance: 14 tests, 0 failures,
  0 errors (Step 4: 1; Step 5: 13).
- **PASS:** Frontend type-check and 56 frontend tests.
- **PASS:** Full backend regression after P3-S8: 310 tests, 0 failures,
  0 errors.
- **PASS:** P3-S9 final fresh PostgreSQL certification: 21 tests, 0 failures,
  0 errors (migration 2, Step 4 1, Step 5 13, Step 6 5).
- **PASS:** Supported V50→V72 upgrade and fresh V1→V72 migration with no
  checksum errors.
- **PASS:** Phase 3 Guardian closure recorded in `RUN-P3-S9-001.md`.

## Known Failures

Earlier non-empty external-database evidence was discarded. The governing
P3-S7 evidence is the final disposable empty PostgreSQL database run recorded
in `RUN-P3-S7-001.md`.

## Blockers

- **OWNER BLOCKER:** HIGH-009 requires approved invoice/payment/return,
  fiscal-period, tax and accounting-boundary decisions before dependent
  Accounting behavior can be encoded safely.
- **TECHNICAL BLOCKER:** None proven.
- **ENVIRONMENT BLOCKER:** None for the completed evidence.
- **TECHNICAL BLOCKER:** None proven; independent planning is complete.

## Environment Limitations

- Testcontainers inside Maven is not the approved path for this work.
- External PostgreSQL 16 is the intended acceptance path.
- Docker/Compose and other workstation limitations are documented in the
  canonical handoff files and must be rechecked before relying on them.

## Open TBDs

See `docs/24-roadmap-and-open-decisions.md`. Agents must not duplicate or
silently resolve those decisions here.

## Relevant Approved Decisions

- Inventory owns stock, movement, reservation, and serial custody.
- Legacy Product-only records remain valid and are not silently reassigned.
- Variant identity is tenant- and Product-scoped.
- Flyway migrations are forward-only.
- Release/deployment requires separate authorization.

## Technical Debt

- V1 orchestrator artifacts are lightweight and intentionally consolidated in
  the active Contract and run artifact; a full Skill library is not yet built.
- Step 6 acceptance evidence is green on the current disposable PostgreSQL
  run, with final Guardian/DoD/Git closure recorded in the run artifact.

## Next Authorized Action

Resolve the minimum HIGH-009 Product/Architecture decisions required by
`AUTH-P4-S1-ACCOUNTING-BOUNDARY`; then activate P4-S2 only after P4-S1 is
approved and complete. Independent planning is complete; no speculative
Accounting implementation is authorized.

## Resume Instructions

Read `AGENTS.md`, this file, `docs/agent-system/GOVERNANCE.md`, and the
authoritative sources. Verify branch/HEAD/upstream/status, inspect V71 and
Step 6 source/tests, and classify any differences before editing. Preserve all
unrelated dirty changes. Ordinary failures route internally; update this state
at meaningful boundaries.

## State Reconciliation Notes

Recorded facts were reconciled against the current branch, HEAD, upstream,
working tree, V71, P3-S7 source, acceptance tests, Contract and run evidence.
The external PostgreSQL databases used for acceptance were disposable and
isolated by database name; no repository fixture was left modified.
