# SAMI ERP Implementation Roadmap

This roadmap implements the frozen architecture in dependency order. It does
not authorize code changes by itself: each phase needs its own approved
implementation plan and the repository preparation gate in `21-ai-agent-guide`.
P1/P2 items remain backlog, not implied scope.

## Phase 0 — Foundation

- **Scope / ownership:** Organization owns Company/Branch context and grants;
  CRM owns shared Contact identity; Audit owns shared evidence standards.
- **Database / API / frontend:** Add forward-compatible Company/Branch context,
  user Company-role and Branch grants, Contact-role transition paths, trusted
  scope APIs, and active-context UI only after backend enforcement exists.
- **Security / audit:** Derive scope server-side, deny on missing grant, audit
  grant/context/identity-merge changes, and never trust a frontend selector.
- **Migration / compatibility:** Preserve Customer/Supplier records and map
  them to Contact without deleting history; use explicit mapping/provenance and
  a reversible rollout plan.
- **Tests / acceptance:** Tenant/company/branch isolation, different user roles
  per Company, denied absent grants, active context switching, Contact merge,
  and history preservation pass.
- **Rollback:** Additive schema and dual-read/verified backfill only; retain
  legacy records and mappings until acceptance is signed off.

## Phase 1 — Sales Core

- **Scope / ownership:** Sales owns Quotation, Sales Order, Fulfillment,
  Sales Invoice, payment allocation, Return/Credit and price snapshots.
- **Database / API / frontend:** Introduce forward-only separated documents,
  traceable document links, lifecycle APIs, sales workspace/forms, allocation,
  return/refund and pricing views.
- **Security / audit:** Enforce Company/Branch scope, seller correction windows,
  manager exceptions, price/discount authority, and audit every lifecycle or
  allocation change.
- **Migration / compatibility:** Preserve legacy Sale identifiers and accounting
  evidence; map each legacy Sale to explicit target documents with provenance;
  run dual-read/reconciliation before retiring legacy paths.
- **Tests / acceptance:** Quotation has no stock/financial effect; confirmed
  order reservation, partial delivery, backorder, multiple/combined invoices,
  receivable timing, payment allocation, credit block, cheque, partial return,
  refund, RTL/LTR and API contracts pass.
- **Rollback:** Keep old Sale data readable and do not destructively convert or
  overwrite historical records; new writes can be disabled while preserved data
  remains accessible.

## Phase 2 — Purchasing

- **Scope / ownership:** Purchasing owns Purchase Order, Goods Receipt,
  Supplier Invoice, supplier follow-up and Payables integration; Treasury owns
  actual payment movement and Purchase Payment Request remains within it.
- **Database / API / frontend:** Add separated purchase documents and links,
  receipt/invoice APIs, supplier settlement views, exception approval and
  payment-request integration.
- **Security / audit:** Scope all supplier facts, approve invoice differences
  and defective returns, and audit receipt, invoice, payable, cheque and
  correction transitions.
- **Migration / compatibility:** Preserve current Purchase records; map them to
  documents without duplicate stock or payable effects; retain provenance and
  reconciliation evidence.
- **Tests / acceptance:** Partial receipt, no stock increase from invoice alone,
  accepted invoice payable timing, defective goods exclusion, delayed delivery,
  invoice-difference approval, supplier cheque and partial payment paths pass.
- **Rollback:** Additive documents and mappings; disable new flow without
  deleting existing purchase/inventory/treasury history.

## Phase 3 — Inventory

- **Scope / ownership:** Inventory owns Variant stock identity, UOM quantity,
  reservation, backorder, movement and Serial/IMEI custody.
- **Database / API / frontend:** Add Variant/UOM/Price List mappings, conversion
  APIs, reservation/backorder operations and responsive stock views.
- **Security / audit:** Permit inventory movement only through Inventory public
  operations; audit conversions, reservations, expiry/release, serial custody
  and adjustments.
