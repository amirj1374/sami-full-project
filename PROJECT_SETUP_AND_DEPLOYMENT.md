# SAMI Project Setup and Deployment

This guide documents repository-supported ways to run and deploy the complete
SAMI monorepo. Commands assume the repository root unless a working directory
is stated.

## 1. Purpose and architecture

The repository contains:

- `sami-backend`: Java 21, Spring Boot 3.5.3, Maven, Spring Security JWT,
  Spring Data JPA, Flyway and PostgreSQL.
- `sami-frontend`: Vue 3, TypeScript, Vuetify, Pinia, Vue Router, npm and Vite.
- PostgreSQL 16 in the provided Compose files.
- Flyway migrations under
  `sami-backend/src/main/resources/db/migration`.
- nginx in the frontend production image, serving the SPA and proxying `/api`
  to the backend.

Supported approaches:

1. Native development: external PostgreSQL, Maven backend and Vite frontend.
2. Local Docker backend stack: PostgreSQL and backend through
   `sami-backend/docker-compose.yml`; run the frontend separately.
3. Production-style full stack: PostgreSQL, backend and nginx frontend through
   `sami-backend/docker-compose.prod.yml`.

## 2. Prerequisites

### Native development

- Git
- JDK 21
- Maven (no Maven wrapper is present; Maven 3.9+ is documented)
- Node.js compatible with Vite 8 (`^20.19.0` or `>=22.12.0`); CI and Docker use
  Node 22
- npm; `sami-frontend/package-lock.json` is lockfile version 3
- PostgreSQL; Compose uses PostgreSQL 16

### Docker development

- Git
- Docker Engine or Docker Desktop
- Docker Compose v2 (`docker compose`)
- Node.js/npm only when running the Vite frontend outside Compose

### Production Compose host

- Git or another approved release-transfer method
- Docker Engine and Docker Compose v2
- Enough persistent storage for PostgreSQL and uploaded files
- Host firewall, DNS and TLS termination configured by the operator

No server operating system, cloud provider or infrastructure-as-code platform
is prescribed. The containers are Linux-based.

## 3. Repository setup

```bash
git clone <repository-url> sami-full-project
cd sami-full-project
git switch development
```

Use an approved release branch or immutable revision for production rather than
deploying arbitrary development work.

For Compose configuration:

```powershell
Set-Location sami-backend
Copy-Item .env.example .env
```

```bash
cd sami-backend
cp .env.example .env
```

Replace development placeholders. Never commit `.env`, passwords, tokens,
private keys or production host details.

Install frontend dependencies from its lockfile:

```bash
cd sami-frontend
npm ci
```

Maven resolves backend dependencies when a Maven command first runs.

## 4. Environment variables and configuration

Authoritative sources are `sami-backend/src/main/resources/application*.yml`,
`sami-backend/.env.example`, the Compose files, `sami-frontend/.env.example`
and `sami-frontend/vite.config.ts`.

### Core runtime variables

| Variable | Purpose | Requirement and local example | Production guidance |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Spring profile | Optional; defaults to `dev` | Set `prod` |
| `DB_URL` | JDBC URL for native/backend runtime | Optional; `jdbc:postgresql://localhost:5432/sami` | Compose supplies an internal `db` URL |
| `DB_USERNAME` | JDBC user | Optional; `sami` | Use a dedicated account |
| `DB_PASSWORD` | JDBC password | Optional development default | Required by production Compose |
| `DB_POOL_SIZE` | Hikari maximum pool size | Optional; `10` | Size for host/database capacity |
| `POSTGRES_DB` | Compose database name | Optional; `sami` | Confirm before first deployment |
| `POSTGRES_USER` | Compose database owner/user | Optional; `sami` | Use an approved name |
| `POSTGRES_PASSWORD` | Compose database password | Development example exists | Required by production Compose |
| `SERVER_PORT` | Backend HTTP port | Optional; `8080` | Backend remains internal in production Compose |
| `CORS_ALLOWED_ORIGINS` | Allowed browser origins | `http://localhost:7474` | Set the exact HTTPS application origin |

### Authentication and bootstrap

