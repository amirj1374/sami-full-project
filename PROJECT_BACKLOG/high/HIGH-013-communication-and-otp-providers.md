---
id: HIGH-013
title: Implement approved communication and OTP providers
status: needs-decision
priority: high
type: integration
area: communication
owners: []
depends_on: [CRIT-002, HIGH-010]
blocks: [MED-003]
estimated_size: L
risk: high
source_refs: [sami-backend/src/main/java/com/sami/app/comm/spi/CommProviderHandler.java, sami-backend/src/main/java/com/sami/app/portal/spi/OtpDeliveryChannel.java, sami-backend/src/main/resources/db/migration/V27__communication_hub.sql]
---

# Implement approved communication and OTP providers

## Summary
Add production email/SMS and required channel adapters, including reliable OTP delivery.

## Why this is needed
Communication and portal registries have zero provider implementations.

## Current evidence
Provider/OTP SPIs, routing, templates, attempts and durable message rows exist;
no executable external handler exists.

## Existing implementation
Communication hub backend and portal OTP service foundations.

## Missing work and scope
Provider selection, adapters, secrets, sandbox, callbacks, retries,
rate limits, delivery status, fallback and operational monitoring.

## Dependencies
Security/auth and durable-event decisions.

## Business decisions required
Providers/channels, sender identities, localization, SLA, cost limits and data residency.

## Proposed implementation approach
One provider per required channel behind existing SPIs; fake/local provider for
tests; webhook verification and idempotent status updates.

## Impacts
Backend providers/config, optional admin UI and production infrastructure.

## Security and tenant-isolation considerations
Protect credentials/OTP/PII; scoped templates/preferences; rate-limit OTP.

## Testing requirements
Provider contract, sandbox, webhook replay/signature, fallback and OTP abuse tests.

## Documentation requirements
Provider setup without secrets and delivery-failure runbook.

## Acceptance criteria
- [ ] Providers and SLA are approved.
- [ ] Adapters implement existing SPIs.
- [ ] OTP is rate-limited and never logged in plaintext.
- [ ] Retry/fallback/status callbacks are idempotent and observable.

## Validation commands
Provider contract tests and approved non-production sandbox tests.

## Risks and rollback considerations
Feature-flag providers and preserve queued messages during rollback.

## Relevant files
`comm`, `portal/spi`, `portal/service/PortalOtpService`, V27.

## Notes for the next developer or AI agent
Never invoke a real provider without explicit environment authorization.
