---
id: HIGH-014
title: Establish production observability backup recovery and release gate
status: needs-decision
priority: high
type: operations
area: operations
owners: []
depends_on: [CRIT-002, HIGH-001, HIGH-012]
blocks: []
estimated_size: XL
risk: high
source_refs: [sami-backend/src/main/resources/application.yml, sami-backend/docker-compose.prod.yml, .github/workflows/ci.yml, docs/17-deployment-and-operations.md, docs/19-observability.md]
---

# Establish production observability backup recovery and release gate

## Summary
Add measurable health, metrics, tracing/logging, alerts, backup/restore/DR and a
repeatable production-readiness gate.

## Why this is needed
Actuator exposes health/info only; no repository-defined backup, restore, alert,
SLO or disaster-recovery system exists.

## Current evidence
Compose health checks and CI builds exist; production operations remain minimal.

## Existing implementation
Actuator, logs, audit tables, Docker topology and basic CI.

## Missing work and scope
Telemetry stack/contracts, redaction, dashboards/alerts, backup automation,
restore drills, RPO/RTO, deployment validation and rollback criteria.

## Dependencies
Security hardening, database integration tests and scheduler reliability.

## Business decisions required
Hosting platform, SLO/SLA, retention, RPO/RTO, on-call ownership and budget.

## Proposed implementation approach
Define SLOs; instrument critical paths; implement backup and isolated restore
test; codify pre/post-deploy checklist and release-gate evidence.

## Impacts
Backend observability config, deployment/CI and operational systems; limited UI.

## Security and tenant-isolation considerations
Redact secrets/PII and restrict metrics, logs, backups and restore access.

## Testing requirements
Alert tests, backup restore drill, health degradation and deployment smoke tests.

## Documentation requirements
Runbooks, dashboard ownership, escalation and disaster-recovery plan.

## Acceptance criteria
- [ ] SLO/RPO/RTO and owners are approved.
- [ ] Critical metrics/logs/traces and alerts are live.
- [ ] Encrypted backups and isolated restores are verified.
- [ ] Release gate blocks missing evidence.
- [ ] Rollback limitations are documented.

## Validation commands
Release-gate checklist, restore drill, health/smoke tests and secret scan.

## Risks and rollback considerations
Telemetry may expose data and backup changes risk retention gaps; stage and audit access.

## Relevant files
Actuator config, Compose, CI, scheduler/communication/files operational paths.

## Notes for the next developer or AI agent
Infrastructure access is required; do not simulate a passing production drill.
