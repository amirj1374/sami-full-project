# Testing and quality

This is the canonical owner of validation commands and gates. Historical
reports record what passed at a named revision; they are not evidence that the
current checkout has passed.

## Frontend

From `sami-frontend`:

```powershell
npm ci
npm test
npm run type-check
npm run build
npm run dev
```

- `npm test` runs Node-based release/source-contract tests, including exact
  English/Persian key parity, route existence, permission seeding and partial
  module routing boundaries.
- `npm run type-check` runs `vue-tsc --noEmit`.
- `npm run build` repeats type-checking and creates the Vite production bundle.
- No lint, component-test or browser-E2E script is configured. Do not document
  or claim those gates until the repository actually provides them.

## Backend

From `sami-backend` using Java 21 and Maven 3.9+:

```powershell
mvn test
mvn clean package
mvn clean verify
mvn spring-boot:run
```

There is no Maven wrapper. The test tree contains focused unit/service/security
contract tests, but no current Testcontainers PostgreSQL/Flyway integration
suite. A complete release gate therefore also requires an isolated PostgreSQL
16 startup with Flyway and Hibernate schema validation.

## Documentation and backlog contracts

From the repository root:

```powershell
node scripts/validate-documentation.mjs
node --test scripts/validate-documentation.test.mjs
```

## Infrastructure and release gates

From `sami-backend`:

```powershell
docker compose config
docker compose up --build
docker compose -f docker-compose.prod.yml config
docker compose -f docker-compose.prod.yml build
```

From the repository root, deployment verification is owned by:

```powershell
.\scripts\deploy.ps1 -Mode Full -DryRun
.\scripts\verify-deployment.ps1 -BaseUrl <approved-base-url>
```

Production readiness additionally requires fresh/upgrade Flyway validation,
Docker health, authenticated API/browser smoke tests, responsive desktop/mobile
checks, English/Persian RTL/LTR checks and browser console/network inspection.

## Definition of Done

A change is not done until its applicable permissions, tenant isolation,
migrations, backend tests, TypeScript contracts, localization, RTL/LTR,
documentation and operational behavior are verified. Every unavailable or
skipped gate must be reported as unavailable—not converted into a pass.
