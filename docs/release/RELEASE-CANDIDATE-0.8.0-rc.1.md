# SAMI ERP 0.8.0-rc.1

Release type: Release Candidate / Preview

Candidate commit: `0bdb47d` (`development`)

## Included

- Purchasing and receiving foundations
- Inventory variants, UOM provenance, reservations/backorders and serial custody
- Accounting foundation, journal/GL, AR/AP, Treasury posting boundary and tax policy
- Existing Sales, Treasury, Purchasing, Inventory and Accounting workflows
- CRM workflow/reminders and advisory intelligence preview

## Preview limitations

CRM Intelligence remains preview scope. Authenticated desktop Persian/RTL UI
inspection passed; exact tablet/mobile viewport evidence and stable/attention
fixture states were unavailable in the current browser harness. Phase 5 is not
claimed as a roadmap-complete release gate.

## Runtime

- Java 21, Spring Boot 3.5.3, PostgreSQL 16, Node/Vite frontend as locked by repository manifests.
- Flyway schema through V79.
- Required production configuration: `POSTGRES_PASSWORD`, `JWT_SECRET`,
  `PORTAL_JWT_SECRET`, `BOOTSTRAP_ADMIN_PASSWORD`; set distinct strong values.

## Deployment

1. Back up PostgreSQL and verify restore before migration.
2. Prepare `.env` from `sami-backend/.env.example` with production secrets,
   `SPRING_PROFILES_ACTIVE=prod`, and exact CORS origin.
3. Build/load the backend and frontend images for commit `0bdb47d`.
4. Run `docker compose -f sami-backend/docker-compose.prod.yml up -d`.
5. Wait for database/backend health and verify the frontend through nginx.

## Rollback

Stop the stack, restore the pre-deployment database backup, redeploy the prior
image pair, and verify health/authentication. Do not reverse Flyway migrations
in place.

## Smoke checklist

- backend health and Flyway V79;
- staff login and tenant/company context;
- Purchasing, Inventory, Accounting and Treasury navigation;
- CRM preview route and permission enforcement;
- one read-only API request per included module;
- logs contain no startup migration or authentication errors.
