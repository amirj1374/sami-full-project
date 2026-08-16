---
name: sami-ui-workflow-tester
description: Execute evidence-based SAMI ERP browser QA as realistic user journeys. Use when asked to test the UI, all pages or forms, end-to-end business workflows, customer/product/user creation, purchasing, receiving, sales, login/RBAC, keyboard-only operation, Persian/English behavior, RTL/LTR, mobile responsiveness, console/network failures, or to classify which UI defects Codex can fix.
---

# SAMI UI Workflow Tester

Act as a careful ERP operator, not a page scraper. Exercise persisted business
flows through the visible UI and prove the result at each boundary.

## Prepare the run

1. Read `AGENTS.md`, `.agents/SAMI_PROJECT_CONTEXT.md`,
   `docs/21-ai-agent-guide.md`, `docs/15-testing-and-quality.md`, and the
   applicable module documentation.
2. Read the available browser-control skill completely before browser work.
3. Record branch, SHA, worktree state, target URL, environment type, browser,
   locale, viewport, and test-run identifier.
4. Confirm the target is local, disposable, or explicitly approved for test
   mutations. Never create records in production or a shared environment
   without explicit authorization.
5. Use a run marker such as `SAMI-QA-<UTC timestamp>-<short SHA>` in every
   created record. Obtain credentials from approved runtime configuration or
   the user; never print, screenshot, persist, or commit secrets.
6. Prefer the real backend and PostgreSQL. Mock mode is valid only for visual,
   navigation, accessibility, and responsive inspection; mark transactional
   journeys `BLOCKED_REAL_BACKEND_REQUIRED` when only mocks are available.

If this run creates Docker resources, use a unique Compose project name and
follow the repository cleanup policy. Preserve unrelated containers, networks,
volumes, databases, browser sessions, and user data.

## Build the test matrix

Read [references/scenario-catalog.md](references/scenario-catalog.md). Include:

- every route reachable from the authenticated menu for a page-level smoke;
- every critical journey requested by the user;
- prerequisite journeys needed to create valid business data;
- permission, validation, empty, loading, failure, keyboard, localization, and
  responsive variants applicable to the changed area.

Discover current labels, required fields, lifecycle actions, permissions, and
routes from the running UI and authoritative source. Do not encode remembered
selectors or invent missing contracts. Use accessible roles, labels, and names
before CSS selectors. Re-resolve elements after navigation or rerender.

## Execute each journey

For every scenario:

1. State the precondition and expected observable result.
2. Perform the flow through the UI. Direct API calls may prepare isolated
   prerequisites only when the user authorizes them; they never replace the UI
   action under test.
3. Wait for a visible settled state. Check the corresponding network response,
   not only a toast.
4. Re-open or search for the created record to prove persistence.
5. Verify the next module boundary when relevant: for example customer history,
   purchasing settlement, inventory receipt, or sale status.
6. Capture focused screenshots for failures and high-value checkpoints. Avoid
   credentials and unrelated personal data.
7. Inspect console errors, failed requests, unexpected redirects, duplicate
   submissions, stale state, broken focus, and horizontal overflow.
8. Record `PASS`, `FAIL`, `BLOCKED`, or `NOT_RUN`; a skipped or unavailable
   check is never a pass.

Use a fresh standard user created during the run for the login/RBAC journey.
Sign out fully, sign in as that user, verify the visible menu and direct-route
authorization, then restore the administrator session without reading browser
storage or cookies.

## Interaction and viewport passes

- Run critical journeys once without a mouse: Tab/Shift+Tab, Enter, Space,
  Escape, arrow keys, and the project's shortcut system. Verify visible focus,
  logical order, dialog focus containment, and focus restoration.
- Verify Persian/RTL and English/LTR independently; changing locale must update
  existing content without a stale reload state.
- Use desktop plus widths 360, 375, 390, 412, and 430. Check the document and
  each data surface for horizontal overflow. Exercise mobile record cards,
  details expansion, swipe/revealed actions, dialogs, validation messages, and
  sticky controls where present.
- Respect reduced-motion settings. A swipe-only action without an accessible
  keyboard/button alternative is a failure.

## Defect handling

Remain read-only when the request is test/review only. When fixes are also
authorized, classify each defect before changing code:

- `SELF_FIXABLE`: deterministic repository defect with a verified expected
  behavior and no material contract or business decision;
- `NEEDS_BUSINESS_DECISION`: ambiguous workflow, policy, wording, or lifecycle;
- `ENVIRONMENT_BLOCKED`: unavailable service, data, credential, browser, or
  infrastructure;
- `EXTERNAL_BLOCKED`: third-party contract or service failure.

Fix only authorized `SELF_FIXABLE` items. Re-run the failing scenario plus the
smallest relevant regression set, then run configured source tests/type-check/
build. Never weaken assertions or alter business data merely to obtain a pass.

## Clean up and report

Delete only records that carry the current run marker and only through a safe,
supported lifecycle. If cleanup would be destructive, cross-module, or
unsupported, leave the records intact and list their identifiers. Tear down
only disposable infrastructure created by this run.

Use [references/report-template.md](references/report-template.md). Report exact
counts, journeys, evidence, defects, self-fixable count, blocked gates, created
and retained test data, infrastructure cleanup, commands, SHA, and final Git
status. Never claim broad UI coverage from route existence or mocked success.
