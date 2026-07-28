# Multi-tenancy and organization

## Current model

Tenant is the intended isolation root. Company, branch, and store represent
organizational and operational subdivisions beneath that root. Their exact
business permissions and cross-company sharing rules remain open decisions.

`common/tenancy/TenantDefaults` supplies a tenant ID for code written before a
real request-bound `TenantContext` exists. Its own documentation calls it a
bridge. Many entities and writes carry `tenantId`, but repository scoping is
not uniformly enforced by one central mechanism.

## Rules for current work

- Treat tenant IDs supplied by clients as untrusted.
- Resolve scope server-side and include it in repository reads and writes.
- Include tenant scope in uniqueness constraints where data is tenant-owned.
- Never infer company/branch/store access from record existence.
- Cross-tenant administration must be explicit, privileged, and audited.
- Do not copy the default-tenant bridge into a new module as final design.

## Open architecture decision

A production model needs request authentication → tenant/company/branch
context resolution → mandatory repository filtering → write attribution →
background-job context propagation. Until that is implemented and integration
tested, isolation must be classified as transitional.
