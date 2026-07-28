---
id: MED-002
title: Deliver administration UIs for backend-only modules
status: proposed
priority: medium
type: feature
area: frontend
owners: []
depends_on: [CRIT-001, MED-001]
blocks: []
estimated_size: XL
risk: medium
source_refs: [sami-backend/src/main/java/com/sami/app/automation, sami-backend/src/main/java/com/sami/app/dataquality, sami-backend/src/main/java/com/sami/app/metadata, sami-backend/src/main/java/com/sami/app/knowledge, sami-backend/src/main/java/com/sami/app/licensing, sami-backend/src/main/java/com/sami/app/scheduling, sami-frontend/src/views/PlaceholderView.vue]
---

# Deliver administration UIs for backend-only modules

## Summary
Replace lifecycle placeholders with usable, permission-aware UIs for
Automation, Data Quality, Metadata, Knowledge, Licensing and Scheduling.

## Why this is needed
Substantial backend APIs exist but ordinary users cannot operate them through
verified dedicated screens.

## Current evidence
Controllers/services/migrations exist; no matching static frontend views/API
clients exist for these modules and dynamic navigation uses PlaceholderView.

## Existing implementation
Backend APIs, dynamic menu/lifecycle metadata and reusable Vue patterns.

## Missing work and scope
Per-module UX discovery, typed clients, views/forms/tables, permissions,
localization, tests and workflow documentation.

## Dependencies
Tenant decisions and frontend quality infrastructure.

## Business decisions required
Operational roles and which module UI ships first.

## Proposed implementation approach
Split into module-specific child tasks after endpoint/permission audit; deliver
one end-to-end module at a time.

## Impacts
Primarily frontend; backend gaps discovered by contract validation require separate scope.

## Security and tenant-isolation considerations
Presentation checks mirror—but never replace—backend permissions.

## Testing requirements
Component, contract, accessibility, RTL/LTR and E2E tests per module.

## Documentation requirements
Module operator/workflow pages and status updates.

## Acceptance criteria
- [ ] Each selected module has an approved workflow and role map.
- [ ] Placeholder is replaced only when real functionality exists.
- [ ] API/types/locales/permissions remain aligned.
- [ ] Responsive RTL/LTR and tests pass.

## Validation commands
Frontend lint/test/type-check/build and module E2E.

## Risks and rollback considerations
Do not expose incomplete backend actions merely to remove placeholders.

## Relevant files
Listed backend packages, frontend router/menu/view/component infrastructure.

## Notes for the next developer or AI agent
Create child tasks; do not implement all modules as one change.
