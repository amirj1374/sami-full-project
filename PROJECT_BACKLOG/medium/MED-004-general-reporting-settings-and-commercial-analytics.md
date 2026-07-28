---
id: MED-004
title: Define general reporting settings and commercial analytics
status: needs-decision
priority: medium
type: architecture
area: reporting
owners: []
depends_on: [HIGH-004, HIGH-006, HIGH-008, HIGH-009]
blocks: []
estimated_size: L
risk: medium
source_refs: [sami-backend/src/main/java/com/sami/app/dashboard, sami-backend/src/main/java/com/sami/app/dashboard/spi/ReportingProvider.java, sami-frontend/src/views/DashboardReportsView.vue]
---

# Define general reporting settings and commercial analytics

## Summary
Set ownership for cross-module reports, exports, global/business settings and
future inventory/sales/finance analytics.

## Why this is needed
Dashboard and module-specific reports exist, but no confirmed general reporting
or settings bounded context covers future commercial data.

## Current evidence
Dashboard/KPI infrastructure and several module reports exist; commercial core
modules do not yet exist.

## Existing implementation
Dashboard views/providers, CSV exports and configurable metadata foundations.

## Missing work and scope
Report ownership, semantic metrics, settings hierarchy, access/export privacy,
snapshot/freshness and performance strategy.

## Dependencies
Operational commercial modules and finance boundary.

## Business decisions required
Required reports/KPIs, fiscal definitions, export formats, retention and
tenant/company/branch visibility.

## Proposed implementation approach
Metric catalogue and settings ownership first; reuse dashboard provider
extension points; avoid direct ad-hoc cross-module repository coupling.

## Impacts
Dashboard/report backend, UI, query projections and possibly new read models.

## Security and tenant-isolation considerations
Scoped aggregation, export permissions and PII minimization.

## Testing requirements
Metric fixtures, scope/permission, performance and export tests.

## Documentation requirements
Metric dictionary, freshness and reconciliation rules.

## Acceptance criteria
- [ ] Owners approve metric and settings catalogue.
- [ ] Each measure has source, formula, scope and freshness.
- [ ] Reports reconcile to source transactions.
- [ ] Permissions and exports are tested.

## Validation commands
Report fixture tests, query plans, frontend gates and reconciliation checks.

## Risks and rollback considerations
Do not cache or denormalize before source contracts stabilize.

## Relevant files
Dashboard package/views and module report services.

## Notes for the next developer or AI agent
Do not label a dashboard widget as a general ledger/reporting system.
