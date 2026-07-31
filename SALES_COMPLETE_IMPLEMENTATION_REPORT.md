# Sales Complete Implementation Report

## Result

SAMI now has a real tenant-scoped Sales bounded context and a corresponding
Vue/Vuetify workspace. The implementation uses authenticated `TenantContext`,
real customer/product references, server-calculated immutable commercial
snapshots, persisted payments/approvals/returns/audit/events, stock effects,
balanced accounting evidence, reporting, Persian-safe CSV, and real APIs.

## Backend architecture

- Aggregate: Sale, items, services, payments, discounts, returns and audit.
- Lifecycle: Draft, Confirmed, Completed, Cancelled, Partially Returned, Returned.
- Persistence: Flyway V28, optimistic versions, tenant-aware uniqueness, invoice
  sequences, serialized identifier uniqueness, stock ledger and accounting rows.
- Security: twelve `sales:*` permissions enforced by controller annotations.
- Isolation: trusted tenant on every aggregate read/write and reference check;
  company/branch membership is validated server-side.
- Integrations: Product stock, CRM customer references, application events,
  accounting extension records, and advisory AI provider SPI.
- AI: provider results are persisted; when no provider exists, the API records
  and returns `UNAVAILABLE` and never fabricates a recommendation.

## API surface

`GET/POST /api/v1/sales`, `GET/PUT /api/v1/sales/{id}`, confirm, complete,
cancel, return, payment, discount request/decision/history, audit, dashboard,
CSV report export and AI recommendation endpoints are implemented with the
standard response envelope and validation.

## Frontend

- Dashboard metrics and recent Sales workspace.
- Desktop server table and mobile invoice cards.
- Customer/product-backed draft editor with dynamic lines and IMEI fields.
- Invoice detail and permission-controlled lifecycle actions.
- Payment allocations, discount requests and manager decisions.
- Full remaining-quantity returns and audit timeline.
- Responsive fullscreen mobile dialogs, touch targets and print action.
- English/Persian parity with translated statuses, types and actions.

## Verification

- Backend Docker/JDK21/Maven package: passed.
- Fresh PostgreSQL 16 startup: passed; Flyway reported `28|sales|true` and
  Hibernate schema validation completed.
- Focused lifecycle and tenant-context tests: 7/7 passed.
- Broad backend suite: 147 tests passed; two existing path-sensitive contract
  tests failed only because the isolated backend container did not include the
  root Compose file or sibling frontend path.
- Frontend type-check: passed.
- Frontend production build: passed; 913 modules transformed.
- Locale parity: passed.

## Honest extension limits

- Cheque and installment allocations remain `PENDING` until real provider
  modules confirm them; Sales never reports a fake capture.
- AI returns auditable `UNAVAILABLE` until a provider implements the SPI.
- The current organization backend has schema but no lookup controller, so the
  frontend accepts company/branch IDs while the backend validates ownership.
- Existing Product stock is integer-based; serialized inventory uses the new
  canonical unit table and requires inbound inventory population.
