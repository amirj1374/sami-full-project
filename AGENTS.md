# SAMI workspace guidance

Before architecture, implementation, dependency, migration, environment, or release work:

1. Read [`.agents/SAMI_PROJECT_CONTEXT.md`](.agents/SAMI_PROJECT_CONTEXT.md).
2. Treat repository files as authoritative when they differ from the context snapshot.
3. Use the relevant repository-scoped skill under [`.agents/skills`](.agents/skills).
4. Refresh the context document when a repository, runtime requirement, dependency, build command, environment contract, or shared architectural pattern changes.

## Repository safety

- The workspace root is the shared full-project Git repository.
- `sami-frontend` is an independent Git repository registered as a submodule.
- `sami-backend` is currently incomplete and must not be reconstructed from frontend assumptions.
- Preserve unrelated worktree changes.
- Do not modify `main` unless explicitly instructed.
- Use `development` as the normal integration branch.

## Evidence rules

- Never infer completed functionality from a route, menu item, migration, type, package, placeholder, or lifecycle label alone.
- Never invent an unavailable backend contract, database schema, framework version, or environment variable.
- Report checks that could not run.
- Keep Persian and English behavior, RTL/LTR, permissions, audit, tenant scope, API contracts, and migrations synchronized when applicable.
