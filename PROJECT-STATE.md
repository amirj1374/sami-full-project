# SAMI ERP Project State

This file is durable execution memory. It is not business truth, architecture
authority, a roadmap replacement, or a historical narrative. Reconcile it with
repository reality on every resume.

## State Metadata

- **State Version:** 1
- **Last Reconciled:** 2026-09-23
- **Repository Branch:** `development`
- **Recorded HEAD:** `ba0e317758a1edf5b8535516c185309b86a39276`
- **State Status:** ACTIVE — Step 6 implementation in progress

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

- **Authorization ID:** `AUTH-P3-S6-SERIAL-CUSTODY`
- **Scope Type:** FEATURE
- **Scope:** Phase 3 Step 6 — Variant-aware Serial/IMEI custody and integrity
- **Objective:** Complete the already-started Step 6 implementation and its
  acceptance, regression, Guardian, state-update, and permitted commit/push
  gates.
- **Status:** ACTIVE
- **Allowed Change Area:** Step 6 serial/IMEI custody production code, V71,
  directly related tests, validation evidence, and agent-system documentation
  explicitly authorized by the current infrastructure work.
- **Forbidden Change Area:** Step 7; unrelated frontend/documentation work;
  business-rule redesign; architecture redesign; release/deployment; changing
  accepted Step 5 behavior without regression evidence.
- **Commit Permission:** Not yet exercised; only after Step 6 DoD gates pass.
- **Push Permission:** Not yet exercised; only after accepted certification and
  repository-policy checks.
- **Release Permission:** NOT AUTHORIZED.
- **Stop Conditions:** DoD complete, genuine Owner decision, required approval,
  unsafe continuation, explicit interruption, or scope boundary.

The repository documentation/agent-system changes in this phase are separately
authorized and do not expand the Step 6 application scope.

## Current Phase / Wave / Feature

- **Phase:** Phase 3 — Inventory
- **Step:** Step 6 — Serial/IMEI custody and integrity
- **Feature:** Variant-aware serialized custody while preserving Product-only
  legacy records and HAMTA history

## Accepted Baseline

- Step 5 accepted commit: `ba0e317 feat(inventory): add variant reservations and backorders`
- `origin/development` matches the recorded baseline (`0/0` at reconciliation).
- Step 5 PostgreSQL acceptance and Step 4 regression are recorded as accepted
  in repository execution history; current Step 6 work must not reopen them
  without regression evidence.

## Active Contracts

- `docs/agent-system/contracts/AUTH-P3-S6-SERIAL-CUSTODY.md` — ACTIVE
- Run: `docs/agent-system/runs/RUN-P3-S6-001.md`

## Completed Contracts

NONE.

## Completed Foundations

- V66–V70 Variant, UOM, inventory provenance, identity, reservation, and
  backorder foundations are committed in the accepted baseline.
- Existing SAMI governance and Definition of Ready/Done documents are present.

## Work In Progress

- Uncommitted `V71__variant_serial_custody.sql`.
- Uncommitted Inventory serial/transfer service and DTO changes for Variant
  identity, receipt, reservation lookup, release restoration, and transfer.
- Uncommitted `InventorySerialVariantPostgresAcceptanceIT.java`.
- Step 6 lifecycle acceptance is not yet accepted.

## Working Tree Preservation

### Authorized active changes

- Step 6 backend inventory changes listed above.
- V71 migration and Step 6 acceptance test.
- This phase's `AGENTS.md`, `PROJECT-STATE.md`, `docs/agent-system/AGENT-MODEL.md`,
  and index/governance discoverability changes.

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

## Known Failures

An earlier expanded acceptance attempt used a non-empty external database and
was invalid; it was corrected by recreating a disposable empty database. The
current clean run is the governing evidence.

## Blockers

- **OWNER BLOCKER:** None known.
- **TECHNICAL BLOCKER:** None proven.
- **ENVIRONMENT BLOCKER:** None for the completed evidence.
- **NO BLOCKER:** Guardian review and authorized Git closure remain in scope.

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

Step 6 technical validation is complete on a genuinely empty disposable
PostgreSQL database. Complete the independent Guardian/DoD review and then
perform the authorized Step 6 commit/push closure. Do not begin Step 7,
release, or deployment.

## Resume Instructions

Read `AGENTS.md`, this file, `docs/agent-system/GOVERNANCE.md`, and the
authoritative sources. Verify branch/HEAD/upstream/status, inspect V71 and
Step 6 source/tests, and classify any differences before editing. Preserve all
unrelated dirty changes. Ordinary failures route internally; update this state
at meaningful boundaries.

## State Reconciliation Notes

Recorded facts were reconciled against the current branch, HEAD, upstream,
working tree, V71, Step 6 source, and acceptance test. Transient external
database evidence is explicitly marked as environment-blocked rather than
treated as a production result.
