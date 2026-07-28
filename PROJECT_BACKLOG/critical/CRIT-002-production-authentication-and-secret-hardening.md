---
id: CRIT-002
title: Harden production authentication and secret contracts
status: needs-decision
priority: critical
type: security
area: security
owners: []
depends_on: [CRIT-001]
blocks: [MED-003, HIGH-014]
estimated_size: L
risk: critical
source_refs: [sami-backend/src/main/resources/application.yml, sami-backend/src/main/resources/application-prod.yml, sami-backend/src/main/java/com/sami/app/config/SecurityConfig.java, sami-frontend/src/api/tokenStorage.ts]
---

# Harden production authentication and secret contracts

## Summary
Define and enforce production secret, token storage, portal/staff boundary,
public endpoint, CORS/CSRF, rate-limit and bootstrap-admin policies.

## Why this is needed
Base configuration has development fallbacks and browser tokens are readable by
JavaScript. Compose safeguards do not prove every deployment path is safe.

## Current evidence
`tokenStorage.ts` stores access/refresh tokens in `localStorage`; base config
contains development fallbacks; portal and staff JWT services are separate.

## Existing implementation
JWT authentication, database RBAC, password encoding, refresh flow and some
production Compose fail-fast secret checks exist.

## Missing work
Threat model, secret-store contract, non-Compose fail-fast validation,
cookie/CSRF decision, rate limiting, public-route audit and token revocation policy.

## Scope
In scope: authentication/security contract and focused remediation. Out of
scope: changing business permissions.

## Dependencies
Depends on the tenant-context decision and enforcement work in CRIT-001.

## Business decisions required
Cookie versus bearer storage, session/device policy, portal deployment, MFA,
password/reset policy and allowed production origins.

## Proposed implementation approach
Security ADR; configuration validation; public-route/permission inventory;
selected browser-token design; rate limits and security integration tests.

## Backend, frontend, database and API impact
Security filters/config and refresh API may change with matching frontend store
and interceptors; token/revocation persistence may require a new migration.

## Security and tenant-isolation considerations
Separate portal/staff secrets and audiences; rotate safely; redact logs; bind
scope to authenticated identity.

## Testing requirements
Authentication, refresh/replay, CSRF/CORS, public endpoint, rate limit,
revocation and cross-scope integration tests.

## Documentation requirements
Threat model, secret runbook and deployment checklist without secret values.

## Acceptance criteria
- [ ] Production startup fails safely when required secrets are absent.
- [ ] Public endpoints and permission coverage are reviewed.
- [ ] Browser token/CSRF design is approved and tested.
- [ ] Portal and staff tokens cannot cross authentication boundaries.
- [ ] Rotation and bootstrap-admin procedures are documented.

## Validation commands
`mvn clean verify`; frontend tests/build; deployment configuration checks;
repository secret scan.

## Risks and rollback considerations
Authentication changes can invalidate sessions; plan staged rollout and
explicit reauthentication.

## Relevant files
`security`, `auth`, `portal/security`, frontend auth store/API, application profiles.

## Notes for the next developer or AI agent
Never print actual secret values or silently trust Compose as the only runtime.
