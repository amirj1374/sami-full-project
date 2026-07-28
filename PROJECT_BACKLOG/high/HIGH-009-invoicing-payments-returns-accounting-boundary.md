---
id: HIGH-009
title: Define invoicing payments returns and accounting boundary
status: needs-decision
priority: high
type: architecture
area: finance
owners: []
depends_on: [CRIT-001, HIGH-003, HIGH-005, HIGH-007]
blocks: [MED-004]
estimated_size: XL
risk: high
source_refs: [docs/24-roadmap-and-open-decisions.md]
---

# Define invoicing payments returns and accounting boundary

## Summary
Define invoice, payment intent/attempt, refund, exchange, sales return and
accounting posting ownership before implementation.

## Why this is needed
Financial records require immutable totals, auditable reversals and provider idempotency.

## Current evidence
No confirmed invoicing, payment, sales-return or accounting bounded context exists.

## Existing implementation
CRM, Purchasing and future Sales/Pricing/Inventory are upstream.

## Missing work and scope
Business architecture, regulatory/tax decisions, provider boundaries and
implementation backlog decomposition.

## Dependencies
Approved Inventory, Pricing and Sales designs.

## Business decisions required
Invoice lifecycle/numbering, taxes, payment methods/providers, partial/refund
rules, exchange valuation, accounting scope, fiscal periods and currencies.

## Proposed implementation approach
Separate immutable invoice, payment orchestration and accounting journal
boundaries with idempotent contracts and compensating reversals.

## Impacts
New backend/frontend/schema/APIs, provider SPIs and cross-context events.

## Security and tenant-isolation considerations
Financial separation of duties, PCI boundary, webhook authentication and scope.

## Testing requirements
Money/property tests, provider idempotency, webhook replay, reversal and reconciliation tests.

## Documentation requirements
Finance glossary, lifecycle, posting rules, provider and reconciliation runbooks.

## Acceptance criteria
- [ ] Context ownership and regulatory decisions are approved.
- [ ] Historical totals cannot be recomputed from mutable rules.
- [ ] Payment/refund idempotency and reconciliation are defined.
- [ ] Return/exchange effects on stock and accounting are explicit.

## Validation commands
Architecture, finance and security review.

## Risks and rollback considerations
Financial history must be corrected by reversal, never destructive updates.

## Relevant files
Future contexts plus purchasing, CRM and approved upstream designs.

## Notes for the next developer or AI agent
Create separate implementation tasks after this design is accepted.
