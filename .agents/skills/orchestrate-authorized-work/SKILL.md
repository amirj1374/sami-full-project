---
name: orchestrate-authorized-work
---
# Orchestrate Authorized Work

- **Skill ID:** `orchestrate-authorized-work-v1`
- **Skill Name:** Orchestrate Authorized Work
- **Purpose:** Execute an ACTIVE Contract through integration and continuation.
- **Primary Owner Agent:** Lead / Orchestrator
- **Allowed Secondary Agents:** All V1 roles
- **When to Invoke:** Contract is ACTIVE and dependencies are known.
- **Preconditions:** Reconciled state, Contract, safe parallelism plan.
- **Required Inputs:** Contract, repository, handoff protocol, validation plan.
- **Authoritative Context Required:** Governance and Contract references.
- **Procedure:** Delegate; inspect evidence; integrate; validate; route failures;
  continue until DoD or valid stop. For `PHASE`/`WAVE` authorization, a
  completed Step is a checkpoint: update evidence, activate the next
  dependency-valid Step, and continue in the same execution. Do not return an
  intermediate status report while the parent authorization remains ACTIVE.
- **Required Checks:** Handoffs contain evidence; shared changes coordinated;
  unrelated work preserved.
- **Expected Outputs / Evidence:** Integrated changes and run evidence.
- **Completion Conditions:** A task/feature Contract may complete at its
  boundary; a PHASE/WAVE Contract reaches a terminal condition only when the
  parent scope is complete, genuinely Owner-blocked, or explicitly interrupted.
- **Failure Conditions:** Missing evidence, scope drift, unsafe integration.
- **Escalation Conditions:** Authorization/architecture/owner-level blocker.
- **Forbidden Actions:** Stop after one delegated result; invent rules; blind retries.
- **Continuation Check:** After every Step DoD decision, evaluate the parent
  authorization and next dependency graph node before yielding control. Only
  terminal parent conditions permit a user-facing final report.
- **Related Skills:** `route-and-correct-failure`, `validate-and-evaluate-dod`.
- **State Update Requirement:** Record material work, failures, and gate changes.
- **Handoff Requirement:** Use the full handoff fields in the active run.
- **Version / Change Notes:** V1 initial orchestrator skill.
