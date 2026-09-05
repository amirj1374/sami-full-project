# Domain model

## Confirmed business concepts

| Concept | Current owner | Notes |
|---|---|---|
| Tenant | `licensing`, `common.tenancy` | Trusted isolation root resolved from the authenticated server-side principal |
| Company / Branch / Store | organization migrations and shared mappings | Organizational hierarchy; lifecycle corrections exist in V26 |
| Staff User / Role / Permission | `user`, `authz`, `security` | Database-driven RBAC; super-admin bypass |
| Counterparty | Approved business model | One Natural Person or Legal Entity/Company/Store identity with one unified business-facing financial account; current source still stores Customer/Supplier separately |
| Customer / Supplier roles | `crm` / `supplier` current source | Commercial roles may coexist on the same approved Counterparty; they must not create contradictory separate balances |
| Product | `product` | Catalog owner; legacy integer stock is an Inventory compatibility projection |
| Purchase | `purchasing` | Items, approvals, receiving, IMEI/serial capture, returns, logs; receipt/return stock posts to Inventory |
| Inventory | `inventory` | Warehouses, locations, balances, append-only movements, serials, reservations, transfers, counts, valuation and audit |
| Sale | `sales` | Orders, lifecycle, payments, returns and Inventory reservation/issue integration |
| Dashboard / KPI / Widget | `dashboard` | Configurable dashboard and calculation infrastructure |
| Managed file | `files` | Versioning, metadata, scans, derivatives, retention and quota |
| Scheduled job | `common.scheduler` | Handler registry, polling, locking, executions and timeout |

## Current implementation relationships

The following diagram records the current source relationships. It is not a
claim that the approved unified Counterparty financial-account model has already
been implemented.

```mermaid
erDiagram
  TENANT ||--o{ COMPANY : contains
  COMPANY ||--o{ BRANCH : contains
  BRANCH ||--o{ STORE : contains
  TENANT ||--o{ USER : scopes
  USER }o--o{ ROLE : assigned
  ROLE }o--o{ PERMISSION : grants
  TENANT ||--o{ CUSTOMER : owns
  TENANT ||--o{ SUPPLIER : owns
  TENANT ||--o{ PRODUCT : owns
  TENANT ||--o{ WAREHOUSE : owns
  WAREHOUSE ||--o{ INVENTORY_LOCATION : contains
  INVENTORY_LOCATION ||--o{ INVENTORY_BALANCE : holds
  PRODUCT ||--o{ INVENTORY_BALANCE : stocked_as
  PRODUCT ||--o{ INVENTORY_MOVEMENT : traced_by
  SUPPLIER ||--o{ PURCHASE : supplies
  PURCHASE ||--|{ PURCHASE_ITEM : contains
```

## Invariants visible in architecture

- Backend permissions—not frontend visibility—protect operations.
- Flyway is the schema owner; entity mappings must validate against it.
- New tenant-scoped operations resolve the trusted tenant through `TenantContext`
  and fail closed when no authenticated tenant is available.
- Lifecycle transitions are service operations, not arbitrary status updates.
- Purchasing owns receipt evidence; Inventory owns the resulting canonical balance,
  movement, serial and valuation state.
- Auditable entities inherit common ID/timestamp conventions where applicable.

## Undefined future ownership

Sales owns approved price-list/price-resolution policy; current source does not
yet implement the approved model. Accounting still requires explicit bounded
context and integration contracts before it can own canonical journals and
financial statements.
