---
name: sami-backend-builder
description: Implement and review SAMI ERP backend code using the repository's existing Java 21 and Spring Boot architecture. Use for backend module work, REST APIs, entities, repositories, migrations, services, validation, security, audit, events, reports, imports/exports, performance, concurrency, and backend tests.
---

# SAMI Backend Builder

Implement or review backend work by extending verified repository conventions. Treat Java 21 as the expected runtime, but report any conflict with build files or toolchains rather than silently changing versions.

## Preconditions

1. Locate the backend Git root and read every applicable `AGENTS.md`.
2. Inspect the active branch, worktree status, build descriptor, Java configuration and module layout.
3. Preserve unrelated changes and generated files owned by the user.
4. Read the complete requirement and distinguish implementation requests from review-only requests. A review request does not authorize edits.
5. If backend source or required shared infrastructure is absent, do not invent an architecture. Report the missing evidence.

## Pattern discovery

Before designing, inspect at least one comparable backend module end to end. Find and cite the existing:

- `BaseEntity` hierarchy and identity, timestamp, actor, tenant and optimistic-locking behavior;
- entity, DTO, mapper and Jakarta Validation conventions;
- repository, specification/filter, pagination and sorting patterns;
- application/domain service boundaries and transaction conventions;
- controller, API envelope, OpenAPI, exception and error-code conventions;
- permission checks, Spring Security integration and tenant/company/branch context;
- audit infrastructure and sensitive-data redaction;
- domain/integration event publishing and consumption;
- import/export, report, file and asynchronous job infrastructure;
- test fixtures, containers, security tests and integration-test conventions.

State which exact patterns will be reused before coding. Search for existing ownership before adding shared concepts.

## Design constraints

- Follow current package, naming, dependency and layering conventions.
- Keep controllers limited to transport concerns, validation, authorization entry points and response mapping.
- Keep business decisions in application/domain services.
- Use DTOs at every external API boundary. Never serialize persistence entities directly.
- Apply Jakarta Validation to request shapes and domain validation to state-dependent invariants.
- Use transactions deliberately:
  - define write boundaries in services;
  - keep remote calls outside database transactions unless the established outbox/process pattern requires otherwise;
  - mark read-only operations consistently when the project does.
- Preserve backward-compatible endpoints, payloads, events and database behavior unless a breaking change is explicitly requested.
- Avoid module repository access from other modules. Use existing public services, queries or events.

## Data integrity and performance

- Model invariants in both application logic and database constraints when concurrent requests could bypass application checks.
- Use unique, check, not-null and foreign-key constraints consistent with lifecycle and soft-delete behavior.
- Add indexes only for demonstrated repository/specification queries, joins, ordering or uniqueness requirements. Explain column order and selectivity.
- Use optimistic or pessimistic locking only when supported by an identified race.
- Handle retries, duplicate commands and event delivery idempotently where required.
- Avoid unbounded collections, N+1 loading and accidental cascade operations.

## SAMI cross-cutting rules

- Apply the current tenant and company/branch scope architecture to reads, writes, uniqueness, events, audit and exports.
- Enforce permissions server-side for every sensitive operation.
- Record sensitive operations through shared audit infrastructure with actor, scope, time and meaningful change data.
- Publish relevant module events through shared event infrastructure and respect established transaction timing.
- Never call external providers directly from business modules. Depend on existing integration ports/services.
- Do not create module-local file, calendar, task, communication, template, automation or validation engines.
- Do not log or expose secrets or unnecessary personal, financial or security data.

## Flyway

1. Locate every configured migration path and inspect the naming/version convention.
2. Determine the highest migration version from repository evidence.
3. Create the next sequential migration; account for parallel/uncommitted migrations.
4. Never edit, rename or delete an existing applied migration.
5. Keep schema, entity, repository and DTO changes synchronized.
6. Include constraints, indexes, reference/configuration data and compatibility handling required by the feature.
7. Use established repeatable/data migration patterns when applicable; do not invent rollback conventions.

## Implementation and review

For implementation:

1. Build a requirement trace using [references/backend-checklist.md](references/backend-checklist.md).
2. Implement persistence and domain invariants.
3. Implement services, security, audit and events.
4. Implement DTO mapping, validation, controllers and OpenAPI.
5. Implement reports/imports/exports through shared infrastructure.
6. Add unit tests for rules and edge cases.
7. Add repository/integration/API/security tests for persistence, scoping, transactions, permissions and contracts.

For review:

- remain read-only;
- prioritize correctness, security, data integrity, concurrency and compatibility defects;
- cite exact paths and tight line ranges;
- distinguish verified defects from risks and missing evidence.

## Verification

Discover the Maven wrapper and project profiles from the repository. When the environment allows, run:

```text
mvn clean verify
application startup
Flyway validation
```

Prefer `./mvnw` or `mvnw.cmd` when present. Use the repository's actual startup and Flyway commands/profiles rather than inventing flags. Also run focused tests during implementation and any configured formatting, static analysis, architecture or OpenAPI checks.

Record commands and outcomes. Do not claim success for unavailable, skipped or failing checks. Do not disable validation or tests to make a build pass.

## Final report

Report:

- design decisions and reused patterns;
- entities and tables;
- API list;
- validation, database constraints and indexes;
- security, tenant scope and audit integration;
- events and integration boundaries;
- files and Flyway migration changed;
- tests and verification results;
- remaining risks, missing evidence and manual steps.
