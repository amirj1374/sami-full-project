# Sales Domain Design

## Boundary and aggregate

`Sale` is the aggregate root. It owns immutable commercial line snapshots,
payment allocations, discount approvals, returns, audit history, invoice
numbering, stock movements, and accounting postings. Customer and Product remain
references to their owning modules; Sales never duplicates their master data.

The trusted tenant comes exclusively from `TenantContext`. Company and branch
are selected business scope values, but their membership is validated against
the trusted tenant before persistence. Every query includes `tenant_id`.

## Lifecycle

`DRAFT -> CONFIRMED -> COMPLETED` is the normal path. Drafts are editable.
Confirmation freezes pricing snapshots and reserves available stock. Completion
requires fully allocated successful payments, issues reserved stock, posts a
balanced accounting record, and assigns the invoice timestamp. A draft or
confirmed sale may be cancelled; reservations are released. Completed sales are
reversed through full or partial returns, never edited or deleted.

## Entities and relationships

- `sales`: aggregate header, trusted scope, totals, lifecycle and optimistic lock.
- `sale_items`: product reference plus immutable name/SKU/cost/price/tax snapshots.
- `sale_payments`: multiple allocations supporting CASH, CARD, TRANSFER, CHEQUE,
  WALLET and INSTALLMENT; provider references remain opaque.
- `sale_discounts`: requested/approved/rejected discount decisions and history.
- `sale_returns` and `sale_return_items`: immutable reversal documents.
- `sale_audit_history`: append-only actor/action/before/after evidence.
- `sale_numbers`: tenant/year sequence guarded by a unique key and row locking.
- `sales_inventory_units`: tenant/branch product stock and optional unique
  serial/IMEI ownership.
- `sales_stock_movements`: append-only reservation, issue, release and return ledger.
- `sale_accounting_entries`: balanced posting/reversal evidence and extension point.
- `sale_ai_recommendations`: advisory-only persisted recommendations; never mutates a sale.

## Business rules

- Quantity and monetary inputs are non-negative; quantity is strictly positive.
- Totals are calculated server-side using `HALF_UP` currency rounding.
- Historical product, price, tax and cost values are snapshots.
- Serialized units are tenant-unique and can be sold only once.
- Confirmation fails when stock is unavailable.
- Discounts or margin below configured request thresholds require approval.
- Completion requires approved discounts and payments equal to the final amount.
- Cancellation and returns append compensating stock/accounting records.
- Return quantities cannot exceed the unreturned completed quantity.
- AI results are advisory, auditable, tenant-scoped, and user-confirmed.

## API and security

The API is rooted at `/api/v1/sales` and uses the repository `ApiResponse`
envelope. Permissions are fine-grained: `sales:view`, `create`, `edit`,
`confirm`, `complete`, `cancel`, `return`, `payment`, `approve-discount`,
`view-audit`, `report`, and `export`. Client permission checks are presentation
only; controllers enforce every action.

List/detail, draft mutation, lifecycle, payments, discount approval, returns,
audit, stock lookup, dashboard and report endpoints use typed DTOs. Optimistic
versions and optional idempotency keys protect retries and concurrent edits.

## Integration contracts

Inventory, payment and accounting effects are real persisted Sales-owned
records today because those bounded contexts have no executable owner. Domain
events expose stable extension seams so future dedicated modules can consume or
replace orchestration without rewriting the Sale aggregate. No event or posting
claims external provider success unless that provider actually responds.
