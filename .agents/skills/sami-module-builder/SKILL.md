---
name: sami-module-builder
description: Implement, complete, scaffold, or continue production-ready SAMI ERP business modules from structured requirements. Use when work may span the SAMI Java/Spring backend and Vue/TypeScript frontend and must preserve existing module, database, security, event, reporting, localization, and lifecycle conventions.
---

# SAMI Module Builder

Implement requested SAMI ERP modules by extending the repository's current architecture. Treat repository evidence as authoritative; treat the stack below as the expected baseline and report discrepancies before coding.

## Expected stack

- Backend: Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA, PostgreSQL, Flyway, Maven, Jakarta Validation, OpenAPI.
- Frontend: Vue 3 Composition API, TypeScript, Vite, Vuetify, Pinia, Vue Router, Axios, VeeValidate, Zod, VueUse, Persian and English localization, RTL.

## Entry gate

1. Locate the repository root and read every applicable `AGENTS.md`, from root to each affected file.
2. Discover nested backend and frontend repositories. Do not assume both exist.
3. Check `git status` and the active branch in every affected Git worktree.
4. Require the active branch to be exactly `development` before modifying files. If it is not, stop before edits and report the repository and branch. Never switch branches without an explicit request.
5. Preserve unrelated user changes. Do not overwrite or clean them.
6. Read the complete business specification. Separate required work from future foundations and explicitly excluded work.

## Discovery before design

Inspect both application sides when available. Search before creating:

- modules, packages, controllers, public services, application services, entities, value objects, DTOs, mappers, validators, repositories, specifications, migrations, tests;
- security filters, permission evaluators, RBAC seeds, audit facilities, event publishers/listeners, schedulers, file services, reporting/export services, exception and API-envelope conventions;
- routes, API clients, types, schemas, stores, composables, directives, layouts, reusable components, tables, forms, dialogs, translations, RTL styles and tests;
- module lifecycle metadata, feature flags, documentation, Docker and startup configuration.

Use focused code searches and inspect at least one comparable module end to end. Before editing, state:

- affected repositories and module boundaries;
- comparable patterns found and exact patterns to reuse;
- required entities, relationships, APIs, permissions, events, reports, imports/exports and integrations;
- configurable concepts and their persistence mechanism;
- risks, edge cases, extension points and unresolved evidence.

If a required side of the project is absent or its architecture cannot be established, do not invent it. Implement only independently valid requested work and report the blocker.

## Design rules

- Keep business ownership in the module that owns the concept. Consume another module through its existing public service, query interface or event.
- Never duplicate shared security, audit, file storage, reporting, notification, workflow, lookup, configuration or event infrastructure.
- Avoid direct module-to-module persistence access and circular dependencies.
- Keep controllers thin and use existing transaction boundaries, API envelopes, pagination, errors, validation and optimistic-locking conventions.
- Reuse centralized authentication and authorization. Enforce permissions server-side; UI checks are only presentation controls.
- Keep statuses, types, providers, rules, categories, feature flags, workflows and templates database- or configuration-driven in the established way.
- Store file metadata and references in business modules; use the centralized file service for physical content.
- Minimize sensitive fields in DTOs, logs, audits, events and exports.
- Preserve backward compatibility unless the specification explicitly requires a breaking change.
- Never delete functionality or refactor unrelated modules to simplify delivery.

## Implementation sequence

1. Establish a traceable requirement checklist using [references/implementation-checklist.md](references/implementation-checklist.md).
2. Implement the smallest complete backend slice following existing package boundaries.
3. Add the next available Flyway migration:
   - inspect every migration location and naming convention;
   - determine the highest existing version in the affected migration stream;
   - create exactly the next version;
   - never rename or edit an existing migration;
   - make constraints, indexes, foreign keys, seed/config rows and rollback implications explicit.
4. Add server-side permissions and reuse existing RBAC registration/seed patterns.
5. Add audit and domain/integration events through existing mechanisms.
6. Expose public services or queries required by other modules without leaking repositories.
7. Implement requested import/export and reports through shared infrastructure.
8. Implement the frontend only when requested:
   - mirror established API/type/schema/form/table patterns;
   - add routes and menu entries through existing conventions;
   - add both Persian and English translations together;
   - verify RTL, loading, empty, validation, error, forbidden and responsive states.
9. Add unit, integration, repository, security, API and UI tests in the proportions used by comparable modules.
10. Update lifecycle metadata only after evidence supports the new status. Never mark a side complete when its build, tests or required scope remain unverified.

After each major slice, run the narrowest relevant tests before proceeding.

## Validation

Discover commands from repository files; do not invent command names. Run all available:

- backend formatting/static checks, compilation, unit and integration tests;
- migration validation and application-context startup;
- frontend type-check, tests and production build;
- Docker/compose configuration validation when present;
- a focused startup or smoke test covering frontend-to-backend routing when feasible.

Record exact commands, results and skipped checks. A failing check must remain visible in the final report. Do not weaken tests, validation, security or build configuration to obtain a pass.

## Completion standard

Claim completion only when every requested item is implemented and relevant builds/tests pass. Otherwise state the precise partial lifecycle status and remaining work.

Report:

- architecture findings;
- existing components and patterns reused;
- files created and modified;
- migration created;
- APIs and frontend pages implemented;
- events and permissions added;
- tests and verification commands with results;
- remaining limitations and manual steps;
- backend lifecycle status;
- frontend lifecycle status.
