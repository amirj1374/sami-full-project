# Licensing product decisions

This document separates current licensing evidence from product choices that
must remain changeable. It is not an ADR: pending entries do not authorize
schema, API or behavior changes. Approved architectural decisions should be
recorded separately in `docs/adr`.

## A. Confirmed repository facts

- Licensing is owned by `com.sami.app.licensing`.
- Authentication and authorization are separate concerns.
- Business modules are expected to use `FeatureGate` or
  `EntitlementService`, not licensing repositories.
- Flyway V12 and V14 own the current licensing schema.
- Tenant and organization context is transitional; `CRIT-001` remains open.
- General Spring events are in-process rather than crash-durable.
- No dedicated licensing frontend or focused licensing test suite existed
  before the current security-hardening slice.

## B. Existing implemented behavior

The backend currently provides configurable license and tenant statuses,
license types, feature states, feature dependencies, subscription plans,
plan-feature bundles, plan limits, license-level feature overrides, usage
counters, billing cycles, payment statuses, expiry behavior handlers,
activation-provider SPIs, audit records, lifecycle events, scheduled expiry and
renewal, reports and CSV export.

`SubscriptionPlan` is the current commercial bundle. `License` belongs to a
tenant and may carry a company ID. `EntitlementService` selects a license,
computes features and caches the result. `FeatureGate` is the public
availability contract. Only a users usage meter is registered.

## C. Requested capabilities

- Edition management
- Hierarchical feature flags
- Configurable flag conflicts and dependencies
- Company, branch, department, role, user and module targeting
- Enterprise offline validation
- Additional usage meters and enforcement policies
- Search and combined filters
- JSON/XML import
- PDF, Excel, CSV and JSON export
- Additional compliance and consumption reports
- Responsive Persian/English administration UI
- Future marketplace and usage-billing readiness

Requested capabilities are not commitments until approved.

## D. Product decisions still required

Each entry uses these status values: `Pending`, `Approved`, `Rejected` or
`Deferred`. Product decision, date and approver remain blank until supplied by
an authorized decision-maker.

### LIC-PD-001 — Edition and subscription-plan relationship

- **Business question:** Are Edition and `SubscriptionPlan` the same concept,
  hierarchical concepts, or independent?
- **Current repository behavior:** A plan owns features, limits, duration,
  renewal policy and billing cycle. No Edition entity exists.
- **Options:**
  1. Treat plan as edition. Lowest implementation and migration cost.
  2. Edition contains plans. Supports multiple commercial terms per bundle but
     adds lifecycle and compatibility rules.
  3. Independent edition and plan axes. Most flexible, highest complexity and
     risk of contradictory assignments.
- **Database impact:** None for option 1; new relationships and constraints for
  options 2–3.
- **API/frontend impact:** Determines naming, license forms, comparisons and
  downgrade workflows.
- **Security/scope impact:** Edition administration remains permission-controlled;
  it must not grant authorization.
- **Migration/compatibility:** Existing plan codes and license references must
  remain valid.
- **Recommended technical default:** Option 1 unless distinct commercial
  behavior is approved.
- **Product Owner decision:**
- **Status:** Pending
- **Decision date:**
- **Approved by:**
- **Revisit conditions:** Multiple terms must share one stable feature/limit
  bundle, or customers require edition changes independent of billing plans.

### LIC-PD-002 — License ownership and organization scope

- **Business question:** May licenses apply to an installation, tenant, company
  or a combination, and how is the effective license selected?
- **Current repository behavior:** Tenant is required; company ID is optional.
  `findForTenant` ranks any company license before a tenant-wide license without
  receiving company context.
- **Options:**
  1. Tenant-only licenses. Simplest and safest, but no company-specific sales.
  2. Tenant plus explicit company licenses. Requires trusted company context.
  3. Installation, tenant and organization composition. Flexible but introduces
     combination and precedence rules.
- **Database impact:** Options 2–3 require scope constraints and indexed queries.
- **API/frontend impact:** Scope must be visible and selected only from allowed
  server-provided context.
- **Security/scope impact:** Ambiguous or foreign scope must fail closed.
- **Migration/compatibility:** Existing company IDs cannot be reinterpreted
  without a data audit.
