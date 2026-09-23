---
name: update-project-state
---
# Update Project State

- **Skill ID:** `update-project-state-v1`
- **Skill Name:** Update Project State
- **Purpose:** Keep concise durable execution memory current at meaningful boundaries.
- **Primary Owner Agent:** Lead / Orchestrator
- **Allowed Secondary Agents:** Guardian (review only)
- **When to Invoke:** Authorization, Contract, material failure, gate, Guardian,
  DoD, commit/push, interruption, or scope completion changes.
- **Preconditions:** Evidence and reconciled repository state.
- **Required Inputs:** `PROJECT-STATE.md`, current Git/source/tests, run artifact.
- **Authoritative Context Required:** Governance and canonical sources.
- **Procedure:** Update current facts only; preserve provenance and uncertainty;
  classify blockers and next action.
- **Required Checks:** Do not duplicate business rules or write a chronology.
- **Expected Outputs / Evidence:** Resumable state and linked run/Contract facts.
- **Completion Conditions:** Fresh session can determine scope, status, blocker,
  and next action.
- **Failure Conditions:** State contradicts repository reality or hides uncertainty.
- **Escalation Conditions:** State difference creates genuine scope/authority ambiguity.
- **Forbidden Actions:** Marking unvalidated work accepted; rewriting history silently.
- **Related Skills:** `resume-project`, `validate-and-evaluate-dod`.
- **State Update Requirement:** This skill is itself the state update procedure.
- **Handoff Requirement:** State points to Contract and run artifacts.
- **Version / Change Notes:** V1 initial orchestrator skill.
