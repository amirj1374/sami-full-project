# Architecture decision records

ADRs record confirmed decisions, not speculative designs. Current repository
evidence supports these accepted decisions:

| ID | Decision | Evidence | Rationale confidence |
|---|---|---|---|
| ADR-001 | One monorepo with backend and frontend tracked directly | root Git tree and `AGENTS.md` | High; detailed historical rationale unknown |
| ADR-002 | Spring Boot modular monolith plus Vue SPA | build files and source layout | High; alternative analysis unknown |
| ADR-003 | Flyway exclusively owns PostgreSQL schema | configuration and migrations | High |
| ADR-004 | Database-driven RBAC with JWT staff auth | migrations, security and controllers | High |
| ADR-005 | Backend package-by-domain | executable package layout | High |
| ADR-006 | In-process Spring events for current domain integration | publishers/listeners | High; durability trade-off remains open |
| ADR-007 | Registry/SPI extension for providers and jobs | provider and job registries | High |

## Template

```markdown
# ADR-NNN: Short decision

- Status: Proposed | Accepted | Superseded | Rejected
- Date:
- Owners:

## Context
Verified problem and constraints.

## Decision
The chosen rule.

## Alternatives
Options actually considered; write `Unknown` when history is unavailable.

## Consequences
Positive, negative, operational and migration effects.

## Evidence
Repository paths, tests and related ADRs.
```
