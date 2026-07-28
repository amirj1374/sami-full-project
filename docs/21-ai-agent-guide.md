# AI coding agent guide

`AGENTS.md` is authoritative. Repository source/configuration overrides
`.agents/SAMI_PROJECT_CONTEXT.md`, which overrides this explanatory guide when
facts conflict.

## Mandatory reading order

1. `AGENTS.md`
2. `.agents/SAMI_PROJECT_CONTEXT.md`
3. relevant `.agents/skills/*/SKILL.md`
4. `docs/README.md`
5. owning module, workflow, risk and ADR documents
6. current source, migrations, tests and Git diff

## Safe operating rules

- Inspect `git status` before and after; preserve user changes.
- Prefer `rg`/read-only commands for discovery.
- Never edit an applied Flyway migration.
- Never invent endpoints, tables, permissions, tenant rules or business states.
- Do not infer completion from routes, migrations, types, menus or comments.
- Never expose secrets or access production/shared data.
- Keep backend and TypeScript contracts synchronized.
- Apply RBAC server-side; UI checks are presentation only.
- Resolve tenant scope from trusted context.
- Update `en.json` and `fa.json` together and check RTL/LTR.
- Report commands not run and why.

## New-module checklist

- owner, dependencies, status and ubiquitous language defined;
- entities/invariants/lifecycles and tenant scope defined;
- API, errors, permissions, audit and events defined;
- next Flyway migration and indexes reviewed;
- backend/frontend contracts and localization aligned;
- unit, PostgreSQL, security, contract and UI tests added;
- module catalogue, workflow, risks and ADRs updated.

## Workflow-change checklist

- map trigger, actors, preconditions, transitions and failures;
- preserve transaction/idempotency/concurrency behavior;
- verify permissions, tenant scope, audit and event timing;
- update both clients and server contracts;
- test happy, rejection, retry and partial-failure paths.

## Database-change checklist

- inspect full migration chain and current entities;
- create the next unique migration only;
- use backward-compatible expansion where possible;
- define constraints/indexes/backfill and rollback limitations;
- test fresh migration and upgrade on PostgreSQL;
- ensure old/new application compatibility during rollout.

Final responses must list files changed, validations, unavailable checks,
remaining risks, final Git status, and confirmation of prohibited actions not
taken.
