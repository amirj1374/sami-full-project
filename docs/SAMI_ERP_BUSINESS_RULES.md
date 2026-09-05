# SAMI ERP Business Rules

This is the authoritative specification of approved SAMI business behavior.
Rules do not claim that every behavior is already implemented. Architecture
ownership is defined in
[`SAMI_ERP_ARCHITECTURE_CONSTITUTION.md`](SAMI_ERP_ARCHITECTURE_CONSTITUTION.md).

## 1. Counterparty

- **BR-PARTY-001:** Every Counterparty has one unified business-facing financial
  account. Sale, purchase, payment, receipt, advance, refund, return, discount,
  forgiveness, cheque, and adjustment affect the same running balance.
- **BR-PARTY-002:** SAMI shows whether Counterparty owes company, company owes
  Counterparty, or balance is zero; full transaction history remains visible.
- **BR-PARTY-003:** Counterparty is Natural Person or Legal Entity/Company/Store.
- **BR-PARTY-004:** National Code uniquely identifies natural person; National
  Identifier/approved organization identifier uniquely identifies legal entity.
- **BR-PARTY-005:** Existing authoritative identity is reused and never creates
  another Counterparty account.
- **BR-PARTY-006:** Primary-identifier change requires request, manager approval,
  audit, and preservation of the same history.
- **BR-PARTY-007:** Required data is type, identifier, personal/company name,
  address, phone, and acquisition source. Bank account is not required now.
- **BR-PARTY-008:** Company/store is the Counterparty; representative is not its
  financial account owner.
- **BR-PARTY-009:** Deactivation preserves history. Reactivation requires manager
  approval and never creates a new account.
- **BR-PARTY-010:** Deletion requires manager approval and never silently destroys
  financial/operational history.
- **BR-PARTY-011:** Similar name, phone, or address may suggest review but never
  authorizes automatic merge of different authoritative identifiers.
- **BR-PARTY-012:** Uncertain merge requires controlled manager/admin action and
  complete audit.

## 2. CRM

- **BR-CRM-001:** Acquisition/introduction source is recorded for Counterparty.
- **BR-CRM-002:** Managers maintain source options; options are not hardcoded.
- **BR-CRM-003:** A Counterparty can buy from and sell to company while retaining
  one identity and one account.
- **BR-CRM-004:** Customer history retains calls, sales/debt follow-ups, issues,
  negotiation result, notes, and satisfaction follow-up.
- **BR-CRM-005:** Follow-up result can be purchased, not interested now, call
  later, issue/problem, or other. Manager selects next date for call later.

## 3. Sales

- **BR-SALES-001:** Quotation, Sales Order, Fulfillment/Delivery, and Sales
  Invoice are distinct traceable business documents.
- **BR-SALES-002:** One Sales Order can have multiple partial Fulfillments; open
  quantity stays open until fulfilled or cancelled.
- **BR-SALES-003:** One Sales Order can have multiple Invoices or one final
  Invoice for the full Order.
- **BR-SALES-004:** Customer advance before invoice is allowed and later may be
  allocated to one or more invoices in same Counterparty account.
- **BR-SALES-005:** Partial payment is allowed; paid, remaining, and overdue
  amounts are visible where applicable.
- **BR-SALES-006:** Standalone customer payment defaults to oldest debt first
  unless permitted reallocation is selected.
- **BR-SALES-007:** New cash sale defaults payment to that sale despite old debt;
  seller can allocate permitted amount to old debt instead.
- **BR-SALES-008:** Settlement may combine cash, POS/card, transfer, cheque,
  online payment, or multiple methods.
- **BR-SALES-009:** Seller discount is allowed within configurable authority;
  excess requires manager approval.
- **BR-SALES-010:** Seller price override is allowed within configured limits;
  out-of-range change requires manager approval.
- **BR-SALES-011:** Approved transaction price is snapshotted and not silently
  changed by later price update or Market Sync.