- **Recommended technical default:** Option 2 after `CRIT-001`, with explicit
  trusted scope and no implicit company selection.
- **Product Owner decision:**
- **Status:** Pending
- **Decision date:**
- **Approved by:**
- **Revisit conditions:** Installation-wide or cross-company commercial
  licensing becomes a confirmed requirement.

### LIC-PD-003 — Feature-flag targets and precedence

- **Business question:** Which targets are supported, which scope wins, and may
  lower scopes enable a feature disabled above them?
- **Current repository behavior:** Plan bundle plus license override; tenant
  beta opt-ins in JSON. No general precedence engine.
- **Options:**
  1. Approved scopes with fixed precedence.
  2. Configurable precedence policy.
  3. Explicit resolved assignments with no general hierarchy.
- **Benefits/risks:** Fixed precedence is predictable but less adaptable;
  configurable precedence is flexible but harder to validate and explain;
  explicit assignments minimize hidden rules but require more administration.
- **Database impact:** Target representation, uniqueness, effective-date and
  conflict constraints depend on the option.
- **API/frontend impact:** Requires an explainable effective-state response and
  conflict feedback.
- **Security/scope impact:** Flags never grant permissions; all target IDs come
  from trusted scope.
- **Migration/compatibility:** Existing plan/license overrides must retain their
  meaning.
- **Recommended technical default:** Implement only approved scopes with one
  deterministic, explainable policy; store policy data separately from the
  evaluator.
- **Product Owner decision:**
- **Status:** Pending
- **Decision date:**
- **Approved by:**
- **Revisit conditions:** New target scopes or customer-specific override rules
  are approved.

### LIC-PD-004 — Multiple-role and conflicting assignments

- **Business question:** What happens when multiple applicable role/user or
  organization assignments disagree?
- **Current repository behavior:** Not implemented.
- **Options:** explicit deny wins; most-specific wins; ordered policy; reject
  conflicting configuration.
- **Benefits/risks:** Deny-wins is conservative; most-specific is intuitive but
  can unexpectedly enable; ordered policy is flexible but complex; rejecting
  conflict is safest administratively but limits expressiveness.
- **Database impact:** Conflict prevention or priority fields.
- **API/frontend impact:** Validation and explanation of effective state.
- **Security/scope impact:** Authorization remains independent and mandatory.
- **Migration/compatibility:** None until scoped flags exist.
- **Recommended technical default:** Reject ambiguous conflicting
  configuration until an explicit rule is approved.
- **Product Owner decision:**
- **Status:** Pending
- **Decision date:**
- **Approved by:**
- **Revisit conditions:** Real role-combination scenarios are documented.

### LIC-PD-005 — Feature-dependency semantics

- **Business question:** Are dependencies mandatory, conditional, versioned or
  edition-specific, and what happens when a dependency is removed?
- **Current repository behavior:** Dependencies are stored; enabling checks only
  whether dependency definitions are active.
- **Options:** strict effective dependency; warning-only dependency; conditional
  dependency rules.
- **Database impact:** Strict dependencies need cycle protection and possibly
  effective dates.
- **API/frontend impact:** Activation errors and dependency visualization.
- **Security/scope impact:** Dependencies cannot bypass authorization.
- **Migration/compatibility:** Existing dependency rows require validation.
- **Recommended technical default:** Strict effective dependency with cycle
  rejection after flag semantics are approved.
- **Product Owner decision:**
- **Status:** Pending
- **Decision date:**
- **Approved by:**
- **Revisit conditions:** Conditional or version-specific dependencies become
  concrete requirements.

### LIC-PD-006 — Expiry, downgrade and critical-operation behavior

- **Business question:** What remains available after expiry, downgrade, payment
  block or grace exhaustion, including active sessions?
- **Current repository behavior:** Configurable expiry handlers, grace days,
  payment blocking and emergency access exist.
- **Options:** fail closed; read-only; configured operation classes; continued
  access with compliance alerts.
- **Database impact:** May require policy and transition history.
- **API/frontend impact:** Availability reason and read-only state must be
  explainable.
- **Security/scope impact:** No policy may bypass authentication or permission
  checks.
