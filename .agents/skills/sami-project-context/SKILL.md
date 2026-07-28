---
name: sami-project-context
description: Establish authoritative, reusable technical context for SAMI ERP by inspecting the actual repositories without modifying them. Use before architecture decisions, dependency additions, framework upgrades, environment setup, module implementation, refactoring, or other SAMI skills when exact stack versions, repository structure, conventions, shared infrastructure, build commands, compatibility, or frontend/backend alignment matter.
---

# SAMI Project Context

Establish technical context from current repository evidence. Ignore remembered versions and treat user-provided versions as expectations until repository files verify them.

## Operating boundary

- Remain read-only unless the user explicitly changes the task.
- Do not create, edit, delete, format or generate repository files.
- Do not install, upgrade or remove dependencies.
- Do not apply migrations or change services, containers or external systems.
- Prefer static inspection and read-only commands. Run builds/tests only when requested or necessary and safe; disclose possible generated artifacts first.
- Separate verified facts, derived facts, recommendations and unknowns.

## Repository discovery

1. Locate the workspace root, nested Git roots and shared directories.
2. Record each repository's active branch and working-tree status.
3. Read every applicable `AGENTS.md`, `CLAUDE.md`, README and directly relevant document.
4. Inspect CI, editor, container, deployment and environment configuration.
5. Report absent, incomplete or inaccessible repositories immediately. Do not reconstruct their technology from another repository.

## Backend source of truth

Inspect when present:

- `pom.xml`, parent/dependency management, Maven wrapper, profiles and toolchains;
- `application.yml`, `application-*.yml`, test configuration and environment placeholders;
- Flyway configuration and migrations;
- Dockerfiles and compose/orchestration files;
- Java packages, module descriptors and Spring configuration;
- security filters/configuration, authentication providers and permission evaluation;
- tests, fixtures, containers, build plugins and generated API configuration.

Determine from direct or dependency-managed evidence:

- Java, Maven, Spring Boot, Spring Framework, PostgreSQL, Flyway, Hibernate and Spring Security versions;
- API documentation and testing libraries;
- database/container setup;
- authentication method and authorization model.

Do not confuse transitive dependency versions with explicitly supported application versions. If the effective Maven model is required but cannot be generated read-only, report the unresolved source.

## Frontend source of truth

Inspect when present:

- `package.json`, lockfiles, package-manager metadata and Node version declarations;
- Vite and TypeScript configuration;
- `.env.example` and environment typing/usage;
- Vue plugins, Vuetify configuration, router, stores and API clients;
- localization, RTL, global styles, design tokens and shared components;
- test configuration, scripts, Dockerfiles, nginx/reverse proxy and CI.

Determine:

- Node requirements and package manager;
- exact locked Vue, TypeScript, Vite, Vuetify, Pinia, Vue Router and Axios versions;
- form/schema validation and testing libraries;
- UI-kit/shared component infrastructure.

Distinguish declared version ranges from locked versions and the currently installed runtime.

## Architecture inventory

Map verified patterns with representative file evidence:

- package structure, layering, domain/module boundaries and dependency direction;
- entity conventions and base hierarchy;
- DTO/mapping, repository/specification/filter, service and controller conventions;
- API response, exception and validation behavior;
- audit, event, tenant/company/branch context and permission enforcement;
- shared integration, file, report, import/export, notification and workflow infrastructure;
- frontend component, prop/emit, composable, store and API integration conventions;
- routing, menu, permissions, localization and RTL behavior.

Never infer a working pattern from a migration, package, route, menu entry, interface, placeholder or comment alone. Verify executable usage and tests where possible.

## Version and compatibility rules

1. Prefer explicit project configuration, then dependency management/lockfiles, then installed metadata.
2. Report both declared and resolved/locked versions when they differ.
3. Use repository-detected versions in downstream decisions.
4. Reuse existing dependencies before recommending a new library.
5. Do not recommend upgrades unless the user requests them.
6. Identify deprecated or incompatible usage without changing it.
7. Preserve existing architecture and distinguish recommendations from current facts.
8. Reconcile frontend clients/routes/types with backend endpoints/contracts when both repositories exist.
9. Report frontend/backend configuration disagreements and their deployment effect.
10. State `unverified` instead of inventing an exact version or capability.

## Evidence discipline

- Cite repository-relative paths and tight line ranges for material claims when reliable line numbers are available.
- Cite commands for runtime/tool versions and derived effective configuration.
- Never expose secret values. Report variable names, sources and whether values are required.
- Assign confidence as `high`, `medium` or `low` and explain non-high confidence.
- Include checks that could not run, the reason and the fact they leave unresolved.

## Output

Use [references/context-summary-template.md](references/context-summary-template.md) to produce a concise context suitable for other SAMI skills.

Include:

- repository roots, branches and completeness;
- technology stack with exact declared and locked versions;
- repository structure and architecture patterns;
- available shared infrastructure and reusable frontend/backend components;
- verified build, test, run, migration and container commands;
- required runtimes, services and environment variable names;
- known limitations, mismatches and compatibility risks;
- confidence and unverified areas.

End by confirming that no files or dependencies were changed.
