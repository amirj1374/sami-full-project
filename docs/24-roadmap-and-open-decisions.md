# Roadmap and Open Decisions

This register contains unresolved Product Owner and cross-cutting decisions.
Approved business behavior is in
[`SAMI_ERP_BUSINESS_RULES.md`](SAMI_ERP_BUSINESS_RULES.md); architecture
boundaries are in
[`SAMI_ERP_ARCHITECTURE_CONSTITUTION.md`](SAMI_ERP_ARCHITECTURE_CONSTITUTION.md).

## P0 — Architecture Freeze blockers (`STILL_OPEN`)

1. **Sales invoice recognition timing:** when invoice and physical delivery are
   separate, when does the Counterparty debt arise?
2. **Purchase obligation recognition timing:** when supplier invoice and Goods
   Receipt are separate, when does company debt arise?
3. **Received-cheque settlement timing:** does customer cheque settle debt on
   receipt or only after clearance?
4. **Accounting and tax policy:** chart of accounts, fiscal periods, official
   invoice/statements, tax/exemption/withholding, currency, and rounding policy.

## P1 — Business-policy configuration (`STILL_OPEN`)

1. Expense/income type catalog, approval thresholds, and approval timing.
2. Customer-score weights, inactivity baseline, and advisory suggestion policy.
3. Manager delegation policy for approvals.
4. Common approval-policy semantics where needed.
5. Canonical file storage strategy, official Market Sync contracts, and Web Push
   provider/delivery policy.

## P2 — Deferred product scope (`STILL_OPEN`)

1. Repairs and Warranty.
2. Installments.
3. Payroll, leave, and expanded attendance integrations.
4. Expanded Customer Portal workflows.

## Resolved or superseded

- **SUPERSEDED:** Active Intercompany workflow is not a current Architecture Freeze blocker;
  current business scope is one operating company.
- **RESOLVED:** Sales Order may have partial Fulfillments and multiple Sales Invoices; final
  single invoice remains valid.
- **RESOLVED:** Customer/supplier advances are allowed and affect unified Counterparty account.
- **RESOLVED:** Return and refund are separate traceable events.
- **RESOLVED:** Party matching uses authoritative Iranian identifiers; uncertain identity never
  merges automatically.
