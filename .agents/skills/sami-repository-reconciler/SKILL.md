---
name: sami-repository-reconciler
description: Safely audit, compare, reconcile, merge, recover, and verify SAMI ERP Git branches without losing modules, migrations, frontend work, configuration, tests, or history. Use for branch comparison, work recovery, repository reconciliation, unified/integration branches, merge verification, or lost-module checks.
---

# SAMI Repository Reconciler

Default to analysis first. Never merge until containment, history and conflict analysis are complete.

## Safety boundary

- Detect separate frontend/backend Git roots and read applicable `AGENTS.md`.
- The default working branch is `development`; never modify `main` without explicit instruction.
- Preserve dirty worktrees. Do not stash, clean, reset or switch branches without authorization.
- Fetch/prune only when network access and the user's requested reconciliation permit remote updates; fetching changes Git metadata, so state the action.
- Never force-push.
- Never delete a branch without explicit instruction after proving full containment.
- Push only when explicitly requested.

## Inventory and compare

For every relevant repository:

1. List local/remote branches, upstreams, active branch, HEAD and worktree status.
2. Calculate merge bases, ahead/behind counts, unique commits, patch equivalence with `git cherry`, and changed files.
3. Inspect commit content rather than relying only on messages.
4. Map each branch's modules, migrations, entities, services, controllers, views, routes, API clients, tests and configuration.
5. Detect deleted/renamed work, migration-version collisions, schema overlap, competing implementations and config drift.
6. Determine containment separately for commits and effective file content.

Produce an analysis and proposed merge order before mutation.

## Reconciliation

Only after authorization:

- create/update the requested integration or `development` branch;
- merge/cherry-pick in dependency-aware order using normal Git history;
- inspect every conflict semantically; never accept blanket `ours`/`theirs`;
- preserve the more complete compatible implementation, combining non-overlapping behavior;
- resolve migration collisions with new forward migrations when applied history may exist—never renumber historical migrations;
- preserve tests, localization, configuration and lifecycle metadata alongside code.

Stop when a conflict requires an unprovided business decision.

## Verification

After reconciliation, re-run commit/content containment checks. Verify frontend and backend together using configured builds, tests, migration validation and startup/smoke checks. Confirm final upstream/tracking state without pushing unless asked.

## Final report

- repository and branch inventory;
- merge bases, ahead/behind and unique/missing commits;
- branches merged and commits incorporated;
- conflicts and semantic resolutions;
- modules/configuration/tests preserved;
- migration collision/status;
- build/test/startup results;
- final branch, commit and worktree state;
- push/tracking state;
- remaining risks and unverified containment.