- **BR-SALES-012:** Confirmed Order may be edited while flow permits it. Existing
  reservation, delivery, invoice, payment, or other dependency preserves
  consistency and audit history.
- **BR-SALES-013:** Partial/full sales return is supported; partial return affects
  only returned quantity/value and does not cancel full original invoice.

## 4. Purchasing

- **BR-PUR-001:** Credit purchase creates Counterparty debt; cash purchase can
  settle immediately.
- **BR-PUR-002:** Supplier payment term has default and manager override per
  Counterparty.
- **BR-PUR-003:** Overdue supplier debt notifies responsible user/manager and,
  after configured delay, new purchase can require approval.
- **BR-PUR-004:** Partial supplier payment is allowed.
- **BR-PUR-005:** Supplier payment without selected debt defaults to oldest
  outstanding debt.
- **BR-PUR-006:** Purchase Invoice and Goods Receipt are separate. Stock does not
  increase until physical receipt.
- **BR-PUR-007:** Partial Goods Receipt is allowed; only received quantity enters
  Inventory and remaining quantity stays open.
- **BR-PUR-008:** Supplier delivery delay marks delay, notifies purchasing user/
  manager, and creates follow-up work.
- **BR-PUR-009:** Supplier invoice difference always requires manager approval;
  no automatic tolerance acceptance.
- **BR-PUR-010:** Supplier discount is supported; configurable threshold may
  require manager approval beyond normal processing.

## 5. Inventory Interactions

- **BR-INV-001:** Quotation and draft Order do not reserve stock.
- **BR-INV-002:** Confirmed Order reserves stock; cancellation releases reservation.
- **BR-INV-003:** Direct retail invoice issues stock on completion.
- **BR-INV-004:** Purchase Invoice alone never increases Inventory.
- **BR-INV-005:** Goods Receipt increases Inventory only by actual received quantity.
- **BR-INV-006:** Approved defective return adjusts Inventory and Counterparty balance.
- **BR-INV-007:** Same-product supplier replacement stays in same purchase/return
  history and does not create unnecessary new purchase.

## 6. Treasury

- **BR-TREASURY-001:** Company may have multiple bank accounts/cashboxes; each
  has independent balance.
- **BR-TREASURY-002:** Receipt, payment, refund, transfer, cheque, and adjustment
  affect the intended financial account.
- **BR-TREASURY-003:** Internal transfer reduces source and increases destination;
  it is neither income nor expense.
- **BR-TREASURY-004:** Cash/bank adjustment requires request, manager approval,
  and auditable reason/evidence.
- **BR-TREASURY-005:** Incorrect financial amount uses correction request then
  manager approval; finalized record cannot be uncontrolled edited.
- **BR-TREASURY-006:** Before finalization deletion may be possible. Afterward,
  cancellation, reversal, or correction preserves history.

## 7. Credit & Debt

- **BR-CREDIT-001:** Default customer credit limit is zero.
- **BR-CREDIT-002:** Manager grants/changes credit limit; system may recommend,
  but manager decides.
- **BR-CREDIT-003:** Remaining credit uses current outstanding exposure; sale
  exceeding permitted limit stops unless manager approves.
- **BR-CREDIT-004:** Default open-account term without cheque is five days;
  manager may set longer term per Counterparty.
- **BR-CREDIT-005:** Overdue debt marks overdue and notifies seller/manager.
- **BR-CREDIT-006:** Credit sale to overdue Counterparty is blocked until settled
  unless manager explicitly decides. Cash sale is allowed with warning.
- **BR-CREDIT-007:** Manager may forgive debt partly/fully; reason is optional,
  action remains auditable.
- **BR-CREDIT-008:** Supplier advance before receipt makes supplier owe company;
  received goods or separate refund reduce that amount.

## 8. Cheques

- **BR-CHEQUE-001:** Cheque lifecycle includes RECEIVED, PENDING, DUE, CLEARED,
  and BOUNCED behavior.
