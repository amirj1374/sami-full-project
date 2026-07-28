# SAMI module implementation checklist

Use this checklist as a requirement trace. Mark an item `not applicable` only with a reason.

## Discovery and scope

- Applicable `AGENTS.md` read
- All affected branches are `development`
- Worktree changes identified and preserved
- Comparable module inspected end to end
- Reusable shared infrastructure identified
- Backend/frontend scope confirmed from requirements

## Domain and persistence

- Ownership and module boundary defined
- Entities, relationships, invariants and lifecycle mapped
- Configurable concepts persisted through existing mechanisms
- Concurrency, uniqueness, archival and deletion behavior handled
- Next Flyway version created without altering history
- Constraints and indexes match query and integrity needs

## Contracts and cross-cutting concerns

- DTOs and validation follow existing conventions
- Public services/queries prevent repository leakage
- Events are versionable, minimal and transactionally appropriate
- Permissions enforced server-side and surfaced consistently in UI
- Audit captures actor, time and meaningful before/after values
- Sensitive data, files, exports and logs use centralized controls

## User experience

- API client, types and validation schemas align with backend
- Loading, empty, error, forbidden and concurrency states covered
- Persian and English translations added together
- RTL and responsive layout verified
- Import/export/report flows include error handling

## Evidence

- Focused tests pass
- Full backend build/tests pass
- Migration and startup verification pass
- Frontend type-check/tests/build pass
- Container configuration passes when present
- Lifecycle metadata matches verified reality
