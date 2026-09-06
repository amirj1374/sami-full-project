# Roadmap and Open Decisions

This register contains unresolved Product Owner and cross-cutting decisions.
Approved business behavior is in
[`SAMI_ERP_BUSINESS_RULES.md`](SAMI_ERP_BUSINESS_RULES.md); architecture
boundaries are in
[`SAMI_ERP_ARCHITECTURE_CONSTITUTION.md`](SAMI_ERP_ARCHITECTURE_CONSTITUTION.md).

## P0 — Architecture Freeze blockers

None. The Architecture Freeze baseline is recorded in
[`SAMI_ERP_ARCHITECTURE_CONSTITUTION.md`](SAMI_ERP_ARCHITECTURE_CONSTITUTION.md).

## P1 — Business-policy configuration (`STILL_OPEN`)

1. Expense/income type catalog, approval thresholds, and approval timing.
2. Customer-score weights, inactivity baseline, and advisory suggestion policy.
3. Manager delegation policy for approvals.
4. Common approval-policy semantics where needed.
5. Canonical file storage strategy, official Market Sync contracts, and Web Push
   provider/delivery policy.
6. Tax rates, exemptions, withholding applicability, official-document layouts,
   and statutory reporting configuration, owned as finance policy rather than
   a commercial-domain architecture decision.

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
- **RESOLVED:** Registered order price is preserved at order registration.
- **RESOLVED:** Confirmed orders may be corrected by their creator only during the configured
  correction window, with the audit trail preserved.
- **RESOLVED:** Quotation/proforma has no inventory or financial effect.
- **RESOLVED:** Received customer cheques settle debt on receipt, remain pending collection until
  clearance, and reduce available credit until they clear.
- **RESOLVED:** Returned customer cheques do not recreate the original debt; they block further
  credit sales until resolved.
- **RESOLVED:** Delivered supplier cheques settle payable for payable-balance purposes; later return
  does not recreate the original payable.
- **RESOLVED:** Reservation timeout defaults to 30 minutes and is configurable.
- **RESOLVED:** Full requested order quantity may be registered even when stock is short; available
  quantity is reserved and the remainder is backordered.
- **RESOLVED:** Debt forgiveness requires a recorded reason.
- **RESOLVED:** Expense may be recorded before approval; approval state remains explicit.
- **RESOLVED:** Accrued/unreceived income is recorded as a receivable from recognition time.
- **RESOLVED:** A creator-only correction window defaults to 30 minutes and is configurable; manager
  notification is required when such a correction occurs.
- **RESOLVED:** Completely incorrect transactions may be manager-deleted if audit evidence is retained.
- **RESOLVED:** Contact is the shared identity; Customer and Supplier are roles
  on that identity. Confirmed duplicates merge only through an audited,
  history-preserving operation.
- **RESOLVED:** Active Company and Branch are real operating context. Access is
  explicit positive grant; Company roles apply only within granted Branches.
- **RESOLVED:** Sales Invoice creates customer receivable while Delivery controls
  physical stock issue. Multiple traceable Sales Orders may be combined in one
  Invoice for the same Counterparty.
- **RESOLVED:** Accepted Supplier Invoice creates supplier payable while Goods
  Receipt alone controls physical stock receipt.
- **RESOLVED:** Canonical Accounting owns Journal/GL and receives finalized
  commercial and Treasury facts through a posting contract.
- **RESOLVED:** The operating monetary unit is Toman; multi-currency is deferred.
- **RESOLVED:** Tax and official-document details are configuration-led finance
  policy and do not block commercial-domain architecture.
