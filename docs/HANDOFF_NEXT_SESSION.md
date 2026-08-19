# SAMI ERP — Next Session Handoff

Updated: 2026-08-08 (Asia/Tehran)

## Current authoritative state

- Branch: `development`
- End-of-day revision: resolve with `git rev-parse HEAD`; it is the pushed end-of-day documentation commit.
- Remote: `origin/development` must match after the push verification recorded in `END_OF_DAY_HANDOFF_REPORT.md`.
- Expected worktree: clean; one worktree; no stash.
- Schema: contiguous Flyway V1–V42. Today added V38–V42 only; V1–V37 were not edited.
- No deployment was performed.

## Completed today

### Repository/documentation reconciliation — DOCUMENTATION_ONLY

- Reconciled canonical guidance, workstation setup, testing, module catalog, backlog and reports.
- Owners: `docs/PROJECT_INDEX.md`, this handoff and `.agents/SAMI_PROJECT_CONTEXT.md`.

### Customer Purchase / Trade-in — COMPLETE implementation, release infrastructure pending

- Supports Supplier or CRM Customer sellers, inspection/valuation/settlement, linked-Sale integrity, CRM history, canonical Inventory receipt and IMEI validation.
- Backend: `purchasing/**`, CRM/Sales/Inventory public boundaries. Frontend: `PurchaseFormDialog.vue`, models/localization.
- Migration: V38. Existing Purchasing route/menu; existing permissions remain canonical.
- Data Quality activation: V39, route `/data-quality`, permission `data-quality:view`.
- Evidence: backend/frontend source gates, production build and responsive/font browser checks passed; fresh Flyway/containers remain pending.

### Legacy Asan Import & Comparison — COMPLETE for customer UI testing

- Secure RAR/Jet staging, normalization, conservative comparison, tenant isolation, idempotency and archive limits; never writes canonical business data.
- Backend: `legacyimport/**`. Frontend: `/legacy-imports`, `LegacyImportsView.vue`, typed client and mock fixtures.
- Migration: V40. Permissions: `legacy-import:view/upload/execute/compare/delete`.
- Evidence: focused supplied-archive/parser/security tests and full local gates passed. Fresh Flyway/containers remain pending.

### HAMTA Activation Code Management — COMPLETE implementation, release infrastructure pending

- Inventory-owned one-code-per-eligible-serial custody, controlled correction, Sales invoice/delivery exposure, reporting and audit.
- Backend: `hamta/**`, Purchasing/Inventory/Product/Sales integration. Frontend: `/hamta`, `HamtaView.vue`, Product/Purchase/Sales integration.
- Migration: V41. Permissions: `hamta:view/manage/correct/invoice/deliver/report`.
- Evidence: focused contract tests, frontend gates and Persian responsive fixture passed. Fresh Flyway/live bilingual/container smoke remains pending.

### Market Sync & Auto Pricing — PARTIAL production capability

- Implemented tenant sources, structured adapter boundary, normalization, deterministic pricing, Inventory/Sales sold-lock integration, rules, history, scheduler, health and admin UI.
- Backend: `marketsync/**`, Product/Inventory/Sales/Scheduler boundaries. Frontend: `/market-sync`, `MarketSyncView.vue`, Product market status.
- Migration: V42. Permissions: `market-sync:view/manage-sources/manage-pricing/manage-publication/execute/view-history/view-errors`.
- Local backend/frontend/browser gates passed. Production remains partial because authorized Rond structured contracts and a real website publication connector are absent; publication fails closed.

### Typography/mobile validation — COMPLETE validation

- Vazirmatn remains the centralized global font. Persian RTL and English LTR production/mobile paths were validated with available fixtures/build output.
- No separate typography architecture was introduced.

## Partial work

- Market Sync external connectivity: contained on `development`; no separate partial branch. Obtain an authorized structured Rond contract, implement/configure the real website publication adapter, then run live idempotency and failure tests.
- Web Push: NOT_STARTED. Browser/service-worker foundation exists, but backend subscriptions, VAPID secret ownership and delivery require architecture/security approval in `FULL_NEXT_PHASE_IMPLEMENTATION_REPORT.md`.
- Files/Media and Appointments/Resources remain partial and intentionally unrouted. Repairs, Warranty, Installments and canonical Accounting remain planned.
- Local feature branches `codex/feature-legacy-asan-import`, `codex/feature-hamta-activation-code` and `codex/feature-market-sync-auto-pricing` contain no commits missing from `development` and do not need pushing.

## Environment-blocked gates

