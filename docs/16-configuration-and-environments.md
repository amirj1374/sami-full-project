# Configuration and environments

Spring configuration precedence follows command-line/system/environment
overrides, profile files, then `application.yml`. The frontend's `VITE_*`
variables are build-time values.

## Environments and ports

| Item | Development default | Production topology |
|---|---|---|
| Frontend | Vite `7474` | nginx, externally configurable port |
| Backend | `8080` | backend service `8080`, proxied by nginx |
| API base | `/api/v1` server; frontend base `/api` | same-origin nginx proxy |
| PostgreSQL | localhost `5432` | internal Compose network |
| CORS | `http://localhost:7474` | configured deployment origin |

## Variable groups

- Database: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, Compose PostgreSQL variables.
- Staff JWT: `JWT_SECRET`, issuer and token TTLs.
- Portal JWT: `PORTAL_JWT_SECRET`; production requires a distinct value.
- Bootstrap admin: enable flag, email, password and display name.
- CORS: `CORS_ALLOWED_ORIGINS`.
- Legacy storage: `STORAGE_*`.
- Managed files: `FILES_*`.
- Frontend: `VITE_API_BASE_URL`, `VITE_DEV_API_TARGET`,
  `VITE_ENABLE_MOCK_MODE`.
- Demo data: `DEMO_SEED_ENABLED`, false by base default and true by dev-profile default.
- Scheduler: enable/polling settings.

Use `.env.example` only as a variable catalogue. It contains placeholders, not
production values. Production Compose fails fast for its required secrets.
Direct non-Compose production startup safeguards require further review.