- **BR-CHEQUE-002:** Uncleared customer cheque at due date notifies manager and
  relevant seller, makes usable credit zero, and blocks new credit sale until resolved.
- **BR-CHEQUE-003:** Bounced customer cheque reactivates debt and notifies users;
  full settlement restores manager-defined credit limit.
- **BR-CHEQUE-004:** Issued supplier cheque leaves obligation pending clearance;
  bounce reopens debt and triggers notifications/overdue policy.
- **BR-CHEQUE-005:** Customer-cheque settlement on receipt versus clearance is P0 pending.

## 9. Expenses

- **BR-EXP-001:** Small expense may be direct; large expense may require manager
  approval using configurable default threshold.
- **BR-EXP-002:** Manager can change expense threshold.
- **BR-EXP-003:** Expense type policy controls approval before recording or payment.
- **BR-EXP-004:** Recurring expense can be automatic/manual; fixed may reuse
  amount, variable needs user confirmation/change before final posting.

## 10. Income

- **BR-INC-001:** Income outside normal product sale is supported.
- **BR-INC-002:** Income type policy controls direct record versus manager approval.
- **BR-INC-003:** Income type policy controls recognition before receipt or on cash receipt.

## 11. Returns & Refunds

- **BR-RETURN-001:** Sales return/refund are separate traceable events.
- **BR-RETURN-002:** Sales return may produce money refund or Counterparty account credit.
- **BR-RETURN-003:** Purchase return can reduce payable, create supplier receivable,
  produce supplier refund, or create replacement.
- **BR-RETURN-004:** Defective/nonconforming supplier return requires manager approval.
- **BR-RETURN-005:** Replacement price difference adjusts Counterparty balance in
  either direction and requires manager approval.

## 12. Approval Rules

- **BR-APR-001:** Manager approval is required for identifier change,
  reactivation/deletion, credit exception, out-of-authority discount/price,
  financial correction/adjustment, defective return, replacement difference, and
  supplier invoice difference.
- **BR-APR-002:** Manager can approve exceptional credit sale for overdue Counterparty.
- **BR-APR-003:** Expense/income approval timing follows approved policy, not
  implicit universal rule.

## 13. Notifications & Follow-ups

- **BR-NOTIFY-001:** Debt due date is recorded where applicable; due notification
  goes to responsible sales user and manager.
- **BR-NOTIFY-002:** Unpaid debt follow-up continues through configured automation.
- **BR-NOTIFY-003:** Supplier delay notifies purchasing user/manager and creates task.
- **BR-NOTIFY-004:** Three days after completed product sale, seller is reminded
  to obtain satisfaction/problem/issue/feedback outcome.
- **BR-NOTIFY-005:** Outcome attaches to Counterparty history; manager sees issue
  in record and consolidated issue report.

## 14. Customer Intelligence

- **BR-INTEL-001:** Detect abnormal inactivity against own purchase history and notify manager.
- **BR-INTEL-002:** Inactivity creates manager follow-up; future recipient configuration is allowed.
- **BR-INTEL-003:** Score may use purchase frequency/value, payment timeliness,
  overdue behavior, follow-up result, and satisfaction/issues.
- **BR-INTEL-004:** Score is analysis only; it does not automatically restrict sale.
- **BR-INTEL-005:** New customer may have approximate assessment improved by history.
- **BR-INTEL-006:** Churn/decline intelligence detects decline, explains likely
  cause, and suggests recovery using customer history.
- **BR-INTEL-007:** Intelligence/AI is advisory and never modifies business or
  financial data without user approval.

## 15. Audit / Correction Rules

- **BR-AUDIT-001:** Identifier change, uncertain merge, reactivation, deletion,
  correction, forgiveness, adjustment, approval, reversal, return/refund,
  cheque status, and allocation history are auditable.
- **BR-AUDIT-002:** Finalized financial record retains history through cancellation,
  reversal, or corrective transaction rather than silent edit.
- **BR-AUDIT-003:** Unified Counterparty balance is explainable by complete
  underlying transaction history.
