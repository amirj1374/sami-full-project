---
name: real-user-acceptance
description: Execute adversarial, evidence-based browser journeys that prove visible, business, persisted and cross-screen results for user-facing SAMI work.
---

# Real User Acceptance / Adversarial Browser QA

- **Skill ID:** `real-user-acceptance`
- **Primary Owner Agent:** Real User Acceptance / Adversarial Browser QA Agent
- **Allowed Secondary Agents:** QA & Scenario Agent, UI Quality Agent, Product & UX Agent
- **Purpose:** Test a user-facing workflow as a skeptical, non-developer operator and prove the complete observable and persisted outcome.
- **When to Invoke:** Every user-facing Contract containing a material business mutation or cross-module journey, before DoD and Guardian review.
- **Preconditions:** Active Contract; approved requirements; legitimate authenticated environment; disposable or explicitly approved data scope; database read access for verification where authorized.
- **Harness Rule:** When `sami-frontend/playwright.config.ts` and the repository browser runner exist, use `npm run run-real-user-acceptance` (or its documented container equivalent) before declaring `BLOCKED BY HARNESS`. The runner is preferred over an external invisible browser integration.
- **Required Inputs:** Contract, business rules, architecture, route/API conventions, test data plan, credentials from approved configuration, viewport/locale matrix.
- **Authoritative Context Required:** `AGENTS.md`, `docs/agent-system/GOVERNANCE.md`, approved business/architecture sources, Contract, and source-domain ownership rules.
- **Procedure:** Record run ID, commit, environment, browser, locale and viewport; derive the journey from approved behavior without using implementation knowledge to fill UI gaps; for every material action verify `ACTION → VISIBLE RESULT → BUSINESS RESULT → PERSISTED RESULT → CROSS-SCREEN CONSISTENCY`; refresh/reopen and navigate away/back after critical mutations; exercise negative, duplicate-submit, retry, authorization and recovery cases; inspect console/network and loading/empty/error states; inspect desktop/tablet/mobile and Persian/RTL plus English/LTR where supported; compare UI with authoritative backend/database state; record a step-by-step journey ledger and focused evidence.
- **Required Checks:** Required fields and labels, obvious next action, actionable errors, no dead ends, visual defects, cross-screen consistency, refresh/reopen persistence, idempotency, tenant/permission isolation, duplicate prevention, console/network integrity, and database truth.
- **Expected Outputs / Evidence:** Journey ledger with user goal, page, action, expected/actual visible result, business/persisted result, status, defect/evidence reference; screenshot/state evidence; console/network summary; DB read-back; exact test counts.
- **Completion Conditions:** Every applicable critical step is `PASS`; no Critical/Major finding; all applicable states are explicitly `PASS`, `FAIL`, `NOT TESTED`, `BLOCKED BY HARNESS`, or `NOT APPLICABLE`; no unavailable state is implied PASS; independent UI Quality and Guardian review are complete.
- **Failure Conditions:** API-only workaround required for a user action, visible and persisted truth disagree, critical mutation is not durable, a normal user cannot recover, duplicate/negative action corrupts data, material runtime error occurs, or required evidence is unavailable.
- **Escalation Conditions:** Genuine business/authority ambiguity, security boundary requiring policy, or external service decision. Ordinary bugs, UI defects and harness recovery remain internal work.
- **Forbidden Actions:** Invent business rules, bypass authentication/authorization, create hidden developer-only workflow, weaken assertions, modify database to manufacture evidence, claim rendered PASS from source/tests, or certify the implementation Agent's own fix without independent retest.
- **Related Skills:** `sami-ui-workflow-tester`, `validate-and-correct-ui-quality`, `sami-contract-validator`, `validate-and-evaluate-dod`, `requirement-conformance-review`.
- **State Update Requirement:** Update Contract/run with the journey ledger, evidence status and blocked/not-tested states at meaningful boundaries.
- **Handoff Requirement:** Include run ID, commit, environment, viewport/locale/state matrix, evidence references, defects, retest results and downstream action.
- **Version / Change Notes:** V1 — hardened real-user acceptance standard; supersedes shallow browser smoke as a sufficient workflow gate.
