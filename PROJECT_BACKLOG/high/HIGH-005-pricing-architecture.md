---
id: HIGH-005
title: Approve Pricing, Price Lists and Promotions architecture
status: needs-decision
priority: high
type: architecture
area: pricing
owners: []
depends_on: [CRIT-001, HIGH-002]
blocks: [HIGH-006, HIGH-007, HIGH-009]
estimated_size: L
risk: high
source_refs: [docs/24-roadmap-and-open-decisions.md, sami-backend/src/main/java/com/sami/app/product, sami-backend/src/main/java/com/sami/app/crm]
---

# Approve Pricing, Price Lists and Promotions architecture

## Summary
Define deterministic base/list/customer/branch pricing, discounts, promotions,
tax interaction, rounding, history and explanation contracts.

## Why this is needed
No source of truth exists for sale price or reproducible invoice calculations.

## Current evidence
No pricing package, API, migration or evaluation engine exists.

## Existing implementation
Product, CRM segments, purchasing costs and organization foundations can become inputs.

## Missing work and scope
Architecture and contracts only; implementation is HIGH-006.

## Dependencies
Tenant/organization and product variants.

## Business decisions required
Currency, tax-inclusive/exclusive policy, rounding, stacking, priority, margin
limits, wholesale/retail and approval thresholds.

## Proposed implementation approach
Versioned price lists/rules, deterministic evaluation pipeline, immutable
calculation explanation and synchronous quote API.

## Impacts
New backend/frontend/schema/API; integrations with Product, CRM, Purchasing,
Sales, Inventory and Accounting.

## Security and tenant-isolation considerations
Prevent personalized-price leakage and scope lists/rules correctly.

## Testing requirements
Scenario tables, property-based determinism/rounding, priority/stacking and
historical replay tests.

## Documentation requirements
Glossary, algorithm, examples, API and event contracts.

## Acceptance criteria
- [ ] Source of truth and precedence are approved.
- [ ] Historical invoice reproducibility is designed.
- [ ] Tax/currency/rounding decisions are explicit.
- [ ] Explanation and simulation contracts are machine-readable.
- [ ] Implementation slices are estimable.

## Validation commands
Architecture and finance/product-owner review.

## Risks and rollback considerations
Ambiguous rule precedence can produce financial loss; version rules immutably.

## Relevant files
Product, CRM segments, purchasing costs, tenant/organization and future Sales.

## Notes for the next developer or AI agent
Do not embed pricing rules in frontend or Sales.
