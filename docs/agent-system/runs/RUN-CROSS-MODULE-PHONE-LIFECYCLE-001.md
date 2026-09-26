# Run CROSS-MODULE-PHONE-LIFECYCLE-001

- **Contract:** `AUTH-CROSS-MODULE-PHONE-LIFECYCLE`
- **Status:** COMPLETED — backend/service and PostgreSQL E2E certified; UI
  rendered inspection not executable in this environment (no browser surface).
- **Starting revision:** `717dc3a` on `development`
- **Environment:** Pending isolated PostgreSQL-backed authenticated UI run.

## Discovery checkpoint

- Product/Variant-compatible Inventory receipt and reservation public APIs
  exist.
- Current Purchasing order/receipt contracts are Product-only and pass empty
  serial lists to Inventory.
- Current Sales order stores Variant identity, but Delivery omits it from the
  physical-issue command. A Variant order can therefore reserve inventory but
  cannot reliably issue its exact Variant.
- These are integration defects within approved existing rules, not Product
  decision gaps.

## Corrections

- V80 preserves Product/Variant/Serial provenance on Purchasing PO/GR lines,
  Sales order/delivery/invoice lines, and serial-unit identifiers.
- Supplier creation now writes the authenticated tenant explicitly; this fixed
  a real cross-tenant purchase rejection.
- Variant receipt, reservation, issue and movement provenance are carried by
  the Inventory public boundary; duplicate receipt/issue remains idempotent.
- Permanent `PhonePurchaseToSalePostgresAcceptanceIT` protects purchase →
  receipt → stock/IMEI → reservation → delivery issue → invoice/receivable.

## Validation Evidence

- Fresh disposable PostgreSQL 16.15: Flyway V1→V80 PASS; E2E test 1/0/0.
- Supported migration harness V50→V80: 2/0/0.
- `SalesBusinessFixturePostgresIT`: 3/0/0.
- Full Maven backend regression: 315/0/0.
- Frontend type-check PASS; frontend tests 56/0/0; production build PASS.
- Direct read-back: receipt/reserve/issue movements retain the same
  tenant/product/variant; issued serial quantity is one; on-hand variant
  balance is zero; one idempotent accounting receivable exists.

## UI Quality / Limitation

- Frontend source/type/build and existing browser-independent UI regression
  passed. CUA reported no available browser provider, so authenticated
  rendered desktop/tablet/mobile inspection could not be performed and is not
  claimed as PASS. No UI defect was silently hidden.

## Guardian Review

- PASS for approved cross-module ownership, tenant isolation, identity
  preservation, idempotency, migration safety and evidence scope. Browser
  limitation remains explicitly recorded above.

## Hardened QA Recheck

- The repository now requires the independent `real-user-acceptance` skill for
  material user-facing journeys. Its gate requires action, visible, business,
  persisted and cross-screen evidence, refresh/reopen, negative/idempotency
  checks, runtime/network inspection and explicit unavailable-state labels.
- Framework regression: frontend tests 57/0/0, including the permanent
  framework-marker test.
- Phone journey under the hardened standard: `BLOCKED BY HARNESS` for rendered
  authenticated browser execution because no browser provider is available in
  this environment. Prior backend/PostgreSQL evidence remains valid but is not
  promoted to browser certification.
