---
name: sami-migration-guardian
description: Protect SAMI ERP PostgreSQL schema and Flyway migration integrity. Use whenever migrations, entities, tables, constraints, indexes, seed data, statuses, permissions, module registry entries, or schema lifecycle metadata are created, changed, merged, reviewed, or audited.
---

# SAMI Migration Guardian

Guard migration history and reconcile database intent with executable backend code. Read applicable `AGENTS.md` and use `$sami-project-context` findings when available.

## Establish migration history

1. Locate every backend Git root, Flyway configuration, migration location and profile.
2. Record branch/worktree state. Preserve unrelated changes.
3. Enumerate all versioned and repeatable migrations in deterministic version order.
4. Compare tracked history and relevant branches/tags when available to detect renamed, deleted or modified migrations.
5. Detect duplicate versions, version gaps, naming violations and checksum risks. Treat gaps as findings, not automatic errors, until repository conventions establish contiguity.
6. Never edit, rename, renumber or delete a migration that may have run in any environment.

For a new migration, inspect all uncommitted and branch-visible migrations, then choose the next sequential version according to the established convention. Never reuse a version.

## Reconcile schema and code

Compare proposed/current SQL with entities, embeddables, converters, repositories, specifications, DTO assumptions and tests. Verify:

- table, sequence, column and constraint names;
- PostgreSQL types, lengths, precision and time-zone behavior;
- nullability, defaults and generated values;
- foreign-key ownership, actions and lifecycle compatibility;
- unique, check and exclusion constraints;
- indexes against actual filter, join, order and uniqueness patterns;
- tenant/company/branch scope in keys, uniqueness and queries;
- optimistic locking, soft deletion and audit fields;
- entity mappings, enum/storage choices and repository queries.

Protect invariants at database level when concurrent requests could bypass application checks. Explain locking and constraint behavior.

## Seeds and lifecycle

- Make seed operations repeatable/idempotent using the project's established pattern.
- Preserve stable codes and identifiers.
- Check statuses, permissions, roles and module registry rows against all prior seeds.
- Avoid duplicate rows and destructive conflict handling.
- Separate `SCHEMA_ONLY` from executable backend implementation.
- Never mark a module `BACKEND_READY` merely because tables or seed rows exist.

## Safe change rules

- Never silently drop tables, columns, constraints, indexes or data.
- Document compatibility, locking and data-loss impact before weakening a constraint.
- Prefer additive, backward-compatible expansion for rolling deployments.
- Plan data backfills and later enforcement separately when existing rows may violate a new invariant.
- Do not claim rollback safety: describe practical rollback/forward-fix considerations, irreversible transformations and application-version coupling.

## Verification

Use repository-defined commands and profiles. When possible run Flyway validation, migration tests, repository/integration tests, `mvn clean verify`, and application startup against an isolated database. Never apply migrations to an unidentified or shared database.

Report unavailable checks and do not claim validation from SQL inspection alone.

## Final report

- migration created or audited;
- selected/current version and migration range;
- schema and seed changes;
- constraints/index rationale and concurrency protection;
- compatibility and deployment impact;
- rollback/forward-fix considerations;
- Flyway/build/test results;
- entity/repository/schema mismatches;
- module lifecycle status and unverified areas.
