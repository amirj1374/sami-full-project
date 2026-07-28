---
id: MED-001
title: Add frontend lint tests E2E and API contract gates
status: needs-decision
priority: medium
type: test
area: quality
owners: []
depends_on: [HIGH-001]
blocks: [MED-002, MED-003]
estimated_size: L
risk: medium
source_refs: [sami-frontend/package.json, sami-frontend/src/api, sami-frontend/src/types, .github/workflows/ci.yml]
---

# Add frontend lint tests E2E and API contract gates

## Summary
Introduce lint, unit/component, browser E2E and backend/frontend contract validation.

## Why this is needed
Frontend currently has only type-check/build; client/server drift and behavior
regressions are not automatically detected.

## Current evidence
No ESLint/Vitest/Playwright/Cypress script/config exists. CI runs npm install,
type-check and build.

## Existing implementation
Strict TypeScript, typed API clients, schemas, MSW and backend unit tests.

## Missing work and scope
Tool selection/config, representative tests, OpenAPI/DTO contract strategy,
coverage policy, test data and CI gates.

## Dependencies
Database test foundation for full API tests.

## Business decisions required
Supported browsers and acceptable CI duration/coverage thresholds.

## Proposed implementation approach
Add lint first, then unit/component tests, contract checks and a small critical E2E suite.

## Impacts
Frontend dev dependencies/config/tests and CI; backend contract fixtures may be needed.

## Security and tenant-isolation considerations
Include permission-denial, refresh and cross-scope tests without real credentials.

## Testing requirements
Self-test the gates with intentional failures and deterministic fixtures.

## Documentation requirements
Commands, conventions and test-data lifecycle.

## Acceptance criteria
- [ ] Lint/type/build/test commands are reproducible locally and in CI.
- [ ] Login/refresh and one core workflow have E2E coverage.
- [ ] API client drift is detected automatically.
- [ ] Test fixtures contain no secrets or shared data.

## Validation commands
New npm lint/test/E2E commands and `mvn clean verify`.

## Risks and rollback considerations
Avoid flaky gates; quarantine only with owner and expiry.

## Relevant files
Frontend package/config/source, backend OpenAPI/DTOs and CI.

## Notes for the next developer or AI agent
Dependency additions require explicit implementation authorization.
