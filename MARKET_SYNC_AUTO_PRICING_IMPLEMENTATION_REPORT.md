# Market Sync and Auto Pricing implementation report

Date: 2026-08-08

## Delivered

- Tenant-scoped source, profile, product-state, rule, run/message, price-history, publication and audit persistence in Flyway V42.
- Fail-safe authorized structured JSON adapter with timeouts and per-row isolation; undocumented Rond scraping is not implemented.
- Backend-only deterministic pricing, preview, minimum-profit verification and min/max publication checks.
- Canonical Product upsert and Inventory availability integration; Sale completion produces a sold lock that source sync cannot undo.
- Blacklist/whitelist evaluation, source-collision handling, missing/reappearing state, price-change history and scheduler integration.
- Permission-gated bilingual Vue workspace and Product-listing market status details.

## Local evidence

- Backend focused pricing/schema contracts and full Maven gates: see final implementation commit validation.
- Frontend unit/release-contract tests, localization parity, type-check and production build: see final implementation commit validation.
- Fixture-backed browser inspection: Persian RTL at 360, 375, 390, 412, 430 and 1280 pixels; English LTR at 390 pixels; document scroll width equaled client width at every size.

## Explicit blockers

`BLOCKED_BY_WORKSTATION_TOOLING_OR_EXTERNAL_CONTRACT`:

- Fresh PostgreSQL 16 Flyway V1 through V42 and upgrade validation.
- Backend/frontend linux/amd64 Docker builds, production Compose, nginx proxy and container healthchecks.
- Live authenticated end-to-end API/UI smoke.
- Authorized Rond structured data contract and credentials, if required.
- Concrete website publication connector and its end-to-end idempotency validation.

The module remains non-production-ready until these gates pass. Publication fails closed while the website connector is absent.

## Deferred commands

From the repository root on a Docker-enabled workstation:

```powershell
java -version
mvn -version
docker version
docker compose version
docker buildx version
ssh -V
Set-Location sami-backend
mvn clean verify
docker compose down --volumes
docker compose up -d postgres
docker compose run --rm backend mvn -DskipTests flyway:migrate
docker buildx build --platform linux/amd64 --load -t sami-backend:market-sync .
Set-Location ..\sami-frontend
docker buildx build --platform linux/amd64 --load -t sami-frontend:market-sync .
Set-Location ..\sami-backend
docker compose config
docker compose up -d --build
docker compose ps
```

Then run the repository health/login smoke and verify Market Sync source, pricing, stock, Sale lock, publication failure/success behavior, Products integration, Persian RTL and mobile widths against the live stack.
