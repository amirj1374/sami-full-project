# Phase 6 Execution Plan — Reports / Integrations / Migration

This is a technical decomposition of the approved Phase 6 scope in
`docs/IMPLEMENTATION_ROADMAP.md`. It does not define report semantics, legacy
mapping policy, or third-party contracts.

## Objective

Complete the approved reporting, bounded-integration and Legacy Asan migration
work without bypassing domain owners, exposing credentials, inventing metrics,
or promoting staged legacy evidence before explicit acceptance.

## Dependency-ordered Steps

| Step | Objective | Dependencies | Validation / gate |
|---|---|---|---|
| P6-S1 | Reconcile Phase 6 authority, current capability, stale historical statements, deferred external contracts and blocking business inputs. | Certified Phase 5 and phone E2E baseline. | Evidence-based architecture audit, plan/Contract/run/state, Guardian. |
| P6-S2 | Certify existing domain/module report totals, scope and permissions; add no new KPI semantics. | P6-S1. | Focused API/PostgreSQL reconciliation and tenant/company/branch/RBAC tests; UI only if changed. |
| P6-S3 | Harden Legacy Asan staging, reconciliation and acceptance evidence while proving zero canonical writes. | P6-S1; existing V40/V45/V46 staging. | PostgreSQL provenance/idempotency/archive/negative paths, rendered UI, UI Quality and RUA. |
| P6-S4 | Certify existing opt-in structured Market Sync adapter and fail-closed publication boundaries. | P6-S1; existing V42 adapter. | Malformed/duplicate/collision/cost/tenant/idempotency/failure-path tests, rendered UI and applicable RUA. |
| P6-S5 | Implement any new cross-domain report/read model approved by the Owner. | P6-S2 and approved report catalog/semantics. | Domain-owner read contracts, PostgreSQL reconciliation, API/client/i18n/RTL/responsive UI, UI Quality/RUA as applicable. |
| P6-S6 | Implement final Asan promotion for an approved migration group through canonical public owners. | P6-S3 and approved mapping/acceptance/cutover package. | Dry-run, explicit acceptance, idempotent/concurrent PostgreSQL promotion, audit, compensation, RBAC and full rendered RUA. |
| P6-S7 | Final Phase 6 certification and closure. | P6-S2–S6 complete or explicitly scoped by approved Owner decisions. | Fresh/upgrade migrations, focused and final broad regression, contract validation, UI Quality/RUA, Guardian, state/Git closure. |

P6-S2, P6-S3 and P6-S4 are independent after P6-S1 and may proceed without
waiting for the P6-S5/P6-S6 business inputs. Shared migrations and frontend
surfaces remain serialized.

## Decisions and exclusions

- A new unified report is not authorized until its measures, dimensions, date
  semantics and visibility are approved. Existing domain reports may be
  reconciled without inventing new business meaning.
- Canonical Asan promotion remains fail-closed until a concrete mapping and
  acceptance/cutover package is approved for a named migration group.
- Official external Market Sync publication contracts, scraping, credentials
  and live publication connectors remain deferred backlog. Existing
  `STRUCTURED_JSON_V1` safety behavior may be certified.
- Phase 6 does not reopen accepted prior phases or the certified phone E2E
  unless an executable regression is found. Release/deployment is forbidden.

## Phase Definition of Done

Every in-scope approved outcome is implemented, validated and accepted;
report totals reconcile through owned domain contracts; staging provenance,
idempotency, archive controls and zero-write boundaries pass; authorized
integration failure paths fail closed; applicable browser/UI Quality/RUA gates
pass; Guardian finds no Critical/Major issue; and every Step has a durable
checkpoint and coherent commit/push.
