# Legacy Asan Import — Release Validation Report

Initial validation date: 2026-08-08

Manifest-driven package extension validation: 2026-08-19

Extension base revision: `development` at `daa163c0c6e99f58276b874486637ffc93d88f18`

## Verdict

`READY_FOR_CUSTOMER_UI_TEST` with infrastructure validation pending. The repository-level backend, parser, archive-security, tenant-isolation, idempotency, comparison, frontend contract, localization, build, and fixture-backed browser gates pass. This is not a production-release verdict because PostgreSQL/Flyway and container gates could not run on this workstation.

## Backend evidence

- `mvn test`: passed before focused coverage was added.
- `mvn clean verify`: passed after focused coverage was added; 216 tests, 0 failures, 0 errors, 2 skipped. The two skipped cases are the external supplied-archive cases when `legacy.asan.archive` is not provided.
- Focused supplied-archive gate: 12 tests, 0 failures, 0 errors, 0 skipped.
- The focused run parsed the supplied `ji14050511.rar` through the production RAR5/Jet adapter and verified 190 files, 57 datasets, the 7,401-row `My_Zone` dataset, explicit unsupported classifications, and archive file-count rejection.
- Service/contract tests verify trusted tenant scope, exact tenant-scoped customer-code comparison, duplicate-hash object cleanup, no canonical customer/product/invoice writes, and legacy staging ownership.
- Java: Microsoft OpenJDK 21.0.12. Maven: 3.9.16.
- 2026-08-19 full `mvn clean verify`: 244 tests, 0 failures, 0 errors, 0 skipped.
- 2026-08-19 focused Excel/ZIP plus RAR-regression run: 14 tests, 0 failures, 0 errors, 0 skipped.

Focused command:

```powershell
$archive='<absolute-path-to-ji14050511.rar>'
$unrar='<absolute-path-to-UnRAR executable>'
mvn `
  '-Dtest=AsanLegacyImportAdapterTest,LegacyNormalizerTest,LegacyImportServiceTest,LegacyImportContractTest' `
  "-Dlegacy.asan.archive=$archive" "-Dlegacy.unrar.executable=$unrar" test
```

## Frontend and browser evidence

- 2026-08-19 `npm test`: 25/25 passed, including English/Persian key parity, static route/permission/Flyway contracts, backend authorization permission seeding, and the RAR/ZIP release contract.
- `npm run type-check`: passed.
- `npm run build`: passed; Vite production bundle built successfully.
- Fixture-backed mock mode displayed the Legacy Asan history, completed-with-warnings batch, evidence counts, datasets, field dictionary, records, and messages without a backend database.
- English LTR and Persian RTL both set the correct document `lang` and `dir`.
- Widths 360, 375, 390, 412, 430 and desktop 1440 were checked in both languages. Final `documentElement.scrollWidth <= clientWidth` at every width.
- A fresh final browser tab reported no console errors.
- UX refinement: the global mobile header now constrains its title, hides the duplicate page title below 420 px, and hides the notification control below 420 px so essential navigation/search/account controls remain usable without RTL/LTR horizontal overflow. The in-page heading remains visible and notifications remain available at wider mobile/desktop sizes.

## Environment-blocked gates

`BLOCKED_BY_WORKSTATION_TOOLING`:

- fresh PostgreSQL Flyway V1→V45
- Docker linux/amd64 backend image
- Docker linux/amd64 frontend image
- production Compose
- nginx proxy
- container healthchecks

Docker CLI, Compose and Buildx are installed, but Docker Desktop/engine is unavailable. A local PostgreSQL 17 service was detected but was not used because it is not the requested isolated PostgreSQL 16 test database. These gates are not reported as passed.

## Exact Docker-enabled workstation validation

Run from a clean `development` worktree. Supply strong throwaway local values through the environment; do not commit them.

```powershell
$sha = git rev-parse HEAD
$short = $sha.Substring(0, 12)
$project = "sami-legacy-release-$short"
$timestamp = (Get-Date).ToUniversalTime().ToString('o')
git status --porcelain
docker version
docker compose version
docker buildx version

docker buildx build --platform linux/amd64 --load `
  --build-arg BUILD_BRANCH=development --build-arg BUILD_COMMIT=$sha --build-arg APP_VERSION=0.1.0 `
  -t sami-backend:test ./sami-backend

docker buildx build --platform linux/amd64 --load `
  --build-arg VITE_API_BASE_URL=/api --build-arg VITE_BUILD_BRANCH=development `
  --build-arg VITE_BUILD_COMMIT=$sha --build-arg VITE_BUILD_TIMESTAMP=$timestamp `
  --build-arg VITE_APP_VERSION=0.1.0 -t sami-frontend:test ./sami-frontend

