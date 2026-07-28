---
id: HIGH-001
title: Add PostgreSQL and Flyway integration test gate
status: blocked
priority: high
type: test
area: database
owners: []
depends_on: []
blocks: [HIGH-004, HIGH-006, HIGH-008, HIGH-009]
estimated_size: M
risk: high
source_refs: [sami-backend/pom.xml, sami-backend/src/main/resources/db/migration, sami-backend/src/test/java, .github/workflows/ci.yml]
---

# Add PostgreSQL and Flyway integration test gate

## Summary
Test fresh migrations, entity validation, repositories and representative
upgrade behavior on PostgreSQL 16 in CI.

## Why this is needed
Pure unit tests cannot detect PostgreSQL SQL, constraint or JPA/schema drift.

## Current evidence
No Testcontainers dependency or Spring database integration test exists; CI
runs Maven verification only.

## Existing implementation
Flyway V1–V27, `ddl-auto: validate`, duplicate-version CI guard and unit tests.

## Missing work and scope
Add isolated PostgreSQL test infrastructure, fresh-schema validation and
representative repository/constraint tests. Do not access shared databases.

## Dependencies
None; CI must support Docker/Testcontainers.

## Business decisions required
None beyond CI resource/timeout ownership.

## Proposed implementation approach
Reusable PostgreSQL 16 container fixture; boot application with Flyway; assert
latest schema and critical constraints; keep test data deterministic.

## Impacts
Backend tests, test dependencies and CI only; no production API/frontend
behavior. No existing migration may be edited.

## Security and tenant-isolation considerations
Use ephemeral credentials and include tenant uniqueness/isolation cases.

## Testing requirements
The task itself creates the integration tests; prove both clean bootstrap and
at least one supported upgrade fixture.

## Documentation requirements
Document local/CI commands and failure triage.

## Acceptance criteria
- [ ] PostgreSQL 16 container applies V1–V27 from empty state.
- [ ] Hibernate validation passes.
- [ ] Critical constraints and tenant keys are exercised.
- [ ] CI runs the suite without shared state.
- [ ] Migration failure output identifies the version safely.

## Validation commands
`mvn clean verify`; CI backend job.

## Risks and rollback considerations
Container tests add CI time; isolate slow fixtures and avoid weakening coverage.

## Relevant files
`pom.xml`, migrations, backend tests, `.github/workflows/ci.yml`.

## Notes for the next developer or AI agent
Do not use the locally running PostgreSQL service or Flyway repair.

Blocked on 2026-07-28 because the execution environment has Java 17 while the
project requires Java 21, and has neither Maven/a project wrapper nor Docker.
Unblock only when Java 21, Maven (or a valid wrapper), Docker with a working
daemon, and permission to run isolated PostgreSQL/Testcontainers are available.
