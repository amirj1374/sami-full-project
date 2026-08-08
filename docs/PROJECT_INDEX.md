# SAMI ERP Project Index

This is the documentation entry point. When documents disagree, current source
and configuration win. Use the ownership table below before consulting
historical reports.

## Document ownership

| Document | Canonical responsibility |
|---|---|
| `docs/PROJECT_INDEX.md` | Documentation map and trust hierarchy |
| `.agents/SAMI_PROJECT_CONTEXT.md` | Concise reusable Codex technical context |
| `docs/HANDOFF_NEXT_SESSION.md` | Current development handoff and immediate priorities |
| `docs/05-module-catalog.md` | Current module capability/status catalog |
| `docs/15-testing-and-quality.md` | Current validation commands and required gates |
| `docs/16-configuration-and-environments.md` | Configuration groups, ports and environment topology |
| `PROJECT_SETUP_AND_DEPLOYMENT.md` | Local startup and deployment topology |
| `DEPLOYMENT_AUTOMATION_GUIDE.md` | Canonical automated release/deploy/rollback workflow |
| `docs/IMPLEMENTATION_BACKLOG.md` | Prioritized incomplete and future work |
| Feature/release reports | Historical evidence for the named revision only |

## Start Here

- `AGENTS.md` — mandatory repository policy.
- `.agents/SAMI_PROJECT_CONTEXT.md` — fast technical context.
- `docs/HANDOFF_NEXT_SESSION.md` — current state and next actions.
- `docs/21-ai-agent-guide.md` — mandatory AI development workflow.
- `docs/00-project-overview.md` and `docs/02-repository-map.md` — product and monorepo orientation.
- `docs/NEW_WORKSTATION_SETUP.md` — Windows prerequisites and verification.

## Architecture

- `docs/03-system-architecture.md` — system boundaries and dependency direction.
- `docs/04-domain-model.md` — domain concepts and ownership.
- `docs/08-api-and-integrations.md` — API conventions and integration seams.
- `docs/09-authentication-and-authorization.md` and `docs/18-security.md` — authentication, RBAC and security.
- `docs/10-multi-tenancy-and-organization.md` — tenant and organization scope.
- `docs/11-events-automation-and-scheduling.md` — events, automation and schedulers.
- `docs/12-files-and-communications.md` — shared file and communication foundations.
- `docs/23-known-risks-and-technical-debt.md` and `docs/24-roadmap-and-open-decisions.md` — risks and unresolved architecture decisions.

## Backend

- `docs/14-backend-architecture.md` — Java/Spring layering and conventions.
- `sami-backend/README.md` — backend-local startup and command reference.
- `.agents/skills/sami-backend-builder/SKILL.md` — backend implementation workflow.
- `.agents/skills/sami-contract-validator/SKILL.md` — API/client reconciliation workflow.

## Frontend/UI

- `docs/13-frontend-architecture.md` — Vue application structure.
- `SAMI_FRONTEND_EXPERIENCE_REPORT.md` — historical UX foundation evidence.
- `LOCALIZATION_COMPLETION_REPORT.md` — historical localization/RTL evidence.
- `PWA_IMPLEMENTATION_REPORT.md` and `PUSH_NOTIFICATION_FOUNDATION.md` — PWA implementation and explicitly incomplete Web Push boundary.
- `SETTINGS_MOBILE_NAVIGATION_NOTIFICATION_REPORT.md` — Settings, mobile navigation and inbox implementation evidence.

## Modules

- `docs/05-module-catalog.md` — canonical current status for every major module.
- `docs/modules/README.md` — package entry points and extension guidance.
- `docs/06-business-workflows.md` — implemented workflow map.
- `docs/22-glossary.md` — shared business terminology.
- `docs/IMPLEMENTATION_BACKLOG.md` — prioritized completion work.

## Database/Migrations

- `docs/07-database-and-migrations.md` — Flyway ownership and migration rules.
- `.agents/skills/sami-migration-guardian/SKILL.md` — migration safety workflow.
- Current migration directory: `sami-backend/src/main/resources/db/migration`.

## Testing

- `docs/15-testing-and-quality.md` — canonical commands and release gates.
- `PROJECT_BACKLOG/DEFINITION_OF_DONE.md` — backlog completion criteria.
- `.github/workflows/ci.yml` — automated CI configuration when present.

## Deployment

- `PROJECT_SETUP_AND_DEPLOYMENT.md` — setup, local execution and production topology.
- `DEPLOYMENT_AUTOMATION_GUIDE.md` — canonical `scripts/deploy.ps1` workflow.
- `DEPLOYMENT_COMMANDS.md` and `LINUX_SERVER_DOCKER_DEPLOYMENT_GUIDE.md` — operational command references.
- `scripts/verify-deployment.ps1` — non-mutating deployment smoke checks.
- The actual deployed customer revision is currently unverified; historical reports do not establish live state.

## Feature Reports

- `SALES_RELEASE_REPORT.md` and `SALES_DOMAIN_DESIGN.md` — Sales design/release evidence.
- `INVENTORY_RELEASE_REPORT.md` — Inventory release evidence.
- `PURCHASING_RELEASE_REPORT.md` — supplier Purchasing release evidence.
- `AUTOMATION_RELEASE_REPORT.md` — Automation release evidence.
- `LICENSING_RELEASE_REPORT.md` — Licensing release evidence.
- `PWA_IMPLEMENTATION_REPORT.md` and `SETTINGS_MOBILE_NAVIGATION_NOTIFICATION_REPORT.md` — PWA/Settings evidence.
- `ASAN_LEGACY_IMPORT_IMPLEMENTATION_REPORT.md` and `docs/features/LEGACY_ASAN_IMPORT_AND_COMPARISON.md` — secure legacy staging, archive evidence, comparison policy and operational requirements.

## Current Handoff

- `docs/HANDOFF_NEXT_SESSION.md` — current repository facts, tooling blockers and next five tasks.
- `.agents/SAMI_PROJECT_CONTEXT.md` — compact context for future Codex work.
- `REPOSITORY_RECONCILIATION_REPORT.md` — latest cleanup/reconciliation evidence.

## Historical Reports

Root-level files ending in `_REPORT.md` or `_AUDIT.md` preserve implementation,
branch-consolidation and release evidence for their recorded SHA/date. Statements
such as migration ranges, branch names, stashes, tool availability or “current”
status inside them are historical unless a canonical document above confirms
them. Preserve these reports; do not use them as the current handoff.

## Repository-scoped workflows

Specialized workflows live under `.agents/skills/`: project context,
architecture audit, module/backend/frontend builders, contract validation,
migration guardian, repository reconciliation, UI/UX, reporting and release
gate. Contribution rules are in `docs/20-contribution-guide.md`.
# Current implementation update — 2026-08-08

- Customer Purchase / Trade-in: Purchasing now supports Supplier or CRM Customer sellers with valuation, inspection, settlement, linked Sale, CRM timeline, inventory and IMEI integrity (`V38`).
- Data Quality: production route and lifecycle are active (`V39`).
- Legacy Asan import and comparison: tenant-safe staging, verified Jet parsing, RAR controls and conservative comparison are implemented (`V40`).
- Font system: Vazirmatn Variable is globally bundled and browser-verified.
- Full phase evidence and Web Push approval proposal: [`FULL_NEXT_PHASE_IMPLEMENTATION_REPORT.md`](../FULL_NEXT_PHASE_IMPLEMENTATION_REPORT.md).
