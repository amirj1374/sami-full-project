# Getting started

## Prerequisites

| Component | Verified repository requirement |
|---|---|
| Backend | JDK 21; Maven 3.9+ is documented |
| Frontend | npm; Node compatible with Vite 8 (`^20.19.0` or `>=22.12.0`); Docker uses Node 22 |
| Database | PostgreSQL 16 in Compose |
| Containers | Docker Compose |

No `.nvmrc` or project-level `engines` field is present.

## Local setup

Copy safe examples and replace placeholders locally; never commit `.env`.

```bash
cd sami-backend
cp .env.example .env
mvn spring-boot:run
```

The backend defaults to port `8080`. Flyway applies migrations and Hibernate
validates the resulting schema. A local PostgreSQL database must be reachable
using the configured `DB_*` variables.

```bash
cd sami-frontend
npm ci
npm run dev
```

Vite listens on `7474` and proxies `/api` to `http://localhost:8080` unless
`VITE_DEV_API_TARGET` overrides it.

## Verification commands

```bash
cd sami-backend
mvn clean verify

cd ../sami-frontend
npm ci
npm run type-check
npm run build
```

These commands are derived from repository configuration. During the
2026-07-28 documentation pass they were not executed successfully because the
available machine had Java 17, no Maven/Docker, and no installed frontend
dependencies.

## Docker

Development backend/database:

```bash
cd sami-backend
docker compose up --build
```

Production-style full stack:

```bash
cd sami-backend
docker compose -f docker-compose.prod.yml up --build
```

Supply all variables that Compose marks required. Do not use example secrets in
production.

## Common startup failures

- **Java release error:** verify `java -version` reports 21.
- **Database refused:** check PostgreSQL health, `DB_URL`, user, database, and
  network; see [runbooks](runbooks/README.md).
- **Flyway validation failure:** do not edit an applied migration.
- **Browser CORS failure:** frontend origin should be `http://localhost:7474`
  for default development.
- **Frontend API 404:** verify `/api` proxy target and backend port.
