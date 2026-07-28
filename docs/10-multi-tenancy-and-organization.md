# Multi-tenancy and organization

## Current model

Tenant is the intended isolation root. Company, branch, and store represent
organizational and operational subdivisions beneath that root. Their exact
business permissions and cross-company sharing rules remain open decisions.

`common/tenancy/TenantContext` is the trusted request authority for new
tenant-scoped code. It derives ownership exclusively from the authenticated,
database-backed `SecurityUser`; client-supplied tenant identifiers are never an
authority source. It fails closed when authentication or tenant ownership is
missing and does not give platform roles an implicit cross-tenant bypass.

`common/tenancy/TenantDefaults` remains only as a transitional bridge for older
modules. Do not use it in new code. Repository scoping is not yet uniformly
enforced across those older modules.

## Rules for current work

- Treat tenant IDs supplied by clients as untrusted.
- Resolve scope server-side and include it in repository reads and writes.
- Include tenant scope in uniqueness constraints where data is tenant-owned.
- Never infer company/branch/store access from record existence.
- Cross-tenant administration must be explicit, privileged, and audited.
- Do not copy the default-tenant bridge into a new module as final design.

## Remaining enforcement work

New tenant-owned modules must take their tenant from `TenantContext` for reads,
writes, uniqueness, audit and events. Existing modules still require incremental
repository enforcement. Background jobs must obtain tenant scope from trusted
persisted work and must never silently use a default tenant.
