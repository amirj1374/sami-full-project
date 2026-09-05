# SAMI ERP Architecture Constitution v1.0

## 1. Product Vision

SAMI ERP is a tenant-scoped ERP for the commercial, inventory, treasury,
counterparty, and operational records of its operating company. It preserves a
traceable unified business-facing counterparty balance while Accounting will
maintain correct internal debit/credit records. Current business scope is one
operating company; the Tenant -> Company -> Branch platform remains compatible
with future expansion.

## 2. Product Scope

### Included

- Current core authentication, authorization, tenancy, licensing, CRM/product,
  purchasing, inventory, sales, treasury, automation, scheduler, notification,
  reporting, data-quality, and legacy-migration capabilities.
- Approved business behavior for unified Counterparty balances, sales/purchase
  settlement, credit, cheques, financial correction, and customer follow-up.

### Not Included

- Active Intercompany workflow, separate financial Customer/Supplier accounts,
  or a requirement that a representative owns a company/store's financial account.
- Canonical Accounting, official statements, Final Asan Import, Web Push delivery,
  and external Market Sync publication contracts before pending decisions or
  external dependencies are resolved.

### Future Extensions

- Multi-company operations, Repairs, Warranty, Installments, Payroll/leave, and
  expanded portal workflows remain future-compatible but are not current scope.

## 3. Design Principles

1. **One Counterparty, one business-facing account.** All value transfers with a
   Counterparty affect the same traceable running account.
2. **Unique identity.** National Code identifies a natural person; National
   Identifier/approved organization identifier identifies a legal Counterparty.
3. **Trusted tenant scope.** Tenant identity comes from authenticated server
   context, never from client-supplied authority.
4. **Single-company current scope.** Do not introduce active Intercompany
   workflow now.
5. **Commercial document separation.** Quotation, Sales Order, Fulfillment, and
   Sales Invoice are separate traceable documents.
6. **Inventory truth.** Inventory alone owns stock balances, movements,
   reservations, and serial/IMEI custody.
7. **Treasury truth.** Treasury owns cash/bank accounts, cheques, transfers,
   reversals, adjustments, and operational cash movements.
8. **Accounting truth.** Accounting will own canonical journals, ledgers, and
   official statements; unified Counterparty presentation does not replace
   double-entry accounting.
9. **Policy and snapshot.** Approved transaction values remain traceable and
   are not silently replaced by later price updates or Market Sync.
10. **Controlled correction.** Finalized financial facts use approval and
    cancellation, reversal, or corrective transaction rather than silent edit.
11. **Legacy safety.** Legacy staging keeps provenance and does not silently
    mutate canonical records before approved Final Import.

## 4. Architectural Principles

1. Every business capability has one owner.
2. Business rules stay in business domains; controllers are transport and
   infrastructure does not own business workflows.
3. Cross-module services support domains without owning their business facts.
4. Backend authorization is authoritative; UI visibility is presentation only.
5. Lifecycle changes are service operations, not arbitrary client status edits.
6. Flyway owns schema and applied migrations are forward-only.
7. New tenant-owned work uses trusted `TenantContext`; `TenantDefaults` is
   transitional legacy behavior and is not propagated.
8. Integrations and legacy migration use public domain contracts and do not
   bypass canonical owners through direct persistence writes.

## 5. Core Business Domains

| Domain | Purpose and responsibilities | Owned concepts | Current status |
|---|---|---|---|
| Counterparty / CRM | Owns real Counterparty identity, acquisition source, relationship history, and future customer analysis. | Counterparty, customer/supplier commercial roles, lead, opportunity, interactions | Current CRM/customer scope exists; unified Counterparty behavior is approved but not an implementation claim. |
| Product Catalog | Owns product catalog facts and identifiers. | Product, future variant/UOM mapping | Basic CRUD implemented; approved variant/UOM model remains future work. |
| Sales | Owns quotation, order, fulfillment, invoice, return, price, and commercial lifecycle. | Quotation, Order, Fulfillment, Invoice, Return | Current combined Sale exists; approved separated-document model is not yet implemented. |
| Purchasing | Owns procurement, invoice/receipt relationship, returns, replacement, and delivery follow-up. | Purchase, invoice, receipt, return | Current scope implemented; approved settlement behavior is pending implementation. |
| Inventory | Owns physical stock truth. | Warehouse, balance, movement, reservation, serial | Implemented for current scope. |
| Treasury | Owns monetary settlement and company cash/bank operations. | Bank/cash account, receipt, payment, transfer, cheque, adjustment | Implemented for current scope; Counterparty settlement rules expand future contract. |
| Accounting | Owns official financial truth. | Future CoA, periods, journals, ledgers, statements | Planned. |
| HR / Attendance | Owns employee and attendance records. | Employee, attendance, correction | Partial. |

## 6. Shared Domains

