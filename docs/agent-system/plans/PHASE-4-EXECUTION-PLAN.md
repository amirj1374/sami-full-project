# Phase 4 Execution Plan — Accounting

This is a technical execution decomposition of the approved Phase 4 scope in
[`docs/IMPLEMENTATION_ROADMAP.md`](../../IMPLEMENTATION_ROADMAP.md). It does
not define or supersede SAMI business truth.

## Objective

Establish tenant/company-scoped, auditable canonical Accounting: chart of
accounts, fiscal periods, immutable balanced journals/GL, approved posting
contracts for finalized domain facts, finance-policy configuration, reports and
UI, with forward-only migration and reconciliation evidence.

## Authority

- Business requirements: `docs/SAMI_ERP_BUSINESS_RULES.md` (BR-ACC-001–004 and
  related Sales/Purchasing/Treasury/Returns rules).
- Architecture: `docs/SAMI_ERP_ARCHITECTURE_CONSTITUTION.md`.
- Approved scope: `docs/IMPLEMENTATION_ROADMAP.md`, Phase 4.
- Open decisions: `docs/24-roadmap-and-open-decisions.md` and
  `PROJECT_BACKLOG/HIGH/HIGH-009-invoicing-payments-returns-accounting-boundary.md`.

## Current foundations

Existing V63–V65 payable, receivable and settlement persistence and public
posting ports are partial integration evidence only. They are not canonical
Journal/GL truth. Treasury, Sales and Purchasing remain owners of their facts.

## Dependency-ordered Steps

| Step | Objective | Dependencies | Agents | Validation / completion |
|---|---|---|---|---|
| P4-S1 | Resolve and record the approved accounting boundary, invoice/payment/return lifecycle, fiscal-period and finance-policy decisions. | None; blocked by HIGH-009. | Lead, Product & UX, Guardian | Approved decision records; no unresolved blocking TBD. |
| P4-S2 | Accounting foundation: tenant/company/branch-scoped CoA and fiscal-period aggregates, audit/security boundaries. | P4-S1 | Backend, Data & Integrity, QA, Guardian | Fresh/upgrade PostgreSQL migration and CRUD/API tests. |
| P4-S3 | Immutable balanced Journal/GL and idempotent posting/reversal/correction contract. | P4-S2 | Backend, Data & Integrity, QA, Guardian | Balance, Toman rounding, idempotency, reversal and closed-period tests. |
| P4-S4 | AR/AP integration for finalized Sales Invoice and accepted Supplier Invoice. | P4-S3 and approved P4-S1 contracts | Backend, Contract Validator, QA, Guardian | Cross-module PostgreSQL acceptance and reconciliation. |
| P4-S5 | Treasury, advances, cheques, returns/refunds, forgiveness and correction posting. | P4-S3, P4-S1 decisions | Backend, Data & Integrity, QA, Guardian | Lifecycle, authorization, reversal and period-control acceptance. |
| P4-S6 | Finance-policy/tax configuration and whole-Toman rules. | P4-S1, P4-S3 | Backend, Frontend, Product & UX, QA, Guardian | Configuration/API/UI and tax/rounding acceptance. |
| P4-S7 | Accounting reports/API/UI and balanced journal/report reconciliation. | P4-S3–S6 | Backend, Frontend, Contract Validator, QA, Guardian | UI/API, permission, bilingual/RTL and reconciliation acceptance. |
| P4-S8 | Final fresh PostgreSQL, supported upgrade, focused/full regression and Phase 4 closure. | P4-S2–S7 | Lead, Data, QA, Guardian | All applicable gates green; state/Contract/run closure and permitted push. |

## Blocking and non-blocking TBDs

HIGH-009 is a blocking Product/Architecture decision for P4-S2 onward where
invoice lifecycle, payment providers, returns/refunds, fiscal periods, tax and
posting boundaries are encoded. P1 tax/approval details remain blocking only
for their dependent behavior. Independent planning and evidence reconciliation
may continue; agents must not invent values or policy.

## Phase Definition of Done

Every applicable approved Phase 4 outcome is traceably IMPLEMENTED, VALIDATED
and ACCEPTED; fresh and supported PostgreSQL migrations pass without checksum
errors; cross-module, security, audit, concurrency, UI/API and regression
evidence is green; Guardian finds no Critical/Major issue; unrelated changes
remain preserved; PROJECT-STATE, Contracts and run evidence are current; and
permitted commit/push is synchronized. Release/deployment is out of scope.

## Current execution decision

P4-S1 is the next candidate, but it cannot be completed autonomously while
HIGH-009 remains `needs-decision`. No speculative Accounting implementation is
authorized by this plan.