| Variable | Purpose | Requirement and local example | Production guidance |
|---|---|---|---|
| `JWT_SECRET` | Staff JWT HMAC secret | Development placeholder; minimum 32 bytes | Required, random and secret |
| `JWT_ISSUER` | Staff token issuer | Optional; `sami` | Keep stable per deployment |
| `JWT_ACCESS_TTL` | Access-token lifetime, seconds | Optional; `900` | Set through security policy |
| `JWT_REFRESH_TTL` | Refresh-token lifetime, seconds | Optional; `1209600` | Set through security policy |
| `PORTAL_JWT_SECRET` | Portal JWT secret | Development placeholder | Required, minimum 32 bytes and distinct from `JWT_SECRET` |
| `PORTAL_JWT_ISSUER` | Portal issuer | Optional; `sami-portal` | Keep stable |
| `BOOTSTRAP_ADMIN_ENABLED` | Create admin when users table is empty | Defaults to `true` | Disable after controlled bootstrap where appropriate |
| `BOOTSTRAP_ADMIN_EMAIL` | Initial admin email | Development example | Set an approved account |
| `BOOTSTRAP_ADMIN_PASSWORD` | Initial admin password | Development placeholder | Required by production Compose; rotate after bootstrap |
| `BOOTSTRAP_ADMIN_NAME` | Initial admin display name | `Administrator` | Operator choice |

Additional portal duration, failed-attempt and OTP settings are declared under
`app.portal` in `application.yml`.

### Frontend

| Variable | Purpose | Local example | Production guidance |
|---|---|---|---|
| `VITE_API_BASE_URL` | API base baked into the SPA | `/api` | `/api` for bundled nginx |
| `VITE_DEV_API_TARGET` | Vite proxy destination | `http://localhost:8080` | Development only |
| `VITE_ENABLE_MOCK_MODE` | Enable MSW mock mode | Unset/false | Do not enable in production |
| `FRONTEND_PORT` | Host port in production Compose | `80` | Choose an allowed host port |

### Storage, demo and scheduler

Confirmed variables include `STORAGE_PROVIDER`, `STORAGE_BASE_PATH`,
`STORAGE_MAX_IMAGE_BYTES`, `STORAGE_ALLOWED_IMAGE_TYPES`, `FILES_BASE_PATH`,
`FILES_STAGING_PATH`, `FILES_MAX_UPLOAD_BYTES`, `FILES_QUOTAS_ENFORCED`,
`DEMO_SEED_ENABLED`, `DEMO_SEED_SIZE`, `DEMO_SEED_VALUE`,
`SCHEDULER_ENABLED`, `SCHEDULER_POLL_SECONDS`, `SCHEDULER_BATCH_SIZE` and
`SCHEDULER_STARTUP_DELAY`.

Production Compose persists legacy uploads, managed files and managed-file
staging in separate named volumes.

No Keycloak configuration or implemented external email/SMS provider
credentials exist.

## 5. Running locally without Docker

### Start PostgreSQL

Start an operator-managed PostgreSQL instance. A repository-specific native
database creation script is not provided. One possible operator example is:

```sql
CREATE USER sami WITH PASSWORD '<db-password>';
CREATE DATABASE sami OWNER sami;
```

Use an approved database administrator and do not reuse development credentials
outside local development.

### Start the backend

PowerShell:

```powershell
Set-Location sami-backend
$env:DB_URL='jdbc:postgresql://localhost:5432/sami'
$env:DB_USERNAME='sami'
$env:DB_PASSWORD='<db-password>'
$env:JWT_SECRET='<staff-secret-at-least-32-bytes>'
$env:PORTAL_JWT_SECRET='<distinct-portal-secret-at-least-32-bytes>'
mvn spring-boot:run
```

Linux/macOS:

```bash
cd sami-backend
export DB_URL='jdbc:postgresql://localhost:5432/sami'
export DB_USERNAME='sami'
export DB_PASSWORD='<db-password>'
export JWT_SECRET='<staff-secret-at-least-32-bytes>'
export PORTAL_JWT_SECRET='<distinct-portal-secret-at-least-32-bytes>'
mvn spring-boot:run
```

Flyway runs automatically at startup, followed by Hibernate schema validation.

### Start the frontend

In another terminal:

```bash
cd sami-frontend
npm ci
npm run dev
```

Open:

- Application: `http://localhost:7474`
- Backend health: `http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

The development bootstrap defaults create `admin@sami.local` only when enabled
and the users table is empty. The tracked password is a development placeholder;
prefer overriding it even locally.

Stop Maven and Vite with `Ctrl+C`. Stop PostgreSQL using the mechanism used to
start the operator-managed service.

## 6. Running locally with Docker

### Backend and database

From `sami-backend`:

```bash
docker compose up --build
```

This starts:

- PostgreSQL on host port `5432`, persisted in `db-data`
- Backend on host port `8080`
- Legacy uploads persisted in `uploads`

Run the frontend separately with `npm run dev`.

Useful commands:

```bash
docker compose ps
docker compose logs -f backend
docker compose logs -f db
docker compose up --build -d
docker compose down
```

`docker compose down` removes containers and the network but preserves named
volumes.

> **Destructive: deletes local Compose database and upload volumes**
>
> ```bash
> docker compose down --volumes
> ```
>
> Confirm the working directory and Compose project before running it.

### Full production-style stack locally

From `sami-backend`, after setting required secrets in `.env`:

```bash
docker compose -f docker-compose.prod.yml up --build -d
```

Open `http://localhost` when `FRONTEND_PORT` is left at `80`. PostgreSQL and the
backend are not host-published in this topology.

