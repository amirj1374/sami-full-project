# Contract AUTH-P4-S1-ACCOUNTING-BOUNDARY

- **Contract ID:** `AUTH-P4-S1-ACCOUNTING-BOUNDARY`
- **Authorization ID:** `AUTH-P4-PHASE4-ACCOUNTING`
- **Parent Phase:** Phase 4 — Accounting
- **Objective:** Establish approved accounting boundary and policy decisions
  needed to safely activate the Accounting implementation sequence.
- **Scope:** Decision/authority closure only; no speculative schema, posting,
  tax, payment or return implementation.
- **Out of Scope:** Phase 5, release/deployment, unrelated worktree changes.
- **Authoritative Sources:** `docs/IMPLEMENTATION_ROADMAP.md` Phase 4,
  `docs/SAMI_ERP_BUSINESS_RULES.md` BR-ACC-001–004,
  `docs/SAMI_ERP_ARCHITECTURE_CONSTITUTION.md`, HIGH-009.
- **Primary Agent:** Lead / Orchestrator
- **Supporting Agents:** Product & UX, Backend, Data & Integrity, Guardian
- **Dependencies:** None technical; HIGH-009 business decision is blocking.
- **Validation Required:** Authority traceability and independent Guardian
  review of the Phase 4 plan.
- **Completion Conditions:** Approved decision record resolves HIGH-009's
  dependent boundary questions, or explicitly scopes them out.
- **Status:** COMPLETED — DECISION RECORDED

## Blocking TBDs

The blocking fiscal-period and tax decisions are resolved by
`docs/decisions/DEC-P4-001-accounting-boundary.md`. Remaining implementation
details are delegated to technical Agents within approved architecture.

## Forbidden Actions

Do not invent policy, encode tax rates, choose payment providers, define return
effects, or create canonical journals before the approved boundary exists.
