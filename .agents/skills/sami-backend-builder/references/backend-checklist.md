# SAMI backend checklist

## Evidence

- Applicable guidance read
- Branch and worktree verified
- Java/build configuration verified
- Comparable module inspected
- Shared owners and reusable patterns cited

## Domain and API

- Module ownership and dependencies are valid
- Entities, DTOs and mappings align
- Jakarta request validation is complete
- State-dependent invariants live in services/domain
- Controllers remain thin
- API envelope, errors, pagination and OpenAPI match conventions

## Persistence

- Next Flyway version used without editing history
- Schema, entities and queries reconcile
- Concurrent invariants have database constraints
- Indexes correspond to actual query patterns
- Transaction and locking choices are justified
- Tenant/company/branch scope applies to reads, writes and uniqueness

## Cross-cutting

- Permissions are enforced server-side
- Sensitive actions and changes are audited
- Events use shared infrastructure and correct transaction timing
- External providers are accessed through established ports
- Files, reports and imports/exports use shared infrastructure
- Sensitive data is minimized in APIs, logs, events and exports

## Verification

- Unit tests cover business rules and edge cases
- Repository/integration tests cover schema and scoping
- API/security tests cover contracts and permissions
- `mvn clean verify` passes
- Application startup passes
- Flyway validation passes
- Failures and unavailable checks are reported honestly
