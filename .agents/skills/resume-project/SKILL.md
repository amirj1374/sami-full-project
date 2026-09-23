---
name: resume-project
---
# Resume Project

- **Skill ID:** `resume-project-v1`
- **Skill Name:** Resume Project
- **Purpose:** Reconcile durable state with repository reality before work.
- **Primary Owner Agent:** Lead / Orchestrator
- **Allowed Secondary Agents:** Guardian
- **When to Invoke:** Every fresh session or interruption.
- **Preconditions:** Repository root available; read `AGENTS.md`, governance,
  `PROJECT-STATE.md`, and canonical sources.
- **Required Inputs:** Branch, HEAD, upstream, status, migrations, source,
  tests, Contracts, validation evidence.
- **Authoritative Context Required:** Project Index, Business Rules,
  Constitution, roadmap, open decisions, governance.
- **Procedure:** Inspect; compare; classify differences; preserve evidence;
  recover authorization and next action.
- **Required Checks:** No scope drift; no lost dirty changes; no stale claim
  silently promoted.
- **Expected Outputs / Evidence:** Reconciliation notes and next authorized action.
- **Completion Conditions:** State is reconciled or an owner-level ambiguity is recorded.
- **Failure Conditions:** Missing authority, unsafe repository state, or unknown scope.
- **Escalation Conditions:** Genuine authority or authorization ambiguity only.
- **Forbidden Actions:** Reset, clean, discard, business-rule invention.
- **Related Skills:** `plan-and-contract-work`, `update-project-state`.
- **State Update Requirement:** Update only material reconciliation changes.
- **Handoff Requirement:** Provide state, scope, evidence, blockers, next action.
- **Version / Change Notes:** V1 initial orchestrator skill.
