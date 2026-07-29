# SAMI ERP Deployment Readiness Report

## Executive summary

**Current verdict: Not ready until the release-critical runtime checks run.**
The repository now has a coherent three-container public-IP test topology, and
the deployment blockers found in health ordering and managed-file persistence
were corrected. Docker, backend build, clean-database migration and live smoke
tests still require execution on a Docker-capable host before deployment.

## Detected stack

- Backend: Java 21, Spring Boot 3.5.3, Maven 3.9 build image, PostgreSQL driver,
  Flyway, Spring Security and JJWT 0.12.6.
- Frontend: Vue 3.5.40, TypeScript 5.7.3, Vite 8.1.5, Vuetify 3.12.11,
  Axios 1.18.1 and nginx 1.27.
- Infrastructure: PostgreSQL 16 Alpine, multi-stage backend/frontend images,
  Docker Compose and named volumes.

## Existing Docker support and changes

`sami-backend/docker-compose.prod.yml` already implemented PostgreSQL, backend
and frontend/nginx with only frontend port 80 published. This work added:

- backend image and Compose health readiness;
- nginx waiting for healthy backend;
- public `/health` proxy to the existing Actuator endpoint;
- persistent volumes for managed files and staging;
- an internet-test `.env` template;
- beginner deployment, command, readiness and server-information documents.

## Readiness by area

| Area | Result |
|---|---|
| Frontend type-check/build | Passed |
| Documentation validation | Passed |
| API base/public IP | Relative `/api`; suitable |
| nginx SPA fallback | Present |
| PostgreSQL privacy/persistence | Internal-only; named volume |
| Authentication | Internal staff JWT plus separate portal JWT |
| Bootstrap account | Environment-configured, first-run only |
| Migration range | V1–V27 found; runtime execution unverified here |
| Backend build/tests | Not run: Java/Maven unavailable |
| Compose config/build/up | Not run: Docker unavailable |
| Clean DB/Flyway/startup | Not run: Docker unavailable |
| Live browser/API smoke test | Requires deployment host |

## Remaining blockers and non-blocking issues

The only release blocker is unexecuted Docker/backend/clean-database validation.
Run the guide’s build/start/smoke sequence on the Linux server.

Non-blocking risks:

- HTTP provides no transport confidentiality; use test data only.
- npm reports four high-severity dependency advisories; no breaking dependency
  upgrade was attempted during deployment preparation.
- Test coverage is limited and no frontend test/lint scripts are configured.
- Generic Spring events are not durable.
- Older modules still have transitional tenant enforcement.

## Environment and ports

Required secrets: `POSTGRES_PASSWORD`, `JWT_SECRET`,
`PORTAL_JWT_SECRET`, `BOOTSTRAP_ADMIN_PASSWORD`.
Required deployment settings include `SPRING_PROFILES_ACTIVE=prod`,
`CORS_ALLOWED_ORIGINS=http://SERVER_PUBLIC_IP` and
`VITE_API_BASE_URL=/api`. Database/user/name, issuer, admin identity and
frontend port are configurable.

Public: TCP 80 and the server’s SSH port. Optional future TCP 443. PostgreSQL
5432 and backend 8080 remain internal to Compose.

## Public-IP and security recommendation

Use `http://SERVER_PUBLIC_IP`; health is
`http://SERVER_PUBLIC_IP/health`. Do not expose PostgreSQL or use real customer
data. Keep `.env` mode 600, use distinct random JWT secrets, replace bootstrap
credentials and add HTTPS before production.

## Final recommendation

Run the following release gate on the Linux server:

```bash
docker compose --env-file .env -f docker-compose.prod.yml config
docker compose --env-file .env -f docker-compose.prod.yml build
docker compose --env-file .env -f docker-compose.prod.yml up -d
docker compose -f docker-compose.prod.yml ps
curl -f http://localhost/health
```

Proceed with the test deployment only after backend tests, image builds, clean
Flyway migration, login/API and persistence smoke tests pass.
