# Contribution guide

1. Read `AGENTS.md`, this documentation index, and the owning module.
2. Record Git status and preserve unrelated changes.
3. Confirm the bounded-context owner and inspect a comparable implementation.
4. Keep controller/DTO/service/repository responsibilities separate.
5. Enforce RBAC, tenant scope, validation and audit on the backend.
6. Create only forward Flyway migrations; never edit applied history.
7. Synchronize backend DTOs, TypeScript types, API clients and mocks.
8. Add English and Persian localization together; verify RTL and LTR.
9. Add focused tests and run relevant build gates.
10. Update the smallest owning documentation page.

## Naming and contracts

Use domain terminology already present in code. Permission codes follow
`resource:action`. API endpoints are versioned under `/api/v1`. Prefer explicit
request/response DTOs and stable event contracts. Do not hardcode configurable
statuses, providers, workflows, menu items, or business rules.

## Pull-request checklist

- Scope contains no unrelated formatting or generated files.
- API/business behavior and migration impact are described.
- Authorization and tenant isolation are tested.
- Fresh PostgreSQL migration path is tested where schema changes.
- Frontend type-check/build and backend verification pass.
- Both locales and responsive RTL/LTR behavior are checked.
- Documentation, risk register and ADRs are updated when applicable.
- No credentials, tokens or private data are present.