| Shared domain | Ownership boundary |
|---|---|
| Counterparty identity support | Counterparty is the financial/commercial identity. A representative may support communication history but does not own a company/store's financial account. |
| Documents and Attachments | Owns files, versions, metadata, retention, and attachments; never the represented business entity. |
| Metadata and Dynamic Forms | Owns reusable metadata/form capability, not a business lifecycle. |
| Calendar and Working Time | Owns shared calendar, holiday, and working-time concepts. |
| Workflow and Approval Policy | May provide common approval semantics while each domain retains its lifecycle and facts. |
| Product Units of Measure | Shared UOM definitions; Product owns product mapping and Inventory owns canonical quantity. |

## 7. Cross Module Services

| Service | Ownership boundary |
|---|---|
| Automation | Rules, triggers, actions, and executions; never business records or policy. |
| Scheduler | Job execution, locking, polling, and runtime behavior. |
| Notification Center | Notification decision/delivery, preferences, in-app state, and future push boundary. |
| Communication Hub | Templates and provider dispatch, not trigger business facts. |
| Data Quality | Rules, issues, and remediation tracking across domains. |
| Audit Trail | Shared audit standards; domains own truthful facts about their actions. |
| Reporting and Analytics | Cross-domain analysis; domains retain their facts. |
| Numbering and Sequences | Reusable numbering support; documents retain identity/lifecycle. |

## 8. Infrastructure

- Authentication/Security, Authorization/RBAC/Module Lifecycle, trusted Tenancy,
  PostgreSQL/Flyway persistence, API/DTO/validation/error foundation, storage
  provider boundary, localization/RTL-LTR/presentation, PWA/user preferences,
  Docker/Compose/nginx deployment, and Licensing entitlements.

## 9. Domain Ownership Matrix

| Capability | Owner | Consumers | Single source of truth |
|---|---|---|---|
| Tenant scope | Core Tenancy | All scoped domains | Authenticated `TenantContext` |
| Company/Branch platform structure | Organization | All business domains | Organization records/rules |
| Counterparty identity/account | Counterparty / CRM | Sales, Purchasing, Treasury, Accounting, Reporting | One Counterparty and traceable running account |
| Acquisition source | Counterparty / CRM | Sales, Reporting | Manager-maintained source catalog |
| Product catalog | Product Catalog | Sales, Purchasing, Inventory, HAMTA | Product/Variant model |
| Sales flow | Sales | Inventory, Treasury, Accounting, CRM | Sales documents/lifecycle |
| Purchasing flow | Purchasing | Inventory, Treasury, Accounting | Purchase documents/lifecycle |
| Stock state | Inventory | Sales, Purchasing, HAMTA | Inventory movements/balances/reservations |
| Cash/bank/cheques | Treasury | Sales, Purchasing, Accounting | Treasury accounts/movements/cheques |
| Journals/ledgers | Accounting | Treasury, Sales, Purchasing, Reporting | Canonical Accounting journal |
| Notifications/reminders | Notification Center | Automation/all domains | Notification state/preferences |
| Follow-up history | CRM | Sales, Treasury, Automation | Counterparty/customer history |
| Legacy provenance | Legacy Asan Migration | Migration/reconciliation | Staged source evidence |

## 10. Module Dependency Map

- Authentication, RBAC, Tenancy, Organization, and Licensing underpin all scoped modules.
- Counterparty/CRM supplies identity and history to Sales, Purchasing, Treasury,
  Accounting, Reporting, and customer intelligence.
- Sales/Purchasing use Inventory for stock and Treasury for monetary settlement.
- Treasury updates Counterparty running account; Accounting will record official
  ledger truth after posting policy is finalized.
- Automation/Scheduler/Notification Center execute reminders, never decide debt,
  credit, cheque, or commercial policy.
- Market Sync supplies policy-controlled price input, never rewrites approved
  transaction snapshots. Legacy Asan stages/reconciles until Final Import exists.

## 11. Architectural Rules

1. Every Counterparty has one unified business-facing financial account.
2. Customer/Supplier roles never create contradictory balances for the same real Counterparty.
3. Authoritative Iranian identifier is unique; uncertain identity does not auto-merge.
4. Identifier change, uncertain merge, reactivation, and deletion are controlled/audited.
5. One Order may have multiple Fulfillments and multiple Invoices; final single invoice remains valid.
6. Sales, purchase, stock, payment, cheque, return, refund, and correction events remain distinct and traceable.
7. Inventory increases only for actual receipt and decreases only for approved issue/fulfillment.
8. Advance payment affects the unified Counterparty account; it is not a hidden unrelated balance.
9. Internal company transfer is neither income nor expense.
10. Finalized financial records are not directly and silently edited.
11. Current business scope is one operating company; no active Intercompany workflow is introduced.
12. Automation/Notification Center execute approved reminders, not business approvals.
13. Intelligence/AI suggestions are advisory and never alter financial/business data without user approval.

