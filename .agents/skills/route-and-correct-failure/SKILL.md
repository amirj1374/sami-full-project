---
name: route-and-correct-failure
---
# Route and Correct Failure

- **Skill ID:** `route-and-correct-failure-v1`
- **Skill Name:** Route and Correct Failure
- **Purpose:** Classify, route, fix, and retest ordinary failures internally.
- **Primary Owner Agent:** Lead / Orchestrator
- **Allowed Secondary Agents:** Backend, Data & Integrity, QA, Guardian
- **When to Invoke:** Any material failure in active scope.
- **Preconditions:** Failure evidence and active Contract.
- **Required Inputs:** Exact command/error, state, hypothesis, responsible area.
- **Authoritative Context Required:** Governance failure classes and Contract.
- **Procedure:** Classify; assign; formulate hypothesis; correct minimally;
  targeted retest; record attempt; reassess after repeated failure.
- **Required Checks:** No weakened assertion/constraint; no stale failure reuse.
- **Expected Outputs / Evidence:** Failure record and retest result.
- **Completion Conditions:** Resolved, rerouted, or genuine blocker recorded.
- **Failure Conditions:** Blind repetition or missing evidence.
- **Escalation Conditions:** Owner-level authority, authorization, or architecture issue.
- **Forbidden Actions:** JVM-global locking, disabling tests, hiding failures.
- **Related Skills:** `orchestrate-authorized-work`, `update-project-state`.
- **State Update Requirement:** Record material failure and resolution.
- **Handoff Requirement:** Include Failure ID, classification, evidence, fix, retest.
- **Version / Change Notes:** V1 initial orchestrator skill.
