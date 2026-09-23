---
name: validate-and-evaluate-dod
---
# Validate and Evaluate DoD

- **Skill ID:** `validate-and-evaluate-dod-v1`
- **Skill Name:** Validate and Evaluate DoD
- **Purpose:** Run required gates and determine evidence-backed completion.
- **Primary Owner Agent:** Lead / Orchestrator
- **Allowed Secondary Agents:** QA & Scenario, Data & Integrity, Guardian
- **When to Invoke:** After implementation and after corrections.
- **Preconditions:** Contract validation plan and stable target environment.
- **Required Inputs:** Commands, acceptance criteria, DoD, run evidence.
- **Authoritative Context Required:** Contract, DoD, testing guide, current source.
- **Procedure:** Run gates; capture exact totals; classify unavailable checks;
  compare evidence to each DoD item.
- **Required Checks:** Fresh DB/migration, regression, concurrency, rollback,
  direct persistence evidence where applicable.
- **Expected Outputs / Evidence:** Validation matrix and DoD verdict.
- **Completion Conditions:** All applicable gates PASS or explicitly permitted.
- **Failure Conditions:** Required failure, unavailable release-critical gate,
  or missing evidence.
- **Escalation Conditions:** Only owner-level unresolved blocker.
- **Forbidden Actions:** Treat NOT RUN as PASS; use historical reports as current proof.
- **Related Skills:** `requirement-conformance-review`, `update-project-state`.
- **State Update Requirement:** Record gate transitions and verdict.
- **Handoff Requirement:** Give Guardian exact evidence and unresolved findings.
- **Version / Change Notes:** V1 initial orchestrator skill.
