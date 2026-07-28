# SAMI architecture audit checklist

## Repository evidence

- Roots, branches and worktree state
- Guidance and documentation
- Build tools and declared runtime versions
- Configuration, environments, containers and deployment
- Test organization and lifecycle metadata
- Checks attempted, passed, failed or unavailable

## Backend

- Module/package dependency direction
- Domain ownership and persistence boundaries
- API, DTO, validation, error and transaction patterns
- Migrations, constraints, indexes and data seeding
- Authentication, RBAC, scope enforcement and sensitive data
- Audit, events, scheduling and observability
- Public services, integrations, files, reports and exports
- Duplicate owners, direct coupling and dependency cycles

## Frontend

- Routes, layouts and navigation
- API envelope/client and type conventions
- State, composables, components and form validation
- Permission presentation and forbidden handling
- Localization, Persian parity, RTL and responsiveness
- Loading, empty, failure and concurrency behavior
- Route/client/type alignment with backend endpoints

## Reconciliation

- Entity fields versus table columns and constraints
- Repository queries versus schema and indexes
- DTOs/types versus API request and response contracts
- Permissions versus backend enforcement and UI visibility
- Event publishers versus consumers
- Lifecycle claims versus executable implementation and tests
- Orphan migrations, tables, routes, endpoints and placeholders

## Proposed work

- Business owner and affected modules
- Reusable patterns with file evidence
- Required entities, contracts, permissions and events
- Configurable rules and extension points
- Edge cases, failure modes and integration risks
- Minimal architecture-compatible sequence
- Build, migration, test and startup verification plan
- Status classification and supporting evidence
- Dependency graph and highest fan-in blockers
