---
name: sami-architecture-auditor
description: Perform evidence-based, analysis-only SAMI ERP architecture audits before implementation, refactoring, integration, migration, or major module work. Use when asked to inspect, review, analyze, compare, verify, map, reconcile, or assess architecture, modules, frontend/backend alignment, data ownership, dependencies, security, events, migrations, infrastructure, lifecycle status, risks, or gaps.
---

# SAMI Architecture Auditor

Audit the repository as it exists. Produce implementation guidance, not code or mutations.

## Non-mutation boundary

- Default to read-only operation.
- Do not edit, create, delete, format or generate repository files.
- Do not install dependencies, apply migrations, start stateful services or change external systems.
- Use read-only discovery, compilation or tests only when they do not mutate tracked sources or shared state.
- Do not propose a new architecture until repository evidence proves the existing architecture cannot satisfy a stated requirement.
- Modify files only after the user explicitly changes the task from analysis to implementation. Apply the appropriate implementation skill and re-check authorization and branch conditions first.

## Discovery

1. Locate repository and nested worktree roots.
2. Read all applicable `AGENTS.md` and project documentation.
3. Record active branches, worktree status and relevant uncommitted changes without altering them.
4. Inventory frontend, backend, shared packages, build tools, versions, database, migrations, configuration, containers, deployment files, tests and documentation.
5. Distinguish observed facts from inferences and unknowns.

Inspect comparable modules end to end:

- module and package boundaries and dependency direction;
- entities, tables, relationships, repositories, DTOs, validation, transactions and migrations;
- services, controllers, API endpoints, pagination, filtering, errors and OpenAPI;
- authentication, authorization, permissions, tenant/company/branch scoping and sensitive-data handling;
- audit, events, public services, integrations, workflow, notifications, files, reports and import/export;
- frontend routes, stores, API clients, types, forms, state, permissions, localization, RTL and reusable UI;
- tests, lifecycle metadata, observability, startup and deployment.

Search for existing ownership before recommending any new capability.

Build a traceable inventory that maps, when present:

- module → packages → services/controllers → endpoints;
- entity → table → migration → repository;
- frontend route → page → store/composable → API client → backend endpoint;
- permission → backend enforcement → frontend presentation;
- event → publisher → consumer;
- module → tests → lifecycle metadata.

Never treat a migration, table, menu item, package name, route, DTO, placeholder, comment or lifecycle declaration alone as proof of an implemented feature. Verify executable paths and tests.

## Analysis

For the proposed work, map:

- bounded context and business owner;
- affected and consuming modules;
- entities, cardinalities, lifecycle and invariants;
- configurable rules and lookup ownership;
- public service/query contracts and events;
- permissions, audit records and privacy concerns;
- reporting, import/export and file-storage needs;
- edge cases, concurrency, idempotency and failure handling;
- migration and backward-compatibility risks;
- extension points and expected integrations;
- frontend/backend scope split.

Identify exact reusable patterns with file references. Flag duplication, inappropriate coupling, security bypasses, hardcoded configuration and cross-module data access.

Use [references/audit-checklist.md](references/audit-checklist.md) to ensure coverage. Omit sections that are genuinely irrelevant, but say why.

## Status classification

Assign each module or capability exactly one evidence-backed status:

- `implemented`: executable end-to-end path exists for the claimed scope and relevant verification passes;
- `partially implemented`: meaningful implementation exists but required paths, integrations or verification are incomplete;
- `schema only`: migrations/tables exist without a verified application implementation;
- `backend only`: verified server implementation lacks the required frontend;
- `frontend only`: UI/client contracts exist without a verified server implementation;
- `planned`: only placeholders, metadata, documentation or declarations exist.

Use `unverified` separately when access or tooling prevents classification. Do not upgrade status based on naming or intent.

## Gap and dependency checks

Search specifically for:

- duplicate implementations or competing owners;
- direct module coupling, persistence leakage and circular dependencies;
- missing shared or deployment infrastructure;
- schema/entity/repository/DTO mismatches;
- frontend route/type/client versus backend endpoint mismatches;
- hardcoded statuses, providers, workflows, rules, categories or feature flags;
- authentication, authorization, scope or sensitive-data policy bypasses;
- missing audit coverage or incomplete before/after attribution;
- inaccurate lifecycle metadata and partially implemented modules;
- orphan tables, endpoints, events, routes, permissions and tests.

Construct a dependency graph from observed imports, build dependencies, service calls, repository access, events and API consumption. Identify highest fan-in blockers: incomplete or risky components that block the greatest number of modules or flows. Show how fan-in was determined and avoid numeric claims when the inventory is incomplete.

## Recommendations

Rank findings by impact:

- Critical: security, data loss, corrupt migrations or deploy blockers.
- High: architectural violation or likely production failure.
- Medium: maintainability, correctness or operability risk.
- Low: improvement with limited immediate risk.

For each finding provide evidence, consequence and the smallest architecture-compatible recommendation. Do not present guesses as findings.

Use exact repository-relative file paths and tight line ranges whenever the available tools provide reliable line numbers. For generated artifacts or database/runtime observations, cite the command or query instead. Clearly label inferred relationships.

Produce this report:

1. Executive summary.
2. Current architecture map.
3. Module status matrix using the required status taxonomy.
4. Dependency graph, with observed versus inferred edges distinguished.
5. Highest fan-in blockers.
6. Cross-cutting gaps.
7. Technical debt.
8. Security risks.
9. Data integrity risks.
10. Recommended implementation order based on dependencies and risk.
11. Exact evidence for every material finding.
12. Confidence and unverified areas, including checks that could not run and why.

State explicitly that no files were changed.
