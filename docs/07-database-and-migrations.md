# Database and Flyway

Flyway is the exclusive schema owner. Migrations are under
`sami-backend/src/main/resources/db/migration`; Hibernate runs with
`ddl-auto: validate`.

## Current sequence

The current chain is V1 through V27:

- V1–V5: users, products, RBAC and user management.
- V6–V9: CRM, purchasing and suppliers.
- V10–V15: dashboard, automation, licensing, data quality and metadata.
- V16–V18: tenancy defaults and managed files.
- V19–V24: appointments, knowledge, portal, organization, calendar and scheduler.
- V25–V27: module lifecycle, organization correction and communication hub.

V26 is the organization lifecycle correction; communication is V27.

## Conventions and safety

- Never edit a migration that may have been applied.
- Add the next unique version and make forward-only corrections.
- Align PostgreSQL types, defaults, constraints, indexes and JPA mappings.
- Use foreign keys and uniqueness constraints for invariants that must survive
  concurrency.
- Base entities commonly carry IDs and audit timestamps; several aggregates
  use optimistic `@Version`.
- Tenant-bearing tables must include tenant scope in uniqueness and lookup
  rules unless a record is explicitly global.
- Seed reference data through deterministic, idempotent migration SQL.

## Current gaps

No automated PostgreSQL/Flyway integration suite was found. Unit tests do not
prove migrations apply on PostgreSQL 16 or that entity mappings validate
against a fresh schema. Backup/restore automation is not present in the repo.
