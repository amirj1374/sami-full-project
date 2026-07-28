# ERM implementation continuation — 2026-07-29

## Current state

- ERM discovery and the phase-one plan are complete.
- No ERM schema or business-module source has been created yet.
- The reusable request-scoped `TenantContext` foundation is implemented and
  derives tenant authority from the authenticated, database-loaded principal.
- Licensing response hardening removes raw license keys from normal API DTOs.
- Permanent AI workflow and project setup guidance are version controlled.

## Decisions already made

- ERM owns business risks, assessments, appetite, controls, compliance,
  governance incidents, mitigations and indicators; it does not duplicate
  Security Policy, Audit or Business Continuity.
- New tenant-scoped code must use `TenantContext`, fail closed and never use
  `TenantDefaults` or client-provided tenant IDs as authority.
- JWT and public authentication payloads remain unchanged; tenant ownership is
  loaded from the user record on every authenticated request.
- Platform roles have no implicit cross-tenant bypass.
- Historical assessments must reference immutable/versioned scoring
  configuration.
- Implementation proceeds as complete vertical slices, starting with the risk
  register and assessment foundation.

## Next tasks

1. Re-check `development`, worktree state and migration head.
2. Design and add the next Flyway migration for configurable risk taxonomy,
   statuses, methodologies, scales, appetite, risks, assessments, permissions
   and lifecycle registration.
3. Implement tenant-scoped entities, repositories, specifications, services,
   scoring/appetite evaluation, audit and events.
4. Add REST DTOs/controllers and focused tenant, security, concurrency and
   historical-reproducibility tests.
5. Add the typed frontend API, models, schemas, risk register/detail/assessment
   UI, permissions and English/Persian localization.
6. Run backend, migration, frontend and contract validation available in the
   next environment and report the slice before proceeding to controls.

## Assumptions

- Existing `users.tenant_id` and `roles.is_platform` columns remain authoritative.
- ERM codes and uniqueness are tenant-scoped.
- Company and branch references must belong to the trusted tenant.
- Cross-cutting workflow, durable outbox and generalized approval
  infrastructure remain outside the first ERM slice.
- The next migration version is determined again from current repository state;
  it was `V28` at this checkpoint.