- **Migration / compatibility:** Preserve simple Products and current balances;
  create explicit Product-to-Variant and legacy-UOM mappings, never rewrite
  historical movement quantities.
- **Tests / acceptance:** Simple and variant products, UOM conversion/snapshot,
  30-minute configurable reservation timeout, shortage backorder, concurrent
  reservation and serial integrity pass.
- **Rollback:** Additive variant/UOM records and compatibility reads; retain
  source product/balance provenance until validated reconciliation completes.

## Phase 4 — Accounting

- **Scope / ownership:** Accounting owns chart of accounts, Counterparty
  account presentation, Journal/GL, AR/AP, Fiscal Period, tax policy and the
  posting contract. Sales, Purchasing and Treasury remain owners of their facts.
- **Database / API / frontend:** Add forward-only accounting records, posting
  contract, period close/reopen, finance-policy configuration and financial
  report/API/UI paths.
- **Security / audit:** Only approved final domain actions post; correction uses
  reversal/corrective evidence, closed periods require managerial authority,
  and all postings stay tenant/company scoped and auditable.
- **Migration / compatibility:** Do not treat current local accounting evidence
  as GL truth. Preserve it, map it with provenance, reconcile before promotion,
  and require an approved Asan acceptance process for final import.
- **Tests / acceptance:** Sales Invoice receivable, accepted Supplier Invoice
  payable, Treasury posting, advances, cheques, returns/refunds, forgiveness,
  correction, period control, whole-Toman rounding, tax configuration and
  balanced journal/report reconciliation pass.
- **Rollback:** Forward-only compensating entries and feature-gated posting;
  never delete an accepted journal or legacy source evidence.

## Phase 5 — CRM Intelligence

- **Scope / ownership:** CRM owns Lead, Opportunity, Contact history and
  follow-up; Automation/Scheduler/Notification execute approved reminders.
- **Database / API / frontend:** Add CRM workflow records and APIs, follow-up
  workspace, customer scoring/inactivity views and notification links.
- **Security / audit:** Scope history to authorized users, audit outcome/task
  changes, and keep scoring advisory with no automatic financial mutation.
- **Migration / compatibility:** Preserve existing CRM notes/history and map
  them to Contact; avoid duplicate task creation with idempotency keys.
- **Tests / acceptance:** Missed expected purchase creates one follow-up,
  three-day satisfaction reminder, issue capture, role visibility, idempotency,
  Persian/English behavior and responsive UI pass.
- **Rollback:** Disable rules/schedules without deleting interaction history or
  notification evidence.

## Phase 6 — Reports / Integrations / Migration

- **Scope / ownership:** Reporting reads domain facts; Legacy Asan retains source
  staging/reconciliation; Market Sync supplies policy-controlled price input;
  external integrations remain bounded adapters.
- **Database / API / frontend:** Add read models/reports only through owned
  domain contracts, reconciliation acceptance UI, and authorized integration
  adapters where contracts exist.
- **Security / audit:** Enforce reporting scope, keep import provenance,
  idempotency and archive controls, and never expose credentials or bypass
  domain owners.
- **Migration / compatibility:** Final Asan promotion requires approved mapping,
  reconciliation, acceptance evidence, historical preservation and a tested
  rollback/compensating strategy.
- **Tests / acceptance:** Report totals reconcile with source domains; Asan
  staging/reconciliation/acceptance, Market Sync safety and authorized adapter
  failure paths pass; no canonical write occurs before acceptance.
- **Rollback:** Preserve staging and mapping evidence; disable adapters/promote
  only through compensating, auditable operations.

## Deferred Backlog

Repairs, Warranty, Installments, Payroll, leave workflows, expanded Customer
Portal, Web Push delivery, external Market Sync publication contracts, and
canonical file-storage policy remain P1/P2 backlog and are not part of Phases
0–6 unless separately approved.
