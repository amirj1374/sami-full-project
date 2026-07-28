---
id: CRIT-001
title: Establish enforceable tenant and organization isolation
status: needs-decision
priority: critical
type: security
area: tenancy
owners: []
depends_on: []
blocks: [HIGH-003, HIGH-005, HIGH-007, HIGH-009, MED-003]
estimated_size: XL
risk: critical
source_refs: [sami-backend/src/main/java/com/sami/app/common/tenancy/TenantDefaults.java, sami-backend/src/main/resources/db/migration/V17__tenancy_write_defaults.sql, sami-backend/src/main/resources/db/migration/V22__organization.sql]
---

# Establish enforceable tenant and organization isolation

## Summary
Replace the temporary default-tenant bridge with authenticated, mandatory
tenant/company/branch/store scope across HTTP requests, jobs and persistence.

## Why this is needed
Inconsistent scope can expose or corrupt another tenant's data.

## Current evidence
`TenantDefaults` and V17 explicitly call themselves temporary scaffolds.
Organization tables exist, but one central repository-enforcement mechanism does not.

## Existing implementation
Many entities carry `tenant_id`; tenant, company, branch and store structures
and lifecycle migrations exist.

## Missing work
Scope semantics, trusted resolution, repository/write enforcement, job
propagation, cross-tenant administration and isolation tests.

## In scope
Architecture decision, inventory of tenant-bearing paths, phased removal of
defaults and enforcement tests. Out of scope: new business modules.

## Dependencies
Identity/RBAC and organization ownership decisions.

## Business decisions required
Can users span tenants/companies/branches? Is store an organizational or stock
boundary? Which records are global?

## Proposed implementation approach
ADR → scoped principal/context → query/write enforcement → background context →
forward migration removing defaults only after callers are compliant.

## Backend, frontend, database and API impact
Backend repositories/services and job context change; frontend may select an
allowed scope; new forward migrations remove unsafe defaults and strengthen
constraints; API scope headers/claims require an explicit contract.

## Security and tenant-isolation considerations
Fail closed when scope is absent; never trust client-supplied tenant IDs; audit
privileged scope changes.

## Testing requirements
Cross-tenant negative integration tests for every module, job-context tests and
concurrent write tests on PostgreSQL.

## Documentation requirements
ADR, tenancy guide, permission model and migration rollout runbook.

## Acceptance criteria
- [ ] Scope semantics and global records are owner-approved.
- [ ] Every tenant-bearing repository/read/write path is inventoried.
- [ ] Missing/foreign scope fails closed for requests and jobs.
- [ ] PostgreSQL integration tests prove isolation.
- [ ] Temporary defaults are removed through new migrations.

## Validation commands
`mvn clean verify`; fresh and upgrade Flyway tests; targeted cross-tenant API tests.

## Risks and rollback considerations
Removing defaults before all writers migrate can break production writes; use
expand/enforce/contract phases.

## Relevant files
`common/tenancy`, `licensing`, `security`, V16–V17, V22 and V26.

## Notes for the next developer or AI agent
Do not edit V17. Verify every query; entity columns alone are not isolation.
