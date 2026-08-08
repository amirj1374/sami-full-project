# Repository Reconciliation Report

Date: 2026-08-08 (Asia/Tehran)

## Scope and revision

- Starting branch: `development`.
- Starting SHA: `da44091645036e59f16fc6414a86aa542b94788c`.
- Starting sync: clean and identical to freshly fetched `origin/development`.
- Final SHA: the reconciliation commit containing this report; resolve with
  `git rev-parse HEAD` (a commit cannot embed its own content-derived SHA).
- Push/deployment: not performed.

## Documentation corrected

- Made `docs/PROJECT_INDEX.md` the documentation entry point and defined
  canonical ownership versus historical reports.
- Rebuilt `docs/05-module-catalog.md` with the requested current taxonomy and
  evidence-backed module boundaries.
- Corrected `docs/15-testing-and-quality.md` to include the real frontend
  `npm test` command and exact frontend/backend/documentation/infrastructure gates.
- Clarified configuration ownership in `docs/16-configuration-and-environments.md`.
- Replaced the stale handoff with a concise operational current handoff.
- Reduced `.agents/SAMI_PROJECT_CONTEXT.md` to reusable stack, ownership,
  status, migration, gate and priority facts.
- Added `docs/NEW_WORKSTATION_SETUP.md` and
  `docs/IMPLEMENTATION_BACKLOG.md`.
- Corrected canonical deployment references from the nonexistent
  `scripts/deploy-sami.ps1` to `scripts/deploy.ps1`.

Historical release/audit reports were intentionally retained. Their old
migration ranges, branches, stashes and validation results remain valid only
for their recorded revision and are now labelled as historical by the index.

## Stale state resolved

- Licensing is now documented as COMPLETE; current source includes its backend,
  route, permissions, localization and `LicensingView.vue`.
- Data Quality, Files and Appointments are no longer described as backend-only:
  each has backend, typed client, localization and a Vue view.
- They remain PARTIAL because V35 deliberately disables them and the current
  release-contract test deliberately excludes them from static routes.
- The old handoff's worktree, branch, stash and commit `744ca38` do not exist in
  this fetched clone. Recovery instructions now require an old-workstation Git
  bundle or approved remote branch.
- Font documentation now separates bundled/global Vazirmatn source
  configuration from unproven device/computed/offline behavior.
- PWA/in-app notification implementation is separated from absent backend
  Web Push subscription/VAPID delivery.
- Current migration range is consistently V1–V37 in canonical documents.
- The live deployed customer revision remains explicitly unknown.

## Route, menu, permission and placeholder findings

No production route/menu/permission mutation was safe or required.

- Static routes, their route permissions and Flyway permission seeds pass the
  source-contract suite.
- Sales consistently uses the `sales` namespace.
- English/Persian localization keys are in exact parity.
- Data Quality, Files and Appointments have intentionally orphaned partial views;
  V35 disables their module rows, and the release-contract suite protects the
  absence of direct routes. Activating them during cleanup would bypass the
  recorded release boundary.
- Placeholder lifecycle content remains valid for genuinely incomplete modules;
  no completed module was found rendering a misleading placeholder.

## Form, font, notification and PWA findings

- Shared form ownership is present in Vuetify defaults, `global.css` tokens and
  `AppFormSection.vue`; no obvious conflicting global override justified removal.
- The production build emits bundled Vazirmatn Arabic/Latin font assets.
  Physical mobile requests, computed styles and offline behavior were not tested.
- PWA install access, Settings, mobile-navigation preferences, staff inbox and
  opt-in hourly demo notifications are implemented.
- Closed-app Web Push remains unimplemented; frontend Push API code is only a
  provider-neutral foundation.

## Customer-purchase recovery and migration state

- `git cat-file -t 744ca38` fails with unknown object.
- No local branch, fetched remote branch, stash or worktree contains the named
  customer-purchase work.
- Its files could not be inspected or recovered from this repository.
- Flyway files are unique, contiguous and convention-compliant from V1 to V37.
- No migration was modified or created.
- Recovered customer-purchase work must use the next forward version, currently
  V38; the old proposed V37 conflicts with Settings V37.

## Repository artifacts

No accidentally tracked TAR, log, build-output, IDE or temporary audit artifact
was found. Tracked PWA icons are intentional. Existing ignore rules already
cover `.idea`, local `.env`, deployment artifacts, local release config and
worktrees; `.gitignore` required no change.

## Validation results

- `npm ci`: PASS; 185 packages installed from the lockfile. npm reported one
  moderate and two high audit findings plus the existing Vue I18n v9
  deprecation. No dependency was changed because upgrades are outside scope.
- `npm test`: PASS — 9/9 tests.
- `npm run type-check`: PASS.
- `npm run build`: PASS — Vite 8.1.5, 1,021 modules transformed.
- Localization parity, static route existence, backend/frontend permission
  seeding, partial-module route boundary, Sales namespace, PWA and mobile-nav
  contracts: PASS through the frontend suite.
- `node scripts/validate-documentation.mjs`: PASS.
- `node --test scripts/validate-documentation.test.mjs`: PASS — 5/5 tests.
- `git diff --check`: PASS before final commit.

`BLOCKED_BY_WORKSTATION_TOOLING`:

- Backend Maven tests/verify/startup: Java 21 and Maven unavailable.
- PostgreSQL/Flyway fresh/upgrade validation: Docker/PostgreSQL unavailable.
- Docker/Compose/Buildx builds and health checks: Docker unavailable.
- Browser/mobile/RTL network verification: no running backend/container stack.
- Deployment dry-run/SSH verification: Docker and OpenSSH unavailable.

## Files intentionally untouched

- All V1–V37 Flyway migrations.
- Backend/frontend business logic and public contracts.
- Partial module lifecycle state and static-route exclusion.
- Dependency manifests and lockfile.
- Historical implementation, branch, deployment and release reports.
- Production/VPS state and all secrets.

## Prioritized next work

1. Recover customer-purchase Git history, then reconcile and complete it on V38+.
2. Install/verify Java 21, Maven, Docker and OpenSSH and restore full release gates.
3. Verify/fix mobile font behavior on production-like mobile/PWA surfaces.
4. Complete one disabled partial module—Data Quality, Files or Appointments—through a full release task.
5. Design/implement real Web Push only after security/configuration approval.

The complete P0–P3 task definitions are in `docs/IMPLEMENTATION_BACKLOG.md`.

## Remaining verified risks

- Customer-purchase source may be lost unless recovered externally.
- The deployed customer revision is unknown.
- Backend/database/container behavior at this reconciliation revision is unverified.
- npm reports three dependency audit findings; remediation requires a separate
  dependency/security task and compatibility validation.
- Partial source views can drift while disabled unless completed or explicitly
  retired in future approved work.