docker image inspect sami-backend:test --format '{{.Os}}/{{.Architecture}} {{index .Config.Labels "org.opencontainers.image.revision"}}'
docker image inspect sami-frontend:test --format '{{.Os}}/{{.Architecture}} {{index .Config.Labels "org.opencontainers.image.revision"}}'

$env:POSTGRES_PASSWORD='<strong throwaway value>'
$env:JWT_SECRET='<at least 32 random bytes>'
$env:PORTAL_JWT_SECRET='<different at least 32 random bytes>'
$env:BOOTSTRAP_ADMIN_PASSWORD='<strong throwaway value>'
$env:BUILD_BRANCH='development'
$env:BUILD_COMMIT=$sha
$env:BUILD_TIMESTAMP=$timestamp
$env:BACKEND_IMAGE='sami-backend:test'
$env:FRONTEND_IMAGE='sami-frontend:test'

docker compose -p $project -f sami-backend/docker-compose.prod.yml config
docker compose -p $project -f sami-backend/docker-compose.prod.yml up -d
docker compose -p $project -f sami-backend/docker-compose.prod.yml ps
docker compose -p $project -f sami-backend/docker-compose.prod.yml logs --no-color backend

Invoke-WebRequest -UseBasicParsing http://localhost/health
Invoke-WebRequest -UseBasicParsing http://localhost/api/v1/menu -SkipHttpErrorCheck
& ./scripts/verify-deployment.ps1 -BaseUrl http://localhost -PassThru

# Sign in with the throwaway administrator, open /legacy-imports, upload the
# supplied ZIP, then run Analyze, Staging Import and Compare in that order.
# Export/review the discrepancy evidence before any separately approved
# canonical promotion. This revision intentionally has no canonical-promotion action.

docker compose -p $project -f sami-backend/docker-compose.prod.yml exec -T db `
  psql -U sami -d sami -c "select status, record_count, dataset_count, warning_count, error_count from legacy_import_batches order by id desc limit 1;"
docker compose -p $project -f sami-backend/docker-compose.prod.yml exec -T db `
  psql -U sami -d sami -c "select count(*) as staged_records from legacy_records;"

docker compose -p $project -f sami-backend/docker-compose.prod.yml down -v
```

Acceptance on that workstation requires PostgreSQL healthy, Flyway V1→V45 successful on the fresh volume, backend/frontend healthy, nginx `/api` routing to the backend, both images reporting `linux/amd64`, and both OCI revision labels exactly matching `$sha`.

## 2026-08-19 manifest-driven Excel package extension

- Package SHA-256: `5649E744CF7A2E36888B7FF1CEFEDB5CCEC156190FB757113E2992B5AB2C1E13`.
- Archive safety inventory: 37 entries, zero traversal paths, 12,229,349 expanded bytes, no detected VBA/external-link/embedded-object entries.
- Production adapter result: 37 files, 35 datasets, 77,475 source rows, 77,475 staged-record objects, zero parser errors, 21 data-quality warnings, approximately 12.5 seconds.
- Focused backend result: 14 tests passed across Excel/ZIP parsing, RAR regression, archive traversal, manifest requirements, tenant scope, duplicate upload cleanup and no-canonical-write contracts.
- Migration V45 adds only additive JSONB dataset metadata; no historical migration was modified.
- The source trial-balance period turnover reconciles debit to credit. The standalone journal and daily-operation exports do not independently balance, so they require reviewed mapping rather than automatic final promotion.
- Database-backed staging/Flyway remains `BLOCKED_BY_WORKSTATION_TOOLING`: Docker CLI exists, but Docker Desktop/engine is unavailable. No PostgreSQL database was modified.
- Final canonical accounting import remains blocked because SAMI has no canonical accounting, bank-transaction or cash-transaction owner yet and the package itself requires discrepancy reporting plus manager approval before final import.