## 12. Product Decisions

### Counterparty / CRM

- Counterparty type is Natural Person or Legal Entity/Company/Store.
- One Counterparty holds one unified financial account for all commercial and
  financial events; it may both buy from and sell to the company.
- National Code or National Identifier/approved organization identifier is
  unique; existing identity is reused.
- Required data: type, identifier, person or company/store name, address,
  phone, and configurable acquisition source. Bank account is optional/not required.
- Company/store, not its representative, owns the financial account.
- Deactivation preserves history; reactivation/deletion require manager approval.
- Potential duplicates may be suggested; uncertain merge requires manager/admin
  action and full audit.
- Customer history includes calls, follow-ups, issues, negotiation result, notes,
  and satisfaction follow-up.

### Sales / Credit

- Partial delivery and multiple Invoices per Order are allowed; final single
  invoice is allowed.
- Customer advance, partial/multi-method payment, allocation, return/refund,
  credit/account credit, discount, price override, forgiveness, and correction
  follow the business-rules specification.
- Default credit limit is zero and default non-cheque open-account term is five
  days; manager may override per Counterparty.
- Credit sales stop at overdue debt; cash sales remain allowed with warning,
  subject to explicit manager exception where approved.
- Confirmed Order editing is allowed only while dependencies, consistency, and
  audit history remain preserved.

### Purchasing / Supplier Settlement

- Credit purchase creates debt; supplier advance creates receivable until goods
  or refund settle it.
- Purchase Invoice and Goods Receipt are separate; stock increases only on actual receipt.
- Supplier payment defaults to oldest debt when no specific debt is selected.
- Supplier return/refund/replacement/invoice-difference/delay behavior follows
  the business-rules specification.

### Treasury / Financial Control

- Bank accounts/cashboxes have independent balances.
- Receipt, payment, transfer, cheque, adjustment, refund, and correction are
  separate traceable financial events.
- Internal transfer is not income/expense; financial adjustment requires request,
  manager approval, and auditable evidence.

### Automation / Customer Intelligence

- Debt and supplier-delay reminders use Scheduler/Notification Center.
- Inactivity creates manager notification/follow-up. Three days after completed
  product sale, seller receives satisfaction/issue follow-up reminder.
- Customer score and churn suggestions are advisory, never automatic restriction
  or financial mutation.

### Organization

- Tenant -> Company -> Branch remains technical hierarchy. Current business scope
  is one operating company; this supersedes an active Intercompany requirement.

## 13. Pending Product Decisions

### P0

1. When invoice and delivery occur at different times, does customer debt arise
   at Invoice issue or physical delivery?
2. When supplier invoice and Goods Receipt occur at different times, does company
   debt arise at invoice record time or receipt?
3. Does customer cheque settle debt/usable credit on receipt or only clearance?
4. Accounting/Tax: chart of accounts, periods, tax/exemption/withholding, official
   invoice/statements, currency, and rounding.

### P1

1. Expense/income type catalog, default thresholds, and approval timing.
2. Customer-score weights, inactivity baseline, advisory explanations/actions.
3. Manager-delegation policy when multiple managers exist.
4. Common approval-policy semantics, storage strategy, official Market Sync
   contracts, and Web Push delivery policy.

### P2

1. Repairs/Warranty, Installments, Payroll/leave, and expanded Portal workflows.

## 14. Architectural Risks

1. Current source has a combined Sale aggregate, not approved separated documents.
2. Customer/Supplier data overlaps and does not yet implement unified Counterparty balance.
3. Canonical Accounting is absent; local evidence is not official ledger truth.
4. Current company operation is single-company; speculative Intercompany adds risk.
5. Invoice/purchase obligation/cheque timing affects balance, credit, reporting, and Accounting.
6. Legacy Asan is staging/reconciliation only until Accounting/Final Import policy exists.

## 15. Future Architecture Roadmap

### Architecture Freeze
Use this constitution and the business-rules specification as ownership and behavior reference.

### Counterparty Financial Truth
Establish approved unique Counterparty and unified account without weakening double-entry.

### Commercial Document Truth
Establish approved sales/purchase separation, stock events, settlement, and traceability.

### Accounting Finalization
Establish canonical Accounting/Tax before official reporting or Final Asan Import.

### Legacy Retirement
After approved Final Import/reconciliation, retain required evidence and retire Legacy Asan from active workflow.

## 16. Architecture Freeze

**Architecture Version:** v1.0

**Status:** DRAFT

Party matching, Order/Invoice cardinality, advance payment, return/refund, and
active Intercompany priority are resolved. Freeze remains blocked only by P0:
sales invoice recognition timing, purchase obligation recognition timing,
customer cheque settlement timing, and Accounting/Tax policy.

The P0 decisions must be recorded explicitly before status changes to READY FOR FREEZE.
