---
name: sami-release-gate
description: Run evidence-based SAMI ERP pre-release, pre-merge, checkpoint, client-review, push, stability, and deployment readiness validation. Use when asked whether the project, branch, module, or revision is ready, stable, safe to merge/push, or ready for review or deployment.
---

# SAMI Release Gate

Validate without implementing unrelated fixes. A skipped or unavailable check is never a pass.

## Establish release subject

1. Read applicable `AGENTS.md`; detect frontend/backend roots.
2. Record branch, exact commit, upstream, tags and worktree status.
3. Identify expected source/integration branches and verify containment using merge bases and unique commits.
4. Establish repository-detected technology versions; do not trust remembered versions.

## Backend gate

When present, verify:

- configured Java requirement (expected SAMI baseline: Java 21) and Maven availability;
- `mvn clean verify` or wrapper equivalent;
- Spring application-context/startup check;
- Flyway validation and migration history;
- schema/entity validation against an isolated configured database;
- duplicate endpoint and entity/table mappings;
- security, tenant scope and configured integration tests.

## Frontend gate

- install dependencies only when needed, using the existing lockfile/package manager;
- run configured type-check (`vue-tsc --noEmit`), tests, lint and production build;
- validate routes, backend-driven menus and permission guards;
- compare English/Persian keys and RTL paths;
- run a browser smoke test and inspect console/network errors when available.

## Integration gate

Validate API contracts, environment variables, proxy/CORS configuration (including port `7474` only when project/current requested configuration uses it), authentication, permissions and lifecycle metadata.

Generate a reproducible module matrix. Never infer implementation from schema, menu, route, placeholder or metadata. Do not label `SCHEMA_ONLY` as `BACKEND_READY`; flag every falsely complete module.

## Verdict

- `PASS`: all required checks ran and passed; no blocking gaps.
- `PASS WITH RISKS`: all release-critical checks passed, with explicitly accepted non-blocking risks.
- `FAIL`: any blocker, required failed check, dirty/incorrect release state, incompatible contract, or unavailable release-critical verification.

## Output

- verdict and scope;
- exact frontend/backend commit hashes and worktree state;
- migration range/status;
- test counts when tools report them;
- frontend build/type/lint/test/runtime results;
- backend build/startup/Flyway/schema results;
- module status matrix;
- blocking issues and non-blocking risks;
- exact reproducible commands;
- unavailable checks and why.
