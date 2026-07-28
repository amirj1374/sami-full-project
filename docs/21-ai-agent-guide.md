# AI coding agent guide

This guide is the complete operational workflow for AI-assisted development in
SAMI. [`AGENTS.md`](../AGENTS.md) is the concise mandatory policy.

## Instruction hierarchy

Apply instructions in this order:

1. `AGENTS.md`
2. this guide
3. the applicable repository-scoped skill under `.agents/skills`
4. task-specific user instructions

Repository-scoped skills specialize the global workflow; they do not replace
it. Task instructions may narrow scope but must not silently weaken safety,
consistency, approval or scope rules. Current source and configuration are the
implementation source of truth. Documentation supplies architecture and
business context; Git history supplies supporting evidence of intent.

## Mandatory reading and evidence

Before architecture, implementation, dependency, migration, environment or
release work, inspect:

1. `AGENTS.md` and `.agents/SAMI_PROJECT_CONTEXT.md`
2. this guide and the relevant repository-scoped skill
3. current branch, worktree and relevant Git history
4. owning module, workflow, risk, backlog and ADR documentation
5. current source, configuration, migrations and tests

Never infer implemented behavior from documentation, a migration, route, menu,
type, package, placeholder, backlog item or lifecycle label alone.

## Mandatory read-only preparation gate

Before modifying any file:

1. Re-check the current branch and worktree.
2. Read documentation related to the target module.
3. Inspect Git history for the exact files and module involved.
4. Inspect the backend flow end to end: controller, service, DTO, entity,
   repository, migration, security, audit and events.
5. Inspect the frontend flow end to end: route, view, API client, TypeScript
   types, schema, permissions, translations, stores and shared components.
6. Identify affected modules, dependencies and reusable implementations.
7. Assess database, tenancy, security, permission, audit, API, frontend,
   localization, testing, deployment and operational impacts.
8. Identify unresolved business or architectural decisions.
9. Prepare the smallest implementation plan.

Report the current understanding, inspected evidence, reuse plan, likely files,
impact analysis, risks, unresolved decisions and minimal plan. Do not modify
source or documentation during this preparation phase. Wait for explicit
approval unless the user clearly authorized immediate implementation in the
same request.

Do not begin cross-cutting architecture work without explicit approval.

## Reuse-first repository search

Before creating anything, search in this order:

1. existing backend feature and module implementations;
2. existing frontend views;
3. public services;
4. shared infrastructure;
5. common utilities and helpers;
6. shared DTOs, types and schemas;
7. shared entities and repositories;
8. existing migrations;
9. existing services and API clients;
10. existing stores and composables;
11. existing UI components, styles, tokens, themes and layouts;
12. existing routes, menus, permissions and authorization patterns;
13. existing translation keys in every supported locale;
14. existing tests, fixtures, factories and test utilities;
15. existing scripts, Docker/deployment patterns and operational documentation;
16. Git history for similar implementations.

Reuse or extend an equivalent implementation. Refactor only when necessary for
the approved requirement. Never create a second owner for an existing concept.
Treat duplication as a design smell requiring explicit justification.

## Justification for new artifacts

Before adding a module, package, component, composable, store, service,
controller, repository, DTO, entity, mapper, validator, exception, abstraction,
interface, utility, configuration, environment variable, permission, route,
menu entry, event, scheduler, table, migration, Docker service, script or
dependency, explain:

- why an existing artifact cannot be reused or extended;
- why the new artifact is required;
- why it belongs in the chosen location;
- how it follows repository architecture and conventions;
- what maintenance cost it introduces.

Do not create abstractions for hypothetical future use.

## Evolving business requirements

Assume business requirements will evolve. Keep approved behavior inexpensive to
extend by separating changeable business policy from technical infrastructure,
using existing configuration mechanisms where change is reasonably expected,
and preserving stable public contracts and persisted data when practical.

Prefer extension over replacement and document assumptions that may need future
review. When several solutions are correct, choose the simplest design that
offers useful flexibility with the lowest complexity and maintenance cost.

This does not authorize unapproved features, speculative modules, generalized
frameworks or abstractions for hypothetical scenarios. Implement today's
approved requirement completely and no more.

Default to progress and use engineering judgment for ordinary implementation
details, internal design, code organization, reusable components and service or
repository decomposition. Request approval only when an unresolved decision
would materially change security, authentication, authorization, public APIs,
persisted data contracts, irreversible database migrations, externally
observable behavior or long-term architecture.

