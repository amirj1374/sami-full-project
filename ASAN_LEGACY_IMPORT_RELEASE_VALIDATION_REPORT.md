# Legacy Asan Import — Release Validation Report

Validation date: 2026-08-08  
Release subject before validation changes: `development` at `7570e522de1a45d500c6f38ca4c3722f1cc66e59`

## Verdict

`READY_FOR_CUSTOMER_UI_TEST` with infrastructure validation pending. The repository-level backend, parser, archive-security, tenant-isolation, idempotency, comparison, frontend contract, localization, build, and fixture-backed browser gates pass. This is not a production-release verdict because PostgreSQL/Flyway and container gates could not run on this workstation.

## Backend evidence

- `mvn test`: passed before focused coverage was added.
- `mvn clean verify`: passed after focused coverage was added; 216 tests, 0 failures, 0 errors, 2 skipped. The two skipped cases are the external supplied-archive cases when `legacy.asan.archive` is not provided.
- Focused supplied-archive gate: 12 tests, 0 failures, 0 errors, 0 skipped.
- The focused run parsed the supplied `ji14050511.rar` through the production RAR5/Jet adapter and verified 190 files, 57 datasets, the 7,401-row `My_Zone` dataset, explicit unsupported classifications, and archive file-count rejection.
- Service/contract tests verify trusted tenant scope, exact tenant-scoped customer-code comparison, duplicate-hash object cleanup, no canonical customer/product/invoice writes, and legacy staging ownership.
- Java: Microsoft OpenJDK 21.0.12. Maven: 3.9.16.

Focused command:

```powershell
$archive='<absolute-path-to-ji14050511.rar>'
$unrar='<absolute-path-to-UnRAR executable>'
mvn `
  '-Dtest=AsanLegacyImportAdapterTest,LegacyNormalizerTest,LegacyImportServiceTest,LegacyImportContractTest' `
  "-Dlegacy.asan.archive=$archive" "-Dlegacy.unrar.executable=$unrar" test
```

## Frontend and browser evidence

- `npm test`: 10/10 passed, including English/Persian key parity, static route/permission/Flyway contracts, backend authorization permission seeding, and source/release contracts.
- `npm run type-check`: passed.
- `npm run build`: passed; Vite production bundle built successfully.
- Fixture-backed mock mode displayed the Legacy Asan history, completed-with-warnings batch, evidence counts, datasets, field dictionary, records, and messages without a backend database.
- English LTR and Persian RTL both set the correct document `lang` and `dir`.
- Widths 360, 375, 390, 412, 430 and desktop 1440 were checked in both languages. Final `documentElement.scrollWidth <= clientWidth` at every width.
- A fresh final browser tab reported no console errors.
- UX refinement: the global mobile header now constrains its title, hides the duplicate page title below 420 px, and hides the notification control below 420 px so essential navigation/search/account controls remain usable without RTL/LTR horizontal overflow. The in-page heading remains visible and notifications remain available at wider mobile/desktop sizes.

## Environment-blocked gates

`BLOCKED_BY_WORKSTATION_TOOLING`:

- fresh PostgreSQL Flyway V1→V40
- Docker linux/amd64 backend image
- Docker linux/amd64 frontend image
- production Compose
- nginx proxy
- container healthchecks

Docker Desktop/CLI and local PostgreSQL are unavailable. These gates were not retried and are not reported as passed.

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

docker compose -p $project -f sami-backend/docker-compose.prod.yml down -v
```

Acceptance on that workstation requires PostgreSQL healthy, Flyway V1→V40 successful on the fresh volume, backend/frontend healthy, nginx `/api` routing to the backend, both images reporting `linux/amd64`, and both OCI revision labels exactly matching `$sha`.
