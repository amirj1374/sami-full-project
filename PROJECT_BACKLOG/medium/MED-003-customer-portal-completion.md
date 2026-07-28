---
id: MED-003
title: Complete and deploy the Customer Portal
status: blocked
priority: medium
type: feature
area: portal
owners: []
depends_on: [CRIT-001, CRIT-002, HIGH-011, HIGH-013, MED-001]
blocks: []
estimated_size: XL
risk: high
source_refs: [sami-backend/src/main/java/com/sami/app/portal, sami-backend/src/main/resources/db/migration/V21__customer_portal.sql, sami-frontend/src]
---

# Complete and deploy the Customer Portal

## Summary
Complete public endpoints, secure authentication/OTP, customer data providers,
frontend experience, files and operational deployment.

## Why this is needed
Portal services/security/schema exist, but no verified controller and dedicated
frontend end-to-end path exists.

## Current evidence
Portal package has account/auth/dashboard/OTP services and SPIs; no portal
controller was found and OTP delivery implementations are absent.

## Existing implementation
Portal accounts, token service, audit and data-provider/OTP registries.

## Missing work and scope
Approved public API, identity linking, OTP provider, portal SPA/routes,
privacy/access rules, abuse controls and operational support.

## Dependencies
Tenant/security, file platform, communication providers and frontend tests.

## Business decisions required
Deployment domain/app, login methods, customer identity linking, exposed data
and self-service capabilities.

## Proposed implementation approach
Threat model and API first; then one read-only customer journey, authentication,
files and incremental self-service.

## Impacts
Portal backend/API/frontend/config/provider/deployment.

## Security and tenant-isolation considerations
Strict staff/portal token separation, object ownership, rate limits and privacy audit.

## Testing requirements
Account takeover, OTP abuse, IDOR/cross-customer, contract and browser E2E tests.

## Documentation requirements
Portal threat model, customer workflow and support runbook.

## Acceptance criteria
- [ ] Portal scope and identity model are approved.
- [ ] Public API is minimal and tenant/customer-safe.
- [ ] OTP/provider and frontend journey work in approved environment.
- [ ] Security and privacy tests pass.

## Validation commands
Backend security suite, frontend/E2E and sandbox provider tests.

## Risks and rollback considerations
Feature-flag launch and preserve staff/portal isolation.

## Relevant files
Portal package, V21, security config and future portal frontend.

## Notes for the next developer or AI agent
Remain blocked until all dependencies and public-product decisions close.
