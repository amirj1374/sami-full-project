# DEC-P6-001 — Phase 6 Reporting and Asan Promotion Inputs

- **Status:** PROPOSED
- **Date:** 2026-09-28
- **Scope:** P6-S5 new reporting and P6-S6 final Legacy Asan promotion.
- **Decision:** Pending Owner selection. No default is inferred.
- **Authority:** Product Owner.
- **Supersedes:** None.
- **Superseded By:** None.

## Decision 1 — Reporting outcome

- Approve a named initial cross-domain report set with exact measures,
  dimensions/date semantics and role/company/branch visibility; SAMI will build
  and reconcile those reports through domain-owned read contracts.
- Or limit Phase 6 to certifying and correcting existing domain/module reports;
  no new unified analytics workflow will be added in this phase.

## Decision 2 — Final Asan promotion

- Approve a reviewed mapping/acceptance package for a named migration group,
  including legacy account/entity/warehouse/person mappings, accepted
  exceptions, effective dates/opening balances, cutover timing and acceptance
  signer; SAMI can then implement controlled, idempotent canonical promotion.
- Or keep Legacy Asan staging/reconciliation-only; evidence remains safe and
  reviewable, but Final Import cannot reach Phase 6 DoD.

## Constraints regardless of choice

No fuzzy identity matching, direct cross-domain persistence, silent balancing,
credential exposure, destructive rollback, or canonical write before explicit
acceptance is permitted.