## Smallest correct change and scope control

Implement the smallest complete, backward-compatible change that satisfies the
approved requirement. Do not perform unrelated refactoring, modernization,
renaming, formatting, dependency upgrades, framework changes, restructuring,
redesign, cleanup or optimization.

Classify proposed work outside the requested scope as:

- **Critical defect**
- **Security issue**
- **Data integrity issue**
- **Build failure**
- **Performance issue**
- **Maintainability suggestion**
- **Style preference**

Only a demonstrated critical defect, security issue, data-integrity issue or
build failure may justify expanding implementation scope. Report performance,
maintainability and style observations separately and leave them unchanged
without approval.

For task planning, also distinguish work necessary to make the approved change
work, work necessary for correctness/security, optional improvements and
unrelated work. Only the first two belong in the implementation, and should be
reported before editing whenever possible.

## Repository consistency acceptance criterion

A change is incomplete unless it matches existing:

- module boundaries, architecture and dependency direction;
- naming, packages, folders and formatting;
- controller/DTO/service/repository responsibilities;
- DTO, mapping, API response and error conventions;
- logging, validation and transaction conventions;
- authentication, authorization and permission rules;
- tenant/company/branch scoping and audit behavior;
- Flyway and data-integrity strategy;
- frontend composition, state and API-client patterns;
- localization, RTL/LTR, responsive and UI-kit behavior;
- loading, empty, validation and error states;
- tests, Docker/deployment and documentation structure.

When two implementations work, choose the one most consistent with the
repository. Explain any proposed break from consistency and wait for approval.
Do not propagate transitional patterns such as `TenantDefaults` into new code.

## Cross-cutting impact checklist

Explicitly determine whether a task affects:

- schema, migrations, constraints, indexes or data backfill;
- tenant, company, branch, store or user isolation;
- authentication, permissions, roles, menus, buttons or route guards;
- audit logs, events, idempotency or concurrency;
- endpoints, DTOs, validation, errors and TypeScript contracts;
- routes, stores, API clients, types, schemas and reusable UI;
- English/Persian localization, RTL/LTR and responsive behavior;
- loading, empty, validation and failure states;
- tests, fixtures and mocks;
- Docker, deployment, environment variables, backup/restore;
- observability, logging and operational behavior.

Never invent or propagate hardcoded tenant, company, branch, user, permission
or environment defaults.

## Domain-specific safety

- Never edit an applied Flyway migration; create the next unique forward
  migration and verify fresh and upgrade paths on PostgreSQL.
- Keep Hibernate schema generation disabled; Flyway owns the schema.
- Apply RBAC server-side; frontend checks are presentation only.
- Resolve scope from trusted server context and scope reads, writes and
  uniqueness.
- Keep backend DTOs, TypeScript types, API clients, schemas and mocks aligned.
- Update `en.json` and `fa.json` together and verify RTL/LTR.
- Preserve transaction, idempotency, concurrency and event timing.
- Never expose secrets or access production/shared data without authorization.
- Report every check that could not run and why.

## Specialized change checklists

For a new module, define its owner, dependencies, terminology, entities,
invariants, lifecycle, scope, API, errors, permissions, audit, events,
migration, frontend contracts, localization, tests and documentation.

For a workflow change, map actors, triggers, preconditions, transitions,
rejections, retries and partial failures. Verify scope, permissions, audit,
transactions, concurrency, idempotency and event timing.

For a database change, inspect the entire migration chain and current mappings;
define constraints, indexes, backfill and rollback limitations; verify fresh
migration and upgrade behavior and deployment compatibility.

## Implementation report

After implementation, report:

- every modified file and its purpose;
- reused implementations and patterns;
- every new artifact and its justification;
- commands executed and validation/test results;
- checks not run and why;
- unresolved issues and separately classified suggestions;
- final Git status;
- whether anything was staged, committed or pushed.

## Version-controlled AI knowledge

Permanent workflows, conventions, safety rules, architectural decisions and
reusable implementation patterns are project knowledge. Store them in the
smallest appropriate repository document, avoid duplication, and update a
specialized skill only when its specialized workflow changes.

Record meaningful policy changes in
[`22-ai-development-history.md`](22-ai-development-history.md). Prepare a
dedicated knowledge commit when requested, but never commit or push without
explicit approval.

Operational setup and deployment commands belong in
[`PROJECT_SETUP_AND_DEPLOYMENT.md`](../PROJECT_SETUP_AND_DEPLOYMENT.md).
