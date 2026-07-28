---
id: HIGH-008
title: Implement approved Sales and Orders context
status: blocked
priority: high
type: feature
area: sales
owners: []
depends_on: [HIGH-001, HIGH-004, HIGH-006, HIGH-007, HIGH-010]
blocks: [HIGH-009, MED-004]
estimated_size: XL
risk: high
source_refs: [PROJECT_BACKLOG/high/HIGH-007-sales-orders-architecture.md]
---

# Implement approved Sales and Orders context

## Summary
Implement order capture, pricing snapshot, reservation, allocation,
fulfillment, cancellation and audit.

## Why this is needed
Sales is a core operational ERP capability.

## Current evidence
No executable Sales implementation exists.

## Existing implementation
Upstream CRM/Product and future Inventory/Pricing contracts only.

## Missing work and scope
All HIGH-007 slices; invoices/payments/refunds remain HIGH-009.

## Dependencies
HIGH-001, HIGH-004, HIGH-006, HIGH-007 and HIGH-010.

## Business decisions required
Resolved in HIGH-007.

## Proposed implementation approach
Incremental vertical slices from draft/order validation through reservation and fulfillment.

## Impacts
New schema/domain/API/permissions/UI/events and reporting feeds.

## Security and tenant-isolation considerations
Scope every command/query; audit overrides and sensitive customer data.

## Testing requirements
PostgreSQL concurrency, API contract, UI/component and E2E lifecycle tests.

## Documentation requirements
Sales module/workflow, failure recovery and operator guide.

## Acceptance criteria
- [ ] Approved order states and invariants are enforced.
- [ ] Pricing snapshots reproduce totals.
- [ ] Inventory reservations are atomic and idempotent.
- [ ] Partial/cancel flows reconcile all downstream state.

## Validation commands
Backend/frontend gates plus sales E2E suite.

## Risks and rollback considerations
Use compensating actions after fulfillment; never silently rewrite financial snapshots.

## Relevant files
Future sales context and upstream contracts.

## Notes for the next developer or AI agent
Remain blocked until all declared dependencies are done.
