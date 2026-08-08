# Configuration and environments

This document owns environment topology and configuration-variable groups.
Exact startup commands belong to `PROJECT_SETUP_AND_DEPLOYMENT.md`; a fresh
Windows machine checklist belongs to `docs/NEW_WORKSTATION_SETUP.md`.

Spring configuration precedence follows command-line/system/environment
overrides, profile files, then `application.yml`. Frontend `VITE_*` variables
are build-time values.

## Environments and ports

| Item | Development default | Production topology |
|---|---|---|
| Frontend | Vite `7474` | nginx, externally configurable port |
| Backend | `8080` | backend service `8080`, proxied by nginx |
| API base | `/api/v1` server; frontend base `/api` | same-origin nginx proxy |
| PostgreSQL | localhost `5432` | PostgreSQL 16 on internal Compose network |
| CORS | `http://localhost:7474` | exact configured deployment origin |

## Canonical variable groups

- Database: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` and Compose PostgreSQL variables.
- Staff JWT: `JWT_SECRET`, issuer and token TTLs.
- Portal JWT: `PORTAL_JWT_SECRET`; production requires a distinct value.
- Bootstrap administrator: enable flag, email, password and display name.
- CORS: `CORS_ALLOWED_ORIGINS`.
- Legacy storage: `STORAGE_*`.
- Managed files: `FILES_*`.
- Frontend: `VITE_API_BASE_URL`, `VITE_DEV_API_TARGET`, `VITE_ENABLE_MOCK_MODE`.
- Demo data: `DEMO_SEED_ENABLED`, false in base configuration and true by the dev profile unless overridden.
- Scheduler: enable, polling, batch and startup-delay settings.
- Demo inbox notifications: `DEMO_HOURLY_NOTIFICATION_ENABLED`; disabled by default and also user opt-in.

Use `sami-backend/.env.example` and `sami-frontend/.env.example` only as
variable catalogues. Local `.env` files and release configuration are ignored;
never commit real credentials. Production Compose fails fast for required
database, staff JWT, portal JWT and bootstrap secrets.
