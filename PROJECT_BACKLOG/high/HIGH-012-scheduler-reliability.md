---
id: HIGH-012
title: Make scheduler timeout retry and execution reliable
status: proposed
priority: high
type: defect
area: scheduler
owners: []
depends_on: [HIGH-001]
blocks: [HIGH-014]
estimated_size: M
risk: high
source_refs: [sami-backend/src/main/java/com/sami/app/common/scheduler/service/JobRunner.java, sami-backend/src/main/java/com/sami/app/common/scheduler/spi/JobHandler.java, sami-backend/src/main/resources/db/migration/V24__master_scheduler.sql]
---

# Make scheduler timeout retry and execution reliable

## Summary
Ensure timeout semantics, bounded execution, retries, leases and duplicate-run
behavior match a documented reliability contract.

## Why this is needed
`CompletableFuture.cancel(true)` does not guarantee handler termination and the
cached executor is unbounded.

## Current evidence
`JobRunner` records timeout then cancels a future; handlers have no cooperative
cancellation contract.

## Existing implementation
Persistent jobs/executions, polling, locking, handlers, timeout and failure counts.

## Missing work and scope
Reproduction tests, cancellation/execution decision, bounded executor, lease
recovery, retry/idempotency policy and metrics.

## Dependencies
PostgreSQL integration tests.

## Business decisions required
At-most-once versus at-least-once per job type and acceptable timeout behavior.

## Proposed implementation approach
First add deterministic tests; then use cooperative cancellation or isolated
execution, bounded pools and explicit retry-safe handler contracts.

## Impacts
Scheduler backend, handler SPI, operational monitoring; no schema change unless leases evolve.

## Security and tenant-isolation considerations
Propagate tenant context and restrict manual retry/cancel.

## Testing requirements
Timeout continuation, duplicate polling, crash recovery, retry and pool-saturation tests.

## Documentation requirements
Handler contract and scheduler failure runbook.

## Acceptance criteria
- [ ] Timeout behavior is reproducible and documented.
- [ ] Executor capacity is bounded.
- [ ] Duplicate/lease/retry semantics are tested.
- [ ] Non-idempotent jobs cannot be blindly replayed.

## Validation commands
Focused scheduler tests plus `mvn clean verify`.

## Risks and rollback considerations
Changing scheduling semantics can duplicate/skip work; deploy with metrics and controlled jobs.

## Relevant files
`common/scheduler`, registered job handlers, V24.

## Notes for the next developer or AI agent
Diagnose before implementing; do not merely change the timeout number.
