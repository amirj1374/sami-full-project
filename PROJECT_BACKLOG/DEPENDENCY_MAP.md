# Dependency map

```mermaid
flowchart LR
  C1["CRIT-001 Tenant scope"] --> C2["CRIT-002 Auth hardening"]
  C1 --> H2["HIGH-002 Product variants"]
  H1["HIGH-001 DB tests"] --> H10["HIGH-010 Outbox"]
  C1 --> H3["HIGH-003 Inventory design"]
  H2 --> H3
  C1 --> H5["HIGH-005 Pricing design"]
  H2 --> H5
  H1 --> H4["HIGH-004 Inventory implementation"]
  H3 --> H4
  H10 --> H4
  H1 --> H6["HIGH-006 Pricing implementation"]
  H5 --> H6
  H10 --> H6
  H3 --> H7["HIGH-007 Sales design"]
  H5 --> H7
  H4 --> H8["HIGH-008 Sales implementation"]
  H6 --> H8
  H7 --> H8
  H8 --> H9["HIGH-009 Finance boundary"]
  C2 --> H13["HIGH-013 Communication/OTP"]
  H10 --> H13
  H13 --> M3["MED-003 Portal"]
  H1 --> H12["HIGH-012 Scheduler"]
  C2 --> H14["HIGH-014 Production gate"]
  H12 --> H14
```

## Critical path

The commercial-core path is CRIT-001 → HIGH-002 → HIGH-003/HIGH-005 →
HIGH-004/HIGH-006 → HIGH-007 → HIGH-008 → HIGH-009. HIGH-001 and HIGH-010 are
parallel foundation gates that must join before implementation.

## Parallel work

- HIGH-001 and LOW-001 can start immediately.
- After CRIT-001 decisions, product identity and file ownership can proceed.
- Inventory and Pricing design can proceed in parallel after HIGH-002.
- MED-005 is independent.

No circular dependency is intended; automated validation is LOW-001.