## 7. Verification checklist

- `docker compose ps` or the native PostgreSQL tool reports the database ready.
- Backend startup logs show Flyway completion and no Hibernate validation error.
- `http://localhost:8080/actuator/health` returns healthy in native/development
  Compose mode.
- `http://localhost:8080/swagger-ui.html` opens when accessing the backend
  directly.
- `http://localhost:7474` loads in Vite mode.
- `http://localhost:<FRONTEND_PORT>` loads in production Compose mode.
- Browser network requests to `/api/v1/...` reach the backend.
- Authentication works with the configured bootstrap account.
- `/assets/...` resources load successfully.
- Expected ports are listening: `7474` and `8080` for native development;
  `5432` additionally for development Compose; only `FRONTEND_PORT` for
  production Compose.

## 8. Production build

Backend verification and artifact:

```bash
cd sami-backend
mvn clean verify
```

The executable JAR is produced under `sami-backend/target/`.

Frontend verification and artifact:

```bash
cd sami-frontend
npm ci
npm run type-check
npm run build
```

The static bundle is produced under `sami-frontend/dist/`. `npm run build`
already runs `vue-tsc --noEmit`. No lint or automated frontend test script is
configured.

Docker images:

```bash
cd sami-backend
docker build -t sami-backend:<version> .

cd ../sami-frontend
docker build --build-arg VITE_API_BASE_URL=/api -t sami-frontend:<version> .
```

The backend Dockerfile packages with tests skipped, so run `mvn clean verify`
separately before building a release image.

Production Compose build:

```bash
cd sami-backend
docker compose -f docker-compose.prod.yml build
```

## 9. Server/host deployment

The repository-supported production flow is Docker Compose:

1. Provision a host with Docker Engine, Compose v2, persistent disk, DNS,
   firewall policy and an operator-managed TLS solution.
2. Clone or transfer an approved revision into a stable directory.
3. Change to `sami-backend`.
4. Copy `.env.example` to `.env`.
5. Set strong `POSTGRES_PASSWORD`, `JWT_SECRET`, `PORTAL_JWT_SECRET` and
   `BOOTSTRAP_ADMIN_PASSWORD`, plus the deployed CORS origin.
6. Back up any existing database.
7. Run verification/build gates before release.
8. Start:

   ```bash
   docker compose -f docker-compose.prod.yml up --build -d
   ```

9. Verify:

   ```bash
   docker compose -f docker-compose.prod.yml ps
   docker compose -f docker-compose.prod.yml logs --tail=200 backend
   docker compose -f docker-compose.prod.yml logs --tail=200 frontend
   ```

10. Verify the application through its deployed origin.

Services use `restart: unless-stopped`, so Docker restarts them after daemon or
host restart when Docker itself starts.

The repository does not define external TLS termination, DNS automation,
firewall commands, cloud infrastructure or a host service unit. Configure
those through the operator's approved platform. Do not expose PostgreSQL or the
backend directly in the production Compose topology.

## 10. Reverse proxy and HTTPS

`sami-frontend/nginx.conf`:

- serves the SPA from `/usr/share/nginx/html`;
- falls back to `index.html` for history-mode routes;
- caches fingerprinted `/assets/` for one year;
- proxies `/api/` to `http://backend:8080/api/`;
- forwards `Host`, `X-Real-IP`, `X-Forwarded-For` and
  `X-Forwarded-Proto`.

No WebSocket configuration is present. No nginx upload limit is explicitly
configured; backend multipart limits default to 5 MB per file and 6 MB per
request, while the managed-file module has separate configuration.

The bundled nginx listens on HTTP port 80 and does not configure certificates.
HTTPS, certificate renewal, domain routing and any outer reverse proxy are
unresolved operator responsibilities. Set `CORS_ALLOWED_ORIGINS` to the exact
public HTTPS origin.

## 11. Database migrations and production safety

- Flyway is enabled and runs automatically during backend startup.
- Migrations are in `sami-backend/src/main/resources/db/migration`.
- Names follow `V<version>__<description>.sql`.
- Hibernate uses `ddl-auto: validate`.
- Applied migrations must never be edited.
- Review startup logs and Flyway's `flyway_schema_history` table to inspect
  status.
- Back up before deploying a migration.
- No Flyway rollback scripts or automated database rollback are present.
- Prefer backward-compatible forward migrations. Application rollback is safe
  only when the older application remains compatible with the migrated schema.

## 12. Backup and restore

No repository backup or restore scripts exist. The following are operator
examples, not project automation.

Backup a production Compose database:

