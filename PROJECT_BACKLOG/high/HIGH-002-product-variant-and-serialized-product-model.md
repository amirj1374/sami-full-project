---
id: HIGH-002
title: Define product variants and serialized-product identity
status: needs-decision
priority: high
type: architecture
area: product
owners: []
depends_on: [CRIT-001]
blocks: [HIGH-003, HIGH-005, HIGH-007]
estimated_size: L
risk: high
source_refs: [sami-backend/src/main/java/com/sami/app/product, sami-backend/src/main/resources/db/migration/V2__init_products.sql, sami-backend/src/main/java/com/sami/app/purchasing/service/PurchaseReceivingService.java]
---

# Define product variants and serialized-product identity

## Summary
Establish product, variant, SKU, brand/category, color, capacity, lot and
serial/IMEI ownership before Inventory, Pricing and Sales.

## Why this is needed
Current product CRUD and purchasing serial capture do not define a reusable
commercial identity model.

## Current evidence
Product has a basic executable CRUD; purchasing owns receipt IMEI/serial rows.
No canonical variant/SKU aggregate exists.

## Existing implementation
Product API/UI and purchase items/receipt identifiers.

## Missing work and scope
Domain design, uniqueness, lifecycle, import rules and compatibility mapping.
Inventory quantities and valuation are out of scope.

## Dependencies
Tenant/organization scope.

## Business decisions required
Variant dimensions, SKU generation, multiple IMEIs, serial uniqueness,
categories/brands, bundles and whether lots apply.

## Proposed implementation approach
ADR/domain contract, backward-compatible product expansion, public product
identity API and migration plan for existing purchase references.

## Impacts
Product backend/UI/schema/API and every future commercial module.

## Security and tenant-isolation considerations
Define global versus tenant catalog ownership and scoped uniqueness.

## Testing requirements
Variant-combination, uniqueness, serial-format, migration and API contract tests.

## Documentation requirements
Glossary, ER model, lifecycle and import/export contract.

## Acceptance criteria
- [ ] Product/variant/SKU/serial responsibilities are approved.
- [ ] Existing product and purchasing records have a migration mapping.
- [ ] Uniqueness and lifecycle invariants are executable.
- [ ] Backend/frontend contracts and tests are specified.

## Validation commands
Design review; later `mvn clean verify`, frontend build and PostgreSQL tests.

## Risks and rollback considerations
Identity changes have high fan-in; use additive schema and stable legacy IDs.

## Relevant files
`product`, `purchasing/domain`, V2 and V8, frontend product/purchase clients.

## Notes for the next developer or AI agent
Do not move IMEI ownership until Inventory boundaries are approved.
