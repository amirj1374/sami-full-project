---
id: HIGH-010
title: Establish durable integration-event delivery
status: needs-decision
priority: high
type: architecture
area: integration
owners: []
depends_on: [HIGH-001]
blocks: [HIGH-004, HIGH-006, HIGH-008]
estimated_size: L
risk: high
source_refs: [sami-backend/src/main/java/com/sami/app/automation/engine/DomainEventBridge.java, sami-backend/src/main/java/com/sami/app/comm/service/CommunicationService.java, sami-backend/src/main/resources/db/migration/V27__communication_hub.sql]
---

# Establish durable integration-event delivery

## Summary
Define when process-local Spring events are sufficient and implement a
transactional outbox for cross-context effects that must survive crashes.

## Why this is needed
`AFTER_COMMIT` listeners are not crash-durable; future stock, order, price and
financial integrations require reliable delivery.

## Current evidence
Multiple modules publish Spring events. Communication persists messages in an
outbox-like table, but no general integration outbox exists.

## Existing implementation
Typed event records, automation bridge, scheduler and communication dispatcher.

## Missing work and scope
Event taxonomy, atomic persistence, publisher, retries, ordering, idempotent
consumers, retention, dead-letter/replay and monitoring.

## Dependencies
HIGH-001 for database verification.

## Business decisions required
Delivery SLA, ordering scope, retention, replay authority and broker need.

## Proposed implementation approach
ADR and PostgreSQL outbox first; keep internal ephemeral events where loss is acceptable.

## Impacts
Shared backend/schema/operations; no UI except admin monitoring.

## Security and tenant-isolation considerations
Events carry minimal scoped identifiers, never secrets; replay requires permission/audit.

## Testing requirements
Atomicity, crash/restart, duplicate delivery, ordering and poison-event tests.

## Documentation requirements
Event catalogue and replay/dead-letter runbook.

## Acceptance criteria
- [ ] Event durability classification is approved.
- [ ] Durable writes are atomic with business transactions.
- [ ] Consumers are idempotent and tenant-safe.
- [ ] Retry/dead-letter/replay are observable and audited.

## Validation commands
PostgreSQL integration and restart/failure-injection tests.

## Risks and rollback considerations
Dual publication can duplicate effects; migrate consumers explicitly.

## Relevant files
All `event` packages, automation bridge, communication services and V27.

## Notes for the next developer or AI agent
Do not assume every domain event belongs in the outbox.
