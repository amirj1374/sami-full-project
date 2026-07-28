---
id: HIGH-007
title: Approve Sales, Orders and Fulfillment architecture
status: needs-decision
priority: high
type: architecture
area: sales
owners: []
depends_on: [CRIT-001, HIGH-002, HIGH-003, HIGH-005]
blocks: [HIGH-008, HIGH-009]
estimated_size: L
risk: high
source_refs: [docs/00-project-overview.md, docs/24-roadmap-and-open-decisions.md, sami-backend/src/main/java/com/sami/app/crm]
---

# Approve Sales, Orders and Fulfillment architecture

## Summary
Define quotation/order/allocation/fulfillment/cancellation lifecycle and its
contracts with CRM, Pricing and Inventory.

## Why this is needed
No Sales owner exists and implementation would otherwise duplicate stock and price rules.

## Current evidence
No sales package, controller, migration or executable UI exists.

## Existing implementation
Customer, Product, organization, purchasing and future dependency designs.

## Missing work and scope
Owner-approved aggregates, states, invariants, permissions, idempotency and
integration contracts. Invoicing/payment/refund internals are HIGH-009.

## Dependencies
CRIT-001, HIGH-002, HIGH-003 and HIGH-005.

## Business decisions required
Quote/order states, partial fulfillment, channel/store ownership, cancellations,
backorders, approvals and document numbering.

## Proposed implementation approach
DDD/ADR with immutable commercial snapshots and synchronous reservation/price
contracts; publish durable lifecycle events.

## Impacts
New backend/frontend/schema/API and integrations.

## Security and tenant-isolation considerations
Scope orders to tenant/company/branch/store and protect overrides/export.

## Testing requirements
State-machine, idempotency, concurrency and cross-context contract scenarios.

## Documentation requirements
Lifecycle diagrams, API/events and operational failure handling.

## Acceptance criteria
- [ ] Sales owner and lifecycle are approved.
- [ ] Price snapshot and inventory reservation contracts are explicit.
- [ ] Cancellation/partial fulfillment rules are deterministic.
- [ ] Invoice/payment hand-off is defined.

## Validation commands
Architecture review with sales, inventory and finance owners.

## Risks and rollback considerations
Order state and numbering are hard to migrate; use immutable events/snapshots.

## Relevant files
CRM, Product, tenancy/organization and HIGH-003/HIGH-005 designs.

## Notes for the next developer or AI agent
Do not implement stock or price calculation inside Sales.
