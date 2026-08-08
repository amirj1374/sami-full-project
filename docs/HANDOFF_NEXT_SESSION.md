# SAMI ERP — Next Session Handoff

Updated: 2026-08-08 (Asia/Tehran)

## Current repository state

- Authoritative branch: `development`.
- Cleanup baseline: `da44091645036e59f16fc6414a86aa542b94788c`.
- This handoff is part of the current reconciliation commit; resolve its exact
  revision with `git rev-parse HEAD` rather than copying a stale embedded SHA.
- The worktree was clean and synchronized with `origin/development` before the
  reconciliation pass. Verify again before implementation.
- The repository contains one worktree, no stash and no feature branches in
  the fetched local/remote refs.
- Deployed customer/VPS revision remains unknown; no Git document proves the
  current live SHA.

## Authoritative documents

Read, in order:

1. `AGENTS.md`
2. `.agents/SAMI_PROJECT_CONTEXT.md`
3. `docs/21-ai-agent-guide.md`
4. the relevant `.agents/skills/*/SKILL.md`
5. `docs/05-module-catalog.md`
6. `docs/IMPLEMENTATION_BACKLOG.md`

Use `docs/PROJECT_INDEX.md` to locate specialized architecture, testing,
configuration and deployment owners. Root release reports are historical
evidence for their recorded revisions, not current-state authorities.

## Current capability summary

- Complete core: Authentication/RBAC/Users, Dashboard, CRM, Suppliers,
  Products basic CRUD, Inventory, supplier Purchasing, Sales, Automation,
  Licensing, background Scheduler, PWA/Settings.
- Active priority: customer-origin Purchasing/trade-in recovery and completion.
- Partial/disabled: Data Quality, Files/Media, Dynamic Forms/Metadata,
  Appointments/Resources, Knowledge, Portal, Organization and Communication.
  Some have real backend/client/view code, but V35 deliberately excludes them
  from production navigation until completion and release validation.
- Notifications: real staff in-app inbox and opt-in hourly demo messages exist;
  closed-app Web Push does not.
- Planned: Repairs, Warranty, Installments and canonical Accounting.
- Reports: module reports exist; general reporting remains partial.

The detailed evidence and boundaries are in `docs/05-module-catalog.md`.

## Recent completed work and canonical owners

- Sales namespace/lifecycle/inventory integration: `sales/**`,
  `SalesView.vue`, migrations V31/V32/V36.
- Inventory ledger and public integration: `inventory/**`,
  `InventoryStockOperations`, `InventoryView.vue`, V32.
- Settings/mobile navigation/inbox: `ProfileView.vue`, `DefaultLayout.vue`,
  `usePwa.ts`, `StaffNotificationMenu.vue`, user-experience store/API,
  backend `notification/**` and user preferences, V37.
- Form/design system: `src/plugins/vuetify.ts`, `src/styles/global.css` and
  `src/components/AppFormSection.vue`. Prefer these owners over page-local
  spacing, typography and input overrides.
- Deployment automation: `scripts/deploy.ps1`,
  `scripts/verify-deployment.ps1` and `DEPLOYMENT_AUTOMATION_GUIDE.md`.

## Customer-purchase recovery status

The previous handoff named branch `codex/feature-customer-purchases`, worktree
`.worktrees/customer-purchases` and commit `744ca38`. After `git fetch --prune
origin`, none is present in this clone; `git cat-file -t 744ca38` reports an
unknown object, and no local/remote branch or stash contains it. Its changed
files therefore cannot be inspected or safely recovered from this repository.

Recovery procedure:

1. On the old workstation or backup, verify the worktree and run
   `git show --stat 744ca38`.
2. Export the branch with `git bundle create customer-purchases.bundle
   codex/feature-customer-purchases` or push it to an approved remote branch.
3. Transfer the bundle without overwriting this worktree, then fetch it into a
   reviewed feature branch.
4. Use the repository-reconciler and migration-guardian workflows before any
   rebase/cherry-pick.
5. The old proposed customer-purchase V37 conflicts with the applied Settings
   V37. Never rename an already-applied migration; recovered unfinished work
   must introduce the next available forward migration, currently V38.

Do not reconstruct the feature from the old narrative alone.

## Font and PWA status

- Implemented in source: bundled `@fontsource-variable/vazirmatn`, import in
  `src/main.ts`, global `--app-font-family`, and Vuetify application inheritance.
- Not proven: actual mobile font requests/glyph rendering, computed styles,
  offline/service-worker font behavior, and physical iOS/Android results.
- PWA install banner, permanent Settings install action, update handling and
  mobile-navigation preferences are implemented.
- Web Push subscription persistence, VAPID configuration and backend delivery
  provider are absent. Frontend Push API types/composables are only a foundation.

## Database and validation

- Flyway is contiguous and unique from V1 through
  `V37__user_experience_preferences_and_notifications.sql`.
- PostgreSQL 16 is expected; Hibernate uses schema validation and Flyway owns
  schema changes.
- Canonical commands are in `docs/15-testing-and-quality.md`.
- This workstation has Git and Node/npm, but only Java 17; Maven, Docker,
  Compose/Buildx and OpenSSH are unavailable. Backend, Flyway, Docker and
  browser release gates are therefore `BLOCKED_BY_WORKSTATION_TOOLING`.
- Setup instructions: `docs/NEW_WORKSTATION_SETUP.md`.

## Next five tasks

| Priority | Task | Read first | Main ownership |
|---|---|---|---|
| 1 | Recover and reconcile customer-origin Purchasing source | this handoff; `PURCHASING_RELEASE_REPORT.md`; repository-reconciler and migration-guardian skills | Purchasing, CRM, Inventory, Sales, migration V38+ |
| 2 | Verify/fix mobile font rendering on real mobile/PWA surfaces | `SAMI_FRONTEND_EXPERIENCE_REPORT.md`; PWA reports; frontend/UI skills | `main.ts`, `global.css`, Vuetify, nginx/service worker |
| 3 | Complete and release Data Quality, Files or Appointments one module at a time | module catalog; backlog; relevant migrations/controllers/views | existing disabled module slice; new forward lifecycle migration |
| 4 | Implement real Web Push only after architecture approval | `PUSH_NOTIFICATION_FOUNDATION.md`; contract/migration/backend skills | Notification Center → Communication provider boundary |
| 5 | Restore full workstation/release gates and verify a named revision | new-workstation setup; testing doc; deployment guide | Java/Maven/PostgreSQL/Docker/browser/deployment tooling |

Do not start Repairs, Warranty, Installments or Accounting before their
ownership and persisted contracts are explicitly approved.
