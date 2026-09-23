# Phase 3 Inventory Execution Plan

This is a technical execution decomposition of the approved Phase 3 scope in
`docs/IMPLEMENTATION_ROADMAP.md`. It is not a business authority and does not
change Product Owner decisions.

## Objective

Complete the approved Inventory scope: Variant/UOM identity and quantity,
reservation/backorder behavior, movement integrity, Serial/IMEI custody, and
the required user-facing and cross-module validation while preserving legacy
Product-only data and tenant boundaries.

## Authority

- Business truth: `docs/SAMI_ERP_BUSINESS_RULES.md`
- Architecture: `docs/SAMI_ERP_ARCHITECTURE_CONSTITUTION.md`, `docs/adr/`
- Approved roadmap: `docs/IMPLEMENTATION_ROADMAP.md`
- Open decisions: `docs/24-roadmap-and-open-decisions.md`
- Current implementation catalog: `docs/05-module-catalog.md`
- Quality gates: `docs/15-testing-and-quality.md`

## Completed Foundations

| Step | Outcome | Evidence |
|---|---|---|
| P3-S1–S3 | Inventory foundation, movement and Product-only compatibility | V32 and existing Inventory services/tests |
| P3-S4 | Variant/UOM inventory provenance and balance identity | V66–V69; `InventoryVariantUomPostgresAcceptanceIT` |
| P3-S5 | Variant reservations, backorders, fulfillment and cancellation | V70; `InventoryReservationBackorderPostgresAcceptanceIT` |
| P3-S6 | Variant-aware Serial/IMEI custody and HAMTA preservation | V71; `InventorySerialVariantPostgresAcceptanceIT`; run `RUN-P3-S6-001` |

## Remaining Executable Steps

P3-S7, P3-S8 and P3-S9 are complete. No remaining Phase 3 executable Step is
authorized; Phase 4 requires a new authorization.

### P3-S7 — Reservation expiry and timeout lifecycle

- **Objective:** Implement the approved configurable reservation timeout (30
  minutes default), expiry/release behavior, idempotency, and persisted audit
  evidence using the existing reservation model.
- **Source:** BR-INV-009; roadmap reservation timeout criterion; existing
  `inventory_reservations.expires_at` schema and reservation services.
- **Dependencies:** P3-S5; existing scheduler/configuration conventions.
- **Agents:** Backend; Data & Integrity; QA; Guardian.
- **Validation:** real PostgreSQL expiry, release of reserved quantity, no
  duplicate release, configurable duration, tenant isolation, regression of
  P3-S4–S6.
- **Completion:** implementation, migration only if required, permanent
  PostgreSQL acceptance, focused/full regression, Guardian PASS, state and Git
  closure.

### P3-S8 — Cross-module Variant/UOM inventory integration and UI evidence

- **Objective:** Trace and complete approved Variant/UOM propagation through
  Purchasing receipt, Sales delivery/issue, Inventory monitoring and relevant
  API/frontend contracts where repository evidence shows a missing boundary.
- **Dependencies:** P3-S4–S7; stable public Inventory operations.
- **Agents:** Backend, Frontend, Contract Validator, QA, Data & Integrity,
  Guardian.
- **Validation:** persisted cross-module PostgreSQL journeys, API contract
  checks, UI contract/regression checks, tenant and identity isolation.
- **Completion:** only applicable gaps are implemented and certified; unsupported
  or Product-owned behavior is documented, not invented.

### P3-S9 — Phase 3 integration certification

- **Objective:** Run the final clean PostgreSQL and cross-module certification
  for the complete Phase 3 Inventory scope, including rollback, idempotency,
  concurrency, migration upgrade and regression evidence.
- **Dependencies:** P3-S7 and P3-S8.
- **Agents:** QA; Data & Integrity; Guardian; Lead.
- **Validation:** fresh and supported-upgrade migrations, all permanent
  acceptance suites, focused/full regression, final clean database.
- **Completion:** all applicable roadmap gates green and Phase 3 DoD recorded.

## Phase 3 Completion Criteria

All approved roadmap outcomes have executable implementation evidence, permanent
validation evidence, migration compatibility, tenant/concurrency integrity,
cross-module contract/UI evidence where applicable, independent Guardian PASS,
updated Project State, and synchronized authorized Git history. No Phase 4 or
release/deployment work is included.

## Current Candidate

`P3-S9` is complete. Phase 3 is COMPLETE/ACCEPTED; Phase 4 is not authorized.
