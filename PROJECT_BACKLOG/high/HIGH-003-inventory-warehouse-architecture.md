---
id: HIGH-003
title: Approve Inventory and Warehouse bounded-context architecture
status: needs-decision
priority: high
type: architecture
area: inventory
owners: []
depends_on: [CRIT-001, HIGH-002]
blocks: [HIGH-004, HIGH-007, HIGH-009]
estimated_size: L
risk: high
source_refs: [docs/24-roadmap-and-open-decisions.md, sami-backend/src/main/java/com/sami/app/purchasing/service/PurchaseReceivingService.java, sami-backend/src/main/resources/db/migration/V8__purchasing.sql]
---

# Approve Inventory and Warehouse bounded-context architecture

## Summary
Define ownership, immutable ledger, projections, locations, reservations,
serial lifecycle, valuation, counts, returns and integration contracts.

## Why this is needed
Purchasing receipt data is not an auditable stock balance and cannot safely
support sales or accounting.

## Current evidence
No inventory package/controller/migration/ledger exists; purchasing records
receipt quantity and IMEI/serial information.

## Existing implementation
Product, supplier, purchasing, tenant/company/branch foundations and events.

## Missing work and scope
Implementation-ready architecture and owner-approved decisions. Code and
migrations are out of scope for this item.

## Dependencies
CRIT-001 and HIGH-002.

## Business decisions required
Warehouse/store/location boundaries, negative stock, allocation, serial states,
lot use, valuation method, approval thresholds and count policy.

## Proposed implementation approach
DDD model plus ADRs; immutable movement ledger and transactional balance
projection; synchronous command contracts and outbox integration events.

## Impacts
New backend/frontend bounded context, schema, APIs and purchasing/sales/accounting integrations.

## Security and tenant-isolation considerations
Tenant/company/branch/location scope, movement permissions and approval/audit.

## Testing requirements
State-machine, concurrency, property-based ledger, serial uniqueness and
integration-contract scenarios.

## Documentation requirements
Domain glossary, diagrams, API/events and migration plan.

## Acceptance criteria
- [ ] Owner approves boundaries and valuation.
- [ ] Aggregates, invariants and state machines are complete.
- [ ] Purchasing receipt and future sales contracts are decoupled.
- [ ] concurrency, idempotency and outbox decisions are recorded.
- [ ] Implementation slices are estimable.

## Validation commands
Architecture review and schema/API contract review.

## Risks and rollback considerations
Wrong ledger ownership creates irreversible stock/accounting inconsistencies.

## Relevant files
Purchasing receipt/domain, product module, tenancy/organization migrations.

## Notes for the next developer or AI agent
Do not implement stock as a mutable quantity column without the approved design.
