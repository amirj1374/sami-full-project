# DEC-P5-001 — CRM Intelligence Policy

- **Status:** APPROVED
- **Date:** 2026-09-24
- **Authority:** Owner decision recorded for active Phase 5 authorization
- **Scope:** CRM inactivity detection, advisory scoring, explanations and recovery suggestions

## Decision

CRM inactivity is personalized from each customer's completed-purchase history
when sufficient history exists. The expected interval must be robust to outliers;
customers without sufficient history are explicitly classified as having
insufficient history rather than receiving an invented cadence.

Customer intelligence is advisory only and cannot mutate Sales, Accounting,
Treasury, Inventory, Purchasing, prices, credit, permissions or transactions.

The initial configurable score dimensions and default weights are:

- Recency 35%
- Frequency 25%
- Monetary Value 25%
- Relationship Trend 15%

Scores are explainable, normalized to 0–100, tenant-scoped and configuration
auditable. Explanations must derive from recorded behavior and must not invent
psychological motives. States use factual language such as normal, attention
recommended, unusually inactive, declining activity and insufficient history.

Recovery suggestions are advisory existing CRM actions only; no automatic
contact, discount, price, financial incentive or accounting-status change.

## Implementation Boundary

Agents may choose deterministic cadence, normalization, thresholds, persistence,
API, cache, index and UI implementations within approved architecture. CRM may
read authoritative Sales/customer facts but does not become a source of truth
for those domains. Effective configuration is tenant-scoped and changes do not
rewrite historical transactions.

## Affected Scope

Unblocks P5-S4 advisory intelligence and dependent CRM UI/final certification.
