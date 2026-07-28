# SAMI workspace guidance

Before architecture, implementation, dependency, migration, environment, or release work:

1. Read [`.agents/SAMI_PROJECT_CONTEXT.md`](.agents/SAMI_PROJECT_CONTEXT.md).
2. Read and follow [`docs/21-ai-agent-guide.md`](docs/21-ai-agent-guide.md).
3. Treat repository files as authoritative when they differ from documentation or the context snapshot.
4. Use the relevant repository-scoped skill under [`.agents/skills`](.agents/skills).
5. Refresh the context document when a repository, runtime requirement, dependency, build command, environment contract, or shared architectural pattern changes.

## Mandatory development workflow

- Instruction order is: this file, `docs/21-ai-agent-guide.md`, the applicable repository-scoped skill, then task-specific instructions. A narrower instruction must not silently weaken repository safety, consistency, approval or scope rules.
- Before modifying any file, use the read-only preparation gate in the AI agent guide: inspect current state, relevant documentation and history, complete affected flows, reusable implementations and cross-cutting impacts; report a minimal plan and unresolved decisions; then obtain explicit approval unless immediate implementation was clearly authorized in the same request.
- Search for an existing owner or reusable implementation before creating anything. Extend or reuse it where appropriate and justify every new architectural artifact.
- Make the smallest complete change. Do not refactor, modernize, optimize, redesign, rename, reformat or upgrade outside the approved requirement.
- Prefer the simplest design that keeps future approved business changes inexpensive, without implementing speculative functionality or unnecessary abstractions.
- Default to progress and use engineering judgment for ordinary implementation details. Request approval only when an unresolved decision would materially change security, authentication, authorization, public APIs, persisted data contracts, irreversible migrations, externally observable behavior or long-term architecture.
- Repository consistency is an acceptance criterion across architecture, naming, layering, contracts, errors, validation, security, tenancy, audit, migrations, localization, UI, tests, deployment and documentation.
- Repository-scoped skills are specialized workflows subordinate to this policy; do not duplicate the global policy into every skill.
- Permanent AI workflow knowledge belongs in version-controlled repository documentation. Do not commit or push it without explicit approval.

## Repository safety

- The workspace root is the full-project monorepo.
- `sami-frontend` and `sami-backend` are ordinary tracked directories, not submodules.
- Treat the root `development` branch as the shared integration source.
- Preserve unrelated worktree changes.
- Do not modify `main` unless explicitly instructed.
- Use `development` as the normal integration branch.

## Evidence rules

- Never infer completed functionality from a route, menu item, migration, type, package, placeholder, or lifecycle label alone.
- Never invent an unavailable backend contract, database schema, framework version, or environment variable.
- Report checks that could not run.
- Keep Persian and English behavior, RTL/LTR, permissions, audit, tenant scope, API contracts, and migrations synchronized when applicable.