- **Migration/compatibility:** Preserve existing expiry behavior codes until
  explicitly migrated.
- **Recommended technical default:** Preserve existing behavior; do not extend
  it until operation classes are approved.
- **Product Owner decision:**
- **Status:** Pending
- **Decision date:**
- **Approved by:**
- **Revisit conditions:** Customer continuity, compliance or active-session
  requirements are agreed.

### LIC-PD-007 — Offline validation trust model

- **Business question:** What package, signing, revocation, clock and validation
  policy is required for offline deployments?
- **Current repository behavior:** Offline/emergency provider SPIs perform only
  basic validation.
- **Options:** signed long-lived package; signed short-lived lease; hybrid
  periodic validation.
- **Database impact:** Trust metadata, last validation, package identity and
  replay state may be required.
- **API/frontend impact:** Secure import/validation and status surfaces.
- **Security/scope impact:** Private signing keys never enter the application;
  packages, signatures and keys are not returned or logged.
- **Migration/compatibility:** Existing activation modes must remain readable.
- **Recommended technical default:** Signed versioned package verified with a
  pinned public key, only after cryptographic and revocation requirements are
  approved.
- **Product Owner decision:**
- **Status:** Pending
- **Decision date:**
- **Approved by:**
- **Revisit conditions:** SaaS, on-premise and hybrid connectivity expectations
  are documented.

### LIC-PD-008 — Usage-limit enforcement

- **Business question:** For each limit, should the system warn, reject, queue,
  degrade or allow billed overage?
- **Current repository behavior:** Limits and measurements exist; checks return
  allowed/exceeded and publish an event.
- **Options:** hard ceiling; soft warning; grace/overage; policy per limit type.
- **Database impact:** Atomic reservations or period counters may be needed.
- **API/frontend impact:** Stable limit and consumption responses plus clear
  rejection reasons.
- **Security/scope impact:** Counters and reports must be scope-isolated.
- **Migration/compatibility:** Current `-1` unlimited value must remain valid or
  be migrated forward.
- **Recommended technical default:** Policy per approved limit type; owning
  modules provide meters through `UsageMeterProvider`.
- **Product Owner decision:**
- **Status:** Pending
- **Decision date:**
- **Approved by:**
- **Revisit conditions:** Usage billing or overage sales is approved.

### LIC-PD-009 — Import/export and reports

- **Business question:** Which formats and report definitions are required in
  the first release?
- **Current repository behavior:** Reports and CSV export exist; no licensing
  import, PDF or Excel platform.
- **Options:** CSV/JSON first; all requested formats; shared reporting platform
  first.
- **Database impact:** Imports may require idempotency and package history.
- **API/frontend impact:** File upload/download, validation and progress states.
- **Security/scope impact:** Exports must omit secrets and enforce scoped
  permissions.
- **Migration/compatibility:** Version imported formats and preserve old readers.
- **Recommended technical default:** Reuse CSV and add versioned JSON only when
  approved; defer PDF/Excel/XML to approved shared infrastructure.
- **Product Owner decision:**
- **Status:** Pending
- **Decision date:**
- **Approved by:**
- **Revisit conditions:** A customer or operational workflow requires another
  format.

### LIC-PD-010 — Initial administration scope

- **Business question:** Which licensing workflows and roles must ship first?
- **Current repository behavior:** Backend permissions and placeholder
  navigation exist; no dedicated UI.
- **Options:** licenses only; licenses plus catalog/plans; full administration.
- **Database impact:** None beyond selected backend capabilities.
- **API/frontend impact:** Determines page sections and contract stabilization.
- **Security/scope impact:** Separate platform-global and tenant-scoped
  administration where approved.
- **Migration/compatibility:** Placeholder remains until a verified screen
  exists.
- **Recommended technical default:** Deliver one end-to-end license inventory
  and lifecycle slice before catalog or scoped-flag administration.
- **Product Owner decision:**
- **Status:** Pending
- **Decision date:**
- **Approved by:**
- **Revisit conditions:** Operator roles and priority workflows change.

## E. Technical constraints and security invariants

