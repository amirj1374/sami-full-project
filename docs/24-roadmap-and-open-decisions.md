# Roadmap and open decisions

This is a dependency-oriented decision register, not a delivery commitment.

## Cross-cutting decisions

1. Final tenant/company/branch/store context and cross-scope administration.
2. Staff authentication storage, CSRF and token revocation model.
3. General event durability, ordering and idempotent consumption.
4. Canonical file ownership and object-storage strategy.
5. Testcontainers/CI quality gate and frontend test toolchain.
6. Observability, backup, recovery and production secret management.

## Before Inventory

Define warehouse/store/location boundaries, immutable stock ledger, serialized
versus quantity stock, reservations, valuation, purchasing receipt contract,
concurrency, stock count, returns and accounting events.

## Before Sales

Define order lifecycle, customer/branch scope, pricing snapshot, reservation
contract, fulfillment, cancellation/return, invoice ownership and idempotency.

## Before Pricing

Define price-list ownership, currency/tax/rounding, customer segmentation,
promotion priority/stacking, cost integration and historical explanation.

## Before Payments

Define payment intent/attempt/refund states, provider ownership, reconciliation,
PCI boundary, webhook authentication, idempotency and sales/accounting links.

## Before Accounting

Define chart of accounts, fiscal periods, double-entry journal ownership,
posting rules, reversals, valuation method, currency/tax policy and immutable
links to source documents.
