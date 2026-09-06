# Overnight Architecture-Freeze Handoff

## Current State

- Branch: `development`
- Starting commit: `9d9724e03ee9aadac1810549b2721e1d687ee096`
- Documentation status: Product Owner decisions are consolidated in the
  Architecture Constitution and Business Rules; Architecture Freeze is READY.
- Requirements understood: 100% for the frozen Phase 0–6 baseline.

## Confirmed Product Owner Decisions

- One Counterparty identity and one unified business-facing financial account;
  customer/supplier roles do not create separate contradictory balances.
- Iranian primary identifiers are unique; existing identity is reused; uncertain
  duplicates never merge automatically; controlled merge/change is audited.
- Current business scope is one operating company. Do not implement active
  Intercompany workflow now.
- Sales Order supports partial Fulfillment and multiple or final single Invoice.
- Registered order price is preserved.
- Confirmed orders may be corrected by their creator only during the configurable
  correction window.
- Quotation/proforma has no inventory or financial effect.
- Customer/supplier advance, partial payment, allocation, returns/refunds,
  credit control, cheque behavior, purchase receipt, supplier settlement,
  expense/income, and customer follow-up rules are recorded in
  `SAMI_ERP_BUSINESS_RULES.md`.
- Received customer cheques settle debt on receipt and reduce available credit
  until clearance.
- Reservation timeout is configurable and defaults to 30 minutes.

## Architecture Freeze Decisions

- Contact is the shared identity; Customer/Supplier are roles and controlled
  merge preserves all business and financial history.
- Active Company/Branch is trusted operating context with explicit positive
  Company/Branch grants and Company-specific roles.
- Sales Invoice creates customer receivable; Delivery controls physical stock.
  Accepted Supplier Invoice creates payable; Goods Receipt controls stock receipt.
- Canonical Accounting owns Journal/GL and consumes finalized commercial and
  Treasury facts through the posting contract.
- Current money is Toman; multi-currency is deferred. Tax/statutory detail is
  configuration-led finance policy, not a commercial architecture blocker.

## Remaining P1

- Expense/income policy catalog and thresholds.
- Customer score/inactivity/advisory policy.
- Manager delegation policy.
- Common approval semantics if needed.
- File storage, official Market Sync contracts, and Web Push provider policy.
- Tax rates, exemptions, withholding applicability, official-document layouts,
  and statutory-reporting configuration.

## Implementation Gaps

- Current source has separate Customer/Supplier storage and has not implemented
  the frozen shared Contact/unified Counterparty model.
- Current source has combined `Sale`, not approved Quotation/Order/Fulfillment/
  Invoice documents.
- Counterparty advance/allocation, credit/overdue, cheque settlement, creator-only
  correction windows, supplier prepayment, backorders, and approved reminder/
  intelligence rules are not verified as implemented business behavior.
- Canonical Accounting, finance-policy configuration, official reporting, and
  Final Asan Import are not implemented.
- Current business scope is single-company; do not add Intercompany complexity.

## Recommended Next Step Tomorrow

1. Begin Phase 0 planning only, using `docs/IMPLEMENTATION_ROADMAP.md`.
2. Perform the mandatory read-only preparation gate and create a Phase 0
   implementation specification before code or migrations.
3. Keep P1/P2 items out of Phase 0 unless separately approved.

## Last Known Good State

- Clean/dirty status before this task: dirty documentation-only state from the
  preceding Product Owner consolidation task; no source or migration changes.
- Files changed during this task: Constitution, Business Rules, Open Decisions,
  Project Index, Domain Model, Organization guidance, and this handoff.
- Validation: documentation validator, documentation tests, rule-ID uniqueness,
  internal-reference checks, diff whitespace check, and source-change audit must
  pass before checkpoint commit.
