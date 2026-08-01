# Deployment and operations

## Current topology

`docker-compose.prod.yml` defines three services and supports both local builds
and explicitly tagged prebuilt backend/frontend images:

1. PostgreSQL 16 with a persistent database volume and health check.
2. Spring Boot built with Maven/JDK 21 and run as a non-root Java 21 process.
3. Vue assets built with Node 22 and served by nginx 1.27.

nginx performs SPA history fallback and proxies `/api/` to the backend service.
The database is exposed only to the internal Compose network.

## Release checklist

1. Confirm clean/understood Git diff and reviewed migrations.
2. Supply strong required secrets from an approved secret store.
3. Run `mvn clean verify`, frontend type-check/build, and PostgreSQL migration tests.
4. Back up the target database and verify restore procedure.
5. Deploy application compatible with forward migration.
6. Check frontend root, `/actuator/health`, authentication and a critical read.
7. Monitor logs, database connections, failed jobs and communication backlog.

## Windows release automation

[`scripts/deploy.ps1`](../scripts/deploy.ps1) is the repository owner for
building `linux/amd64` images, verifying revision metadata, atomically exporting
and uploading TARs, checking SHA256 values, recreating only application
services, and rolling back to preserved application image IDs. It uses
non-interactive OpenSSH key authentication by default and never reads or copies
the server `.env`.

Use [`DEPLOYMENT_AUTOMATION_GUIDE.md`](../DEPLOYMENT_AUTOMATION_GUIDE.md) for
configuration, SSH key setup, dry-run, full deployment, rollback, cleanup, and
troubleshooting commands.

Flyway rollback scripts are not provided. Prefer backward-compatible forward
migrations and application rollback that remains compatible with the migrated
schema.

## Operational gaps

No repository-defined cloud infrastructure-as-code, automated database
backup/restore, disaster-recovery objective, metrics stack, alert policy, or
general event-delivery monitor was found. Application-image deployment and
rollback are automated, but database rollback and restore remain separate
controlled operations.