- Feature availability never grants authorization.
- Licensing remains separate from authentication and authorization.
- Scope comes from trusted server-side context; client IDs are not authority.
- Business modules do not query licensing repositories.
- Business modules use `FeatureGate` or a stable entitlement contract.
- License keys, signatures, tokens and package contents do not appear in normal
  responses, logs, audits, events or exports.
- Existing Flyway migrations are immutable.
- No new `TenantDefaults` or implicit organization defaults.
- Ambiguous and foreign scope fails closed.
- Configuration changes are audited.
- Configurability cannot weaken backend security.
- New schema supports approved change without speculative abstractions.

## F. Options and consequences

Detailed options are recorded with each pending decision above. Selection should
prefer the simplest option that satisfies approved behavior, preserves current
data/contracts and keeps likely approved changes inexpensive.

## G. Final approved decisions

No licensing product decisions in this document are approved yet. When a
decision is supplied, update its decision field, status, date and approver.
Create or update an ADR only when the approved choice establishes a durable
architectural rule.

## Capability matrix

| Capability | Status |
|---|---|
| License/status/type lifecycle | Already implemented |
| Tenant licenses | Already implemented |
| Company association | Partially implemented; unsafe implicit resolution |
| Plans, features and limits | Already implemented |
| Editions | Requires LIC-PD-001 |
| Feature dependencies | Partially implemented; requires LIC-PD-005 |
| License feature overrides | Already implemented |
| Hierarchical scoped flags | Requires LIC-PD-002–004 |
| Central feature gate | Implemented; business adoption incomplete |
| Usage metering | Partially implemented; requires LIC-PD-008 |
| Audit/events/scheduling | Partially implemented |
| Offline activation | Partially implemented; requires LIC-PD-006–007 |
| Reports and CSV | Partially implemented |
| Other import/export formats | Requires LIC-PD-009 |
| Administration UI | Requested; requires stable selected contracts |
| Marketplace/optimization | Deferred |

## Dependency map

```text
LIC-PD-001 -> edition model/API/UI, comparisons, downgrade
LIC-PD-002 -> trusted entitlement scope, company licenses, scoped reports
LIC-PD-002 + LIC-PD-003 + LIC-PD-004 -> scoped feature flags
LIC-PD-003 + LIC-PD-005 -> effective dependency evaluation
LIC-PD-006 + LIC-PD-007 -> enterprise offline validation
LIC-PD-002 + LIC-PD-008 -> scoped usage enforcement
LIC-PD-009 -> import/export delivery
LIC-PD-010 + stable backend contracts -> administration UI
CRIT-001 -> organization-scoped licensing and negative isolation tests
HIGH-001 -> PostgreSQL/Flyway integration verification
```

## Recommended phased backlog

1. Protect sensitive response contracts and add focused regression tests.
2. Approve product decisions and tenant-context architecture.
3. Implement trusted scope and remove ambiguous company selection.
4. Complete approved edition/catalog/dependency behavior.
5. Implement only approved feature-flag targets and precedence.
6. Implement the approved offline trust model.
7. Add approved usage meters and enforcement.
8. Deliver one stable administration slice with both locales and RTL.
9. Add approved import/export and reports through shared infrastructure.
10. Adopt `FeatureGate` incrementally in owning business modules.

These phases are recommendations, not delivery commitments.

## First safe implementation slice

Sensitive response hardening is independent of pending product semantics.

Acceptance criteria:

- Normal license responses contain no raw license key.
- Audit, events and reports contain no raw key or offline package content.
- Authorized activation/validation inputs continue to work.
- No migration or licensing behavior changes.
- Focused regression tests pass.

## Current security finding

`LicenseRepository.findForTenant` ranks a company-scoped license above a
tenant-wide license without a company parameter. Affected paths include
entitlement computation, trials, validation, usage checks and tenant reporting.
This may apply another company's entitlements or limits.

Do not patch the query by assuming precedence. Until LIC-PD-002 and trusted
scope are approved, the recommended temporary behavior is to fail closed when
company-specific candidates exist but company context is unavailable. Applying
that behavior still requires compatibility approval because it can deny access
in current deployments.

## H. Change history

| Date | Change |
|---|---|
| 2026-07-29 | Initial evidence-backed Product Owner decision register |
