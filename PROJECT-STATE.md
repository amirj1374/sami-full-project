# SAMI ERP Project State

This file is durable execution memory. It is not business truth, architecture
authority, a roadmap replacement, or a historical narrative. Reconcile it with
repository reality on every resume.

## State Metadata

- **State Version:** 1
- **Last Reconciled:** 2026-09-24
- **Repository Branch:** `development`
- **Recorded HEAD:** `842390b` (`fix(crm): close phase 5 validation defects`)
- **State Status:** Phase 3 COMPLETE/ACCEPTED — Phase 4 COMPLETE/ACCEPTED — Phase 5 ACTIVE / validation evidence substantially green

## Authority References

- Business truth: `docs/SAMI_ERP_BUSINESS_RULES.md`
- Architecture: `docs/SAMI_ERP_ARCHITECTURE_CONSTITUTION.md`, `docs/adr/`
- Authority index: `docs/PROJECT_INDEX.md`
- Roadmap: `docs/IMPLEMENTATION_ROADMAP.md`
- Open decisions: `docs/24-roadmap-and-open-decisions.md`
- Agent governance: `docs/agent-system/GOVERNANCE.md`
- Agent roles: `docs/agent-system/AGENT-MODEL.md`
- Validation gates: `docs/15-testing-and-quality.md`

## Completed Authorization

- **Authorization ID:** `AUTH-P4-PHASE4-ACCOUNTING`
- **Scope Type:** ROADMAP
- **Scope:** Phase 4 — Accounting, per `docs/IMPLEMENTATION_ROADMAP.md` and `docs/agent-system/plans/PHASE-4-EXECUTION-PLAN.md`
- **Objective:** Execute the approved Phase 4 Accounting plan through applicable
  DoD without inventing unresolved finance policy.
- **Status:** COMPLETED — Phase 4 certified and closed
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

- **Phase:** Phase 5 — CRM Intelligence
- **Step:** P5-S5 — user-facing follow-up/intelligence workspace (active)
- **Feature:** Lead, Opportunity, Contact history and follow-up

## Accepted Baseline

- Step 5 accepted commit: `ba0e317 feat(inventory): add variant reservations and backorders`
- `origin/development` matches the recorded HEAD (`0/0` at reconciliation).
- Phase 3 is accepted at `d621e19`. Phase 4 is accepted at `5080c94`.
  Phase 5 planning is active from the authorized CRM scope.
  Existing unrelated dirty changes are not part of the accepted baseline.

## Active Contracts

- `docs/agent-system/contracts/AUTH-P4-S1-ACCOUNTING-BOUNDARY.md` — COMPLETED
- Run: `docs/agent-system/runs/RUN-P4-S1-001.md`
- `docs/agent-system/contracts/AUTH-P4-S2-ACCOUNTING-FOUNDATION.md` — COMPLETED
- Run: `docs/agent-system/runs/RUN-P4-S2-001.md`
- P4-S2 is now complete: fresh V1→V73 and supported V72→V73 migration
  validation passed; Guardian review passed.
- `docs/agent-system/contracts/AUTH-P4-S3-JOURNAL-GL-FOUNDATION.md` — COMPLETED
- Run: `docs/agent-system/runs/RUN-P4-S3-001.md`
- `docs/agent-system/contracts/AUTH-P4-S4-AR-AP-INTEGRATION.md` — COMPLETED
- Run: `docs/agent-system/runs/RUN-P4-S4-001.md`
- `docs/agent-system/contracts/AUTH-P4-S5-TREASURY-POSTING.md` — COMPLETED
- Run: `docs/agent-system/runs/RUN-P4-S5-001.md`
- `docs/agent-system/contracts/AUTH-P4-S6-TAX-POLICY.md` — COMPLETED
- Run: `docs/agent-system/runs/RUN-P4-S6-001.md`
- `docs/agent-system/contracts/AUTH-P4-S7-ACCOUNTING-REPORTS-UI.md` — COMPLETED WITH UI LIMITATION
- Run: `docs/agent-system/runs/RUN-P4-S7-001.md`
- `docs/agent-system/contracts/AUTH-P4-S8-PHASE4-CERTIFICATION.md` — COMPLETED
- Run: `docs/agent-system/runs/RUN-P4-S8-001.md`
- P4-S3 and P4-S4 completion are continuation checkpoints under the active
  Phase authorization; P4-S5 must be activated and executed without yielding
  an intermediate completion report.
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

- Phase 3 final certification is complete. P4-S1 through P4-S8 are complete;
  Phase 4 Accounting is COMPLETE/ACCEPTED. No Phase 4 work remains active.
- P5-S1/P5-S2/P5-S4 implementation is present and validation evidence is green;
  P5-S3/P5-S5/P5-S6 remain to be reconciled and executed under the active Phase
  authorization.

## Working Tree Preservation

### Authorized active changes

- Phase 5 implementation, acceptance fixture correction, Contracts/runs and
  validation evidence are authorized active changes.

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
- **PASS:** Docker Maven clean test-compile after Phase 5 fixes.
- **PASS:** Fresh V1→V79 and supported V50→V79 migration acceptance: 2 tests,
  0 failures, 0 errors.
- **PASS:** CRM PostgreSQL acceptance: 2 tests, 0 failures, 0 errors, using
  external PostgreSQL 16.15; cross-tenant fixture and tenant identity are
  explicit.
