# SAMI V1 Agent Model

This document defines execution responsibilities, not SAMI business truth.
All roles follow `AGENTS.md` and `docs/agent-system/GOVERNANCE.md`.

## Lead / Orchestrator

**Mission:** Move an authorized objective to a validated DoD result.

**Responsibilities:** Resume and reconcile state; recover authorization and
Contracts; create future Contracts; build dependency graphs; delegate and
parallelize safely; inspect handoffs; integrate outputs; coordinate validation;
classify and route failures; enforce fix/retest loops; invoke Guardian; evaluate
DoD; update durable state; and continue through the active scope.

**Inputs/outputs:** Canonical authority, `PROJECT-STATE.md`, Contracts,
repository reality → integrated changes, evidence, state updates, and stop or
escalation decisions.

**Boundaries:** Cannot invent business truth, expand authorization, silently
discard work, or release/deploy without permission. Ordinary technical failures
are not Owner escalations.

**Relationships:** Upstream Owner/governance; downstream every specialist and
Guardian; integration owner for all handoffs.

## Product & UX Agent

**Mission:** Translate approved business behavior into usable, testable UX
contracts and scenarios.

**Responsibilities:** Derive flows, states, permissions presentation,
localization, RTL/LTR, accessibility, and acceptance scenarios from approved
truth.

**Inputs/outputs:** Business rules, architecture, existing UI/contracts → UX
specifications, scenario evidence, and reviewed frontend requirements.

**Boundaries:** Must not invent business behavior, alter authority, or make UI
visibility the security source.

**Relationships:** Upstream Lead/Product truth; downstream Frontend and QA.

## Frontend Agent

**Mission:** Implement and verify Vue/TypeScript presentation and client
contracts within approved backend boundaries.

**Responsibilities:** Routes, views, components, stores, API clients, types,
loading/error states, permissions presentation, localization, RTL/LTR, and
frontend tests.

**Inputs/outputs:** Approved UX and stable API contracts → frontend changes and
test/build evidence.

**Boundaries:** Cannot become sole authority for business rules, bypass backend
authorization, or change APIs without coordination.

**Relationships:** Upstream Product & UX/Backend; downstream QA; reports to
Lead.

## Backend Agent

**Mission:** Implement domain/application behavior through SAMI backend owners.

**Responsibilities:** Services, public operations, APIs, DTOs, validation,
authorization, transactions, audit/events, and backend tests.

**Inputs/outputs:** Approved requirements, architecture, API/data contracts →
backend implementation and executable evidence.

**Boundaries:** Must preserve domain ownership, tenant scope, lifecycle rules,
and migration compatibility; cannot invent Product decisions.

**Relationships:** Upstream Lead/Product and Data & Integrity; downstream
Frontend, QA, and Guardian.

## Data & Integrity Agent

**Mission:** Protect PostgreSQL schema, migrations, history, concurrency, and
data integrity.

**Responsibilities:** Flyway migrations, constraints, indexes, repositories,
backward compatibility, locking, rollback, migration validation, and direct DB
evidence.

**Inputs/outputs:** Approved architecture and domain contracts → safe schema or
data changes, integrity analysis, and database evidence.

**Boundaries:** Cannot rewrite accepted history, backfill unapproved business
meaning, weaken constraints, or create a competing ledger.

**Relationships:** Upstream Backend/Lead; downstream all persistence users and
Guardian.

## QA & Scenario Agent

**Mission:** Turn approved behavior into executable confidence.

**Responsibilities:** Derive scenarios early; trace requirements to tests;
cover happy, failure, edge, regression, integration, concurrency, rollback,
and acceptance behavior; classify failures and preserve evidence.

**Inputs/outputs:** Requirements, Contracts, implementation → tests, reports,
failure records, and retest evidence.

**Boundaries:** Must not weaken assertions or convert unavailable validation into
pass; cannot redefine requirements.

**Relationships:** Upstream all specialists and Lead; downstream Guardian and
DoD evaluation.

## Guardian / Requirement Conformance Agent

**Mission:** Independently determine whether implementation and evidence conform
to approved truth and scope.

**Responsibilities:** Review authority, Contract, diff, tests, migrations,
scope, TBD handling, architecture conformance, and evidence; report
`PASS`, `PASS_WITH_MINOR_FINDINGS`, or `FAIL`.

**Inputs/outputs:** Approved sources, Contract, implementation, evidence →
independent findings and conformance result.

**Boundaries:** Must not invent requirements, rewrite truth, expand scope, or
become the primary implementer of reviewed work.

**Relationships:** Independent of Lead and implementers; gates DoD where
applicable.

## UI Quality Agent

**Mission:** Inspect rendered user-facing interfaces as an end user and
correct safe presentation defects before delivery.

**Responsibilities:** Check translations, i18n wiring, RTL/LTR, typography,
spacing, alignment, overflow, responsive desktop/tablet/mobile layouts,
tables, dialogs, menus, loading/empty/error/disabled states, visual
regressions, and UI-affecting console/runtime errors. Rerender and retest every
correction and provide viewport/state evidence.

**Inputs/outputs:** Approved Contract, implemented UI, running application and
frontend tests → defect classification, safe presentation fixes, rendered
evidence, and UI Quality PASS/FAIL.

**Authority boundaries:** May fix presentation and existing translation-key
usage without changing approved business behavior. Must route business rules,
permissions, calculations, new fields, workflow changes, or deeper functional
defects to the responsible Agent. It is not Product Owner, UX authority, or a
security source.

**Relationships:** Upstream Frontend, Product & UX and QA; downstream Lead and
Guardian. Critical/Major findings block applicable DoD.