```bash
cd sami-backend
docker compose -f docker-compose.prod.yml exec -T db \
  pg_dump -U <db-user> -d <db-name> -Fc > sami-backup.dump
```

Restore into a prepared database:

```bash
cd sami-backend
docker compose -f docker-compose.prod.yml exec -T db \
  pg_restore -U <db-user> -d <db-name> --clean --if-exists < sami-backup.dump
```

> **Destructive:** `pg_restore --clean` removes existing database objects.
> Stop application writes, confirm the target database, secure the backup,
> ensure PostgreSQL tool compatibility and test restoration in a non-production
> environment first.

Back up persistent uploaded files separately. The repository does not define a
file-backup or retention process.

## 13. Updating an existing deployment

1. Identify the approved revision and read configuration/migration changes.
2. Back up PostgreSQL and persistent uploads; test recovery where required.
3. Fetch or transfer the approved release.
4. Update `.env` for newly required variables without replacing secrets.
5. Run backend and frontend verification gates.
6. From `sami-backend`, build and start:

   ```bash
   docker compose -f docker-compose.prod.yml up --build -d
   ```

7. Flyway runs during backend startup.
8. Check service status, logs, application health, authentication and a critical
   read workflow.
9. Monitor failed jobs, communication backlog, storage and database health.

## 14. Rollback

- **Application/container:** deploy the previously approved source revision or
  image only if it is compatible with the current database.
- **Frontend:** rebuild/redeploy the previous static bundle or image.
- **Database:** no automatic rollback exists. Restore a verified backup or
  apply an approved forward correction.

Application rollback may be unsafe after an irreversible migration. Decide the
database recovery path before deploying schema changes.

## 15. Common problems

| Symptom | Repository-relevant checks |
|---|---|
| Database connection refused | Verify PostgreSQL health, `DB_URL`, host/port, database and credentials |
| Flyway validation failure | Do not edit applied migrations; compare the migration files and `flyway_schema_history` |
| Hibernate schema validation fails | Confirm all migrations completed against the intended database |
| Port already in use | Check `5432`, `7474`, `8080` or `FRONTEND_PORT`; stop the conflicting local service |
| Frontend cannot reach API | Verify `VITE_API_BASE_URL`, `VITE_DEV_API_TARGET`, backend health and `/api` proxy |
| CORS rejection | Set `CORS_ALLOWED_ORIGINS` to the exact browser origin, including scheme and port |
| JWT startup/authentication failure | Use secrets of at least 32 bytes; keep portal and staff secrets distinct |
| Compose service unhealthy | Run `docker compose ps` and inspect database/backend/frontend logs |
| Stale frontend | Rebuild after changing any `VITE_*` value because it is baked into the bundle |
| SPA route returns 404 | Use the bundled nginx configuration with history fallback |
| `/api` proxy returns 502 | Confirm the `backend` service is healthy on the Compose network |
| Missing production variable | Inspect Compose interpolation errors and `.env`; production has required secrets |
| Maven dependency failure | Confirm supported JDK/Maven versions and network access; rerun without changing dependencies |

Keycloak troubleshooting is not applicable because Keycloak is not configured.

## 16. Security checklist

- Replace every development password and token secret.
- Keep staff and portal JWT secrets strong and distinct.
- Never commit `.env`, secrets, backups or production identifiers.
- Keep PostgreSQL and backend internal in production Compose.
- Expose only the required frontend/TLS ports.
- Terminate HTTPS using an approved operator-managed solution.
- Configure CORS to the exact trusted origin.
- Review public Swagger/OpenAPI exposure before production.
- Protect logs and backups from credentials, tokens, OTPs and personal data.
- Apply restrictive host and volume permissions.
- Disable or tightly control bootstrap/demo initializers after provisioning.
- Verify permission and tenant behavior before release; tenant isolation is
  currently transitional.
- Define persistence and backup for both legacy uploads and managed files.

## 17. Command reference

```bash
# Backend run
cd sami-backend
mvn spring-boot:run

# Backend verification
mvn clean verify

# Frontend run
cd sami-frontend
npm ci
npm run dev

# Frontend verification/build
npm run type-check
npm run build

# Local PostgreSQL + backend
cd sami-backend
docker compose up --build -d
docker compose ps
docker compose logs -f
docker compose restart backend
docker compose down

# Production full stack
docker compose -f docker-compose.prod.yml up --build -d
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f
docker compose -f docker-compose.prod.yml restart
docker compose -f docker-compose.prod.yml down

# Operator backup example
docker compose -f docker-compose.prod.yml exec -T db \
  pg_dump -U <db-user> -d <db-name> -Fc > sami-backup.dump
```

See [`docs/17-deployment-and-operations.md`](docs/17-deployment-and-operations.md)
and [`docs/runbooks/README.md`](docs/runbooks/README.md) for related operational
context.
