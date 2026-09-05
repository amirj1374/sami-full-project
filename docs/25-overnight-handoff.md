# Overnight Pre-Freeze Handoff

## Current State

- Branch: `development`
- Starting commit: `9d9724e03ee9aadac1810549b2721e1d687ee096`
- Documentation status: Product Owner decisions are consolidated in the
  Architecture Constitution and Business Rules; Architecture Freeze remains DRAFT.
- Requirements understood: 91%.

## Confirmed Product Owner Decisions

- One Counterparty identity and one unified business-facing financial account;
  customer/supplier roles do not create separate contradictory balances.
- Iranian primary identifiers are unique; existing identity is reused; uncertain
  duplicates never merge automatically; controlled merge/change is audited.
- Current business scope is one operating company. Do not implement active
  Intercompany workflow now.
- Sales Order supports partial Fulfillment and multiple or final single Invoice.
- Customer/supplier advance, partial payment, allocation, returns/refunds,
  credit control, cheque behavior, purchase receipt, supplier settlement,
  expense/income, and customer follow-up rules are recorded in
  `SAMI_ERP_BUSINESS_RULES.md`.

## Remaining P0 Questions

1. When invoice and delivery are separate, does customer debt start at invoice
   issue or actual delivery?
2. When supplier invoice and Goods Receipt are separate, does company debt start
   at supplier invoice or actual receipt?
3. Does customer cheque count as settlement on receipt or only after clearance?
4. What are the formal Accounting/Tax rules: chart of accounts, fiscal periods,
   taxes/exemptions/withholding, official invoices/statements, currency, rounding?

## Remaining P1

- Expense/income policy catalog and thresholds.
- Customer score/inactivity/advisory policy.
- Manager delegation policy.
- Common approval semantics if needed.
- File storage, official Market Sync contracts, and Web Push provider policy.

## Implementation Gaps

- Current source has separate Customer/Supplier storage and no approved unified
  Counterparty financial account.
- Current source has combined `Sale`, not approved Quotation/Order/Fulfillment/
  Invoice documents.
- Counterparty advance/allocation, credit/overdue, cheque settlement, supplier
  prepayment, and approved reminder/intelligence rules are not verified as
  implemented business behavior.
- Canonical Accounting, tax policy, official reporting, and Final Asan Import are
  not implemented.
- Current business scope is single-company; do not add Intercompany complexity.

## Recommended Next Step Tomorrow

1. Ask Product Owner the four P0 questions above.
2. Record answers in Constitution, Business Rules, and Open Decisions.
3. Re-run the architecture/consistency audit.
4. If no P0 remains, mark Architecture Constitution READY FOR FREEZE.
5. Only then create implementation planning/phases.
6. Do not start coding before Freeze.

## Last Known Good State

- Clean/dirty status before this task: dirty documentation-only state from the
  preceding Product Owner consolidation task; no source or migration changes.
- Files changed during this task: Constitution, Business Rules, Open Decisions,
  Project Index, Domain Model, Organization guidance, and this handoff.
- Validation: documentation validator, documentation tests, rule-ID uniqueness,
  internal-reference checks, diff whitespace check, and source-change audit must
  pass before checkpoint commit.