- Docker Desktop/CLI, Compose and Buildx are unavailable: linux/amd64 images, production Compose, nginx proxy and container healthchecks were not run.
- OpenSSH Client is unavailable; no deployment or remote verification was attempted.
- A PostgreSQL 17 Windows service is running, but no approved isolated database credentials/configuration were available. Fresh PostgreSQL 16 Flyway V1→V42, upgrade-path validation, schema validation and database-backed integration tests were not run.
- Real mobile/iOS/Android installed-PWA checks remain pending.

## Home workstation checklist

First synchronize exactly:

```powershell
git fetch origin
git switch development
git pull --ff-only origin development
git status
git log -1 --oneline
```

Then validate without deploying:

```powershell
$sha = git rev-parse HEAD
$short = $sha.Substring(0,12)
$project = "sami-release-$short"
$timestamp = (Get-Date).ToUniversalTime().ToString('o')

java -version
mvn -version
docker version
docker compose version
docker buildx version
ssh -V

Set-Location sami-backend
mvn clean verify
Set-Location ..

docker buildx build --platform linux/amd64 --load `
  --build-arg BUILD_BRANCH=development --build-arg BUILD_COMMIT=$sha `
  --build-arg APP_VERSION=0.1.0 -t sami-backend:eod ./sami-backend
docker buildx build --platform linux/amd64 --load `
  --build-arg VITE_API_BASE_URL=/api --build-arg VITE_BUILD_BRANCH=development `
  --build-arg VITE_BUILD_COMMIT=$sha --build-arg VITE_BUILD_TIMESTAMP=$timestamp `
  --build-arg VITE_APP_VERSION=0.1.0 -t sami-frontend:eod ./sami-frontend

$env:POSTGRES_PASSWORD='<strong throwaway value>'
$env:JWT_SECRET='<at least 32 random bytes>'
$env:PORTAL_JWT_SECRET='<different at least 32 random bytes>'
$env:BOOTSTRAP_ADMIN_PASSWORD='<strong throwaway value>'
$env:BUILD_BRANCH='development'
$env:BUILD_COMMIT=$sha
$env:BUILD_TIMESTAMP=$timestamp
$env:BACKEND_IMAGE='sami-backend:eod'
$env:FRONTEND_IMAGE='sami-frontend:eod'

docker compose -p $project -f sami-backend/docker-compose.prod.yml config
docker compose -p $project -f sami-backend/docker-compose.prod.yml up -d
docker compose -p $project -f sami-backend/docker-compose.prod.yml ps
docker compose -p $project -f sami-backend/docker-compose.prod.yml logs --no-color backend
docker image inspect sami-backend:eod --format '{{.Os}}/{{.Architecture}} {{index .Config.Labels "org.opencontainers.image.revision"}}'
docker image inspect sami-frontend:eod --format '{{.Os}}/{{.Architecture}} {{index .Config.Labels "org.opencontainers.image.revision"}}'
Invoke-WebRequest -UseBasicParsing http://localhost/health
Invoke-WebRequest -UseBasicParsing http://localhost/api/v1/menu -SkipHttpErrorCheck
& ./scripts/verify-deployment.ps1 -BaseUrl http://localhost -PassThru
```

Acceptance requires fresh-volume Flyway V1→V42, backend integration/schema validation, healthy PostgreSQL/backend/frontend, nginx `/api`, login, provenance equal to `$sha`, and browser/mobile RTL/LTR smoke for Customer Purchase, Data Quality, Legacy Asan, HAMTA, Market Sync and font rendering. Tear down the throwaway stack afterward; do not deploy.

## Deployment candidate update — 2026-08-17

- **Application source:** `development` at
  `8c019753e76153487bd9541e47ae8d77ae023d78`. A subsequent documentation-only
  commit, if present, must not be used as the image provenance.
- Validation completed locally with Docker Desktop: official Maven/Java 21
  `mvn clean verify` (240 tests, zero failures/errors/skips); frontend
  `npm ci`, 25 source-contract tests, type-check and production build; and a
  fresh PostgreSQL 16 production-like Compose stack.
- Fresh runtime evidence: Flyway V1–V44, Spring/Hibernate startup, all three
  service health checks, nginx SPA and protected API proxy, bootstrap login,
  authenticated menu and authenticated user endpoints passed.
- Validated export images are `sami-backend:test`
  (`sha256:95330a3d3dd573ac8e54c136f9918f2b37e2dcd1b2d31c2f113f01dfb852ce6a`)
  and `sami-frontend:test`
  (`sha256:c8c6fee83c7ada9b60296f8c584bb9319c5486c0cf4b4e402f0e23581fd45a98`),
  both linux/amd64 and OCI-labelled with the application SHA. TAR artifacts
  are intentionally ignored under `deployment-artifacts/`.
- The legacy portions of this handoff that say Docker was unavailable or end
  at V42 are historical only; do not treat them as current release evidence.