- **PASS:** Focused CRM regression: 6 tests, 0 failures, 0 errors.
- **PASS:** Full backend regression: 311 tests, 0 failures, 0 errors.
- **PASS:** P5-S5 frontend type-check, production build, and 56 frontend tests.
- **PENDING:** P5-S5 rendered authenticated UI Quality gate; only login shell was
  rendered because no authorized development backend/session was available.
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
- **PASS:** V73 additive SQL applied with `ON_ERROR_STOP=1` on disposable
  PostgreSQL 16 database `sami_p4s2_v73`; V72 Flyway history was preserved and
  all three foundation tables/indexes were created.
- **PASS:** Docker Maven test compilation after the P4-S3 posting boundary.
- **PASS:** Fresh V1→V74 migration on disposable PostgreSQL 16.15.
- **PASS:** Supported V72→V74 migration on disposable PostgreSQL 16.15.
- **PASS:** P4-S4 focused AR/AP validation: 11 tests, 0 failures, 0 errors.
- **PASS:** P4-S5 Docker Maven test compilation after Treasury posting changes.
- **PASS:** P4-S5 `TreasuryContractTest`: 2 tests, 0 failures, 0 errors.
- **PASS:** V75 Treasury mapping/posting SQL applied with ON_ERROR_STOP=1 to
  disposable PostgreSQL 16 database `sami_p4s5_v75`; mapping and posting tables
  were created successfully.
- **PASS:** P4-S6 Docker Maven test compilation after tax-policy service.
- **PASS:** V76 tax-policy SQL applied with ON_ERROR_STOP=1 to disposable
  PostgreSQL 16 database `sami_p4s6_v76`.
- **PASS:** Final backend Maven regression: 310 tests, 0 failures, 0 errors,
  0 skipped.
- **PASS:** Authenticated application startup on PostgreSQL 16.15 validated
  Flyway through V76; Accounting reports route rendered under bootstrap admin.
- **PASS:** Full backend regression in a repository-root-mounted Maven
  container: 310 tests, 0 failures, 0 errors. The earlier missing
  `../sami-frontend/vite.config.ts` error was a test-harness mount defect, not
  a product regression; mounting the repository root corrected the path.
- **PASS:** Phase 3 Guardian closure recorded in `RUN-P3-S9-001.md`.

## Known Failures

- None in final Phase 4 certification. The external acceptance fixture path
  passed all permanent PostgreSQL assertions.

Earlier non-empty external-database evidence was discarded. The governing
P3-S7 evidence is the final disposable empty PostgreSQL database run recorded
in `RUN-P3-S7-001.md`.

## Blockers

- **OWNER BLOCKER:** None proven after DEC-P4-001.
- **OWNER BLOCKER:** None. DEC-P5-001 resolves the former CRM intelligence
  policy blocker.
- **TECHNICAL STATUS:** P5-S1/P5-S2/P5-S4 validation gates recorded green;
    Phase 5 remains unaccepted pending remaining plan steps and final Guardian.
- **Checkpoint:** `a09953f chore(crm): persist phase 5 validation checkpoint` is
    pushed to `origin/development`; subsequent fixes are uncommitted.
- **Validation environment:** external PostgreSQL 16.15 and repository-mounted
    Maven container are available and were used; Testcontainers is not required.
- **Decision:** `docs/decisions/DEC-P5-001-crm-intelligence-policy.md` approved;
  P5-S2 onward may proceed within its bounded scope.
- **TECHNICAL BLOCKER:** None proven in V73 SQL.
- **ENVIRONMENT BLOCKER:** None for the completed evidence.
- **TECHNICAL BLOCKER:** None proven; independent planning is complete.
- **OWNER BLOCKER:** None; DEC-P4-002 resolves the Treasury-to-Accounting
  boundary.

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

## Current Authorization

- **Authorization ID:** `AUTH-P5-PHASE5-CRM`
- **Scope Type:** ROADMAP / PHASE
- **Scope:** Phase 5 — CRM Intelligence
- **Status:** ACTIVE — planning and independent foundation sequencing
- **Release Permission:** NOT AUTHORIZED

## Next Authorized Action

 Continue P5-S5 rendered authenticated UI Quality inspection, then P5-S6 final
 certification. P5-S3 history/reporting is covered by the existing append-only
 customer timeline and workflow outcome evidence and must be explicitly
 reconciled in its Contract.

## Agent-System Retrospective

Phase 4 retrospective: `docs/agent-system/retrospectives/PHASE-4-EXECUTION-RETROSPECTIVE.md`.
No generic framework correction was required; the external PostgreSQL fixture
path and continuation/checkpoint protections are recorded in existing artifacts.
Phase 5 is AUTHORIZED and ACTIVE. Phase 6 remains NOT STARTED / NOT AUTHORIZED.

## Resume Instructions

1. Read `docs/agent-system/plans/PHASE-5-EXECUTION-PLAN.md`,
   `docs/decisions/DEC-P5-001-crm-intelligence-policy.md`, and active P5
   Contracts/runs.
2. Reconcile P5-S1 evidence, then continue P5-S2 and later dependency-valid
   work without reopening completed foundations.
3. Continue only within active Phase 5 authorization; do not start Phase 6.

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
