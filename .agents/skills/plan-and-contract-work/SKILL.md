---
name: plan-and-contract-work
---
# Plan and Contract Work

- **Skill ID:** `plan-and-contract-work-v1`
- **Skill Name:** Plan and Contract Work
- **Purpose:** Turn active authorization into a bounded executable Contract.
- **Primary Owner Agent:** Lead / Orchestrator
- **Allowed Secondary Agents:** Product & UX, Backend, Data & Integrity, QA
- **When to Invoke:** Before substantial implementation.
- **Preconditions:** Active authorization and reconciled state.
- **Required Inputs:** Approved sources, scope, dependencies, DoD, known failures.
- **Authoritative Context Required:** Governance, Business Rules, Constitution,
  roadmap, current source/configuration/tests.
- **Procedure:** Define outcome, scope, exclusions, dependencies, agents,
  validation, Guardian, handoff, and stop conditions.
- **Required Checks:** No invented requirements; no unauthorized scope; actual
  dependencies and file overlap inspected.
- **Expected Outputs / Evidence:** Contract and dependency/parallelism plan.
- **Completion Conditions:** Contract ACTIVE and reviewable.
- **Failure Conditions:** Missing authorization, blocking TBD, unclear owner.
- **Escalation Conditions:** Owner-level product/architecture decision only.
- **Forbidden Actions:** Turning roadmap intent into authorization; scope drift.
- **Related Skills:** `resume-project`, `orchestrate-authorized-work`.
- **State Update Requirement:** Record Contract activation and status.
- **Handoff Requirement:** Contract ID and responsibilities accompany each task.
- **Version / Change Notes:** V1 initial orchestrator skill.
