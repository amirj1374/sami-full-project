# Operational runbooks

## Backend cannot connect to PostgreSQL

- **Symptoms:** startup failure, connection refused, authentication/database error.
- **Checks:** database health; `DB_URL` host/port/database; username variable;
  network/DNS; PostgreSQL logs. Never print the password.
- **Safe actions:** correct local environment, start local Compose, retry.
- **Escalate:** suspected production credential, network, storage or corruption issue.

## Flyway validation failure

- **Checks:** identify failed version, checksum mismatch, duplicate version and
  current application revision.
- **Safe actions:** fix an unapplied local migration or add a new corrective
  migration. Never edit applied history or run repair on shared data without approval.

## Frontend cannot reach backend / CORS

- Verify Vite `7474`, backend `8080`, `/api` base, proxy target and
  `CORS_ALLOWED_ORIGINS`. Check browser network response before changing code.

## Authentication or refresh failure

- Check status code, token expiry, user status, refresh endpoint and server
  clock. Clear local tokens only in the affected local browser. Never log tokens.

## File-storage failure

- Identify whether the consumer uses legacy `app.storage` or managed
  `app.files`; check configured path, volume, permissions, capacity and metadata
  ownership. Do not delete orphan candidates without reconciliation.

## Scheduled-job failure

- Inspect job definition, handler registration, execution history, lock state,
  timeout and logs. A timed-out handler may still be running; avoid blind retry
  when the handler is not idempotent.

## Production health-check failure

- Check container state, backend `/actuator/health`, database health and nginx
  upstream resolution. Do not expose secrets or mutate the database while
  diagnosing. Escalate when recovery would require rollback, restore or Flyway repair.
