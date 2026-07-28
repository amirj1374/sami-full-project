# Testing and quality

## Current evidence

Backend tests are concentrated in pure unit tests for authorization, JWT,
dashboard KPIs, files, knowledge rules, profile validation, calendars and
scheduling arithmetic. A deployment-configuration contract test is present in
the current worktree. No Testcontainers/PostgreSQL integration suite was found.

The frontend defines `type-check` and `build`, but no lint, unit, component, or
end-to-end test script.

CI runs Maven verification and frontend install/type-check/build for pushes and
pull requests targeting `development`.

## Required quality model

- Pure domain/unit tests for rules and transitions.
- Spring slice tests for validation, security and serialization.
- PostgreSQL Testcontainers tests for Flyway, constraints and repositories.
- API contract tests aligned with TypeScript clients.
- Vue component tests for forms/tables/permissions.
- Browser E2E tests for login, refresh and critical workflows.
- Property-based tests for date, money, scheduling and pricing algorithms when added.

## Definition of Done

A change is not done until relevant permissions, tenant isolation, migrations,
backend tests, TypeScript contracts, localization, RTL/LTR, documentation and
operational behavior are verified. Report any command not run; never translate
static inspection into a passing-test claim.
