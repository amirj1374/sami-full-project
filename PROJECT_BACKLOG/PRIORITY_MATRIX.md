# Priority matrix

## Critical

- CRIT-001: tenant and organization isolation.
- CRIT-002: production authentication and secret hardening.

## High-value foundation

HIGH-001 database tests, HIGH-002 product identity, HIGH-010 durable events,
HIGH-011 file ownership/storage and HIGH-012 scheduler reliability.

## Operational ERP core

Inventory architecture/implementation (HIGH-003/004), followed by Sales
architecture/implementation (HIGH-007/008).

## Commercial and finance domain

Pricing architecture/implementation (HIGH-005/006), then the
invoice/payment/return/accounting boundary (HIGH-009).

## Quality and security

CRIT-001/002, HIGH-001, MED-001 and MED-005.

## Production readiness

HIGH-011–014 cover files, scheduler, external communication, telemetry,
backup/restore and release gating.

## Suggested order

1. Run the completed LOW-001 documentation gate; unblock HIGH-001 by providing
   Java 21, Maven and Docker/Testcontainers.
2. Resolve CRIT-001 and CRIT-002 owner decisions.
3. Approve HIGH-002 and HIGH-010.
4. Design Inventory and Pricing in parallel where dependencies permit.
5. Implement Inventory/Pricing only after designs and database gate pass.
6. Approve then implement Sales.
7. Decide finance boundaries and production integrations.
8. Complete portal/partial UIs and cross-domain reporting after their owners stabilize.
