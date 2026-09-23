---
name: requirement-conformance-review
---
# Requirement Conformance Review

- **Skill ID:** `requirement-conformance-review-v1`
- **Skill Name:** Requirement Conformance Review
- **Purpose:** Independently review implementation and evidence against approved truth.
- **Primary Owner Agent:** Guardian / Requirement Conformance Agent
- **Allowed Secondary Agents:** QA & Scenario (evidence support only)
- **When to Invoke:** After technical validation is green.
- **Preconditions:** Contract, approved sources, diff, tests, and evidence available.
- **Required Inputs:** Business/architecture references, Contract, implementation,
  migration, validation, scope diff.
- **Authoritative Context Required:** Governance, Business Rules, Constitution,
  ADRs, Contract.
- **Procedure:** Check invented/weakened requirements, TBD handling, scope,
  architecture, legacy compatibility, tenant isolation, and evidence.
- **Required Checks:** Independent reasoning; inspect actual files and outputs.
- **Expected Outputs / Evidence:** PASS, PASS_WITH_MINOR_FINDINGS, or FAIL.
- **Completion Conditions:** No Critical/Major finding remains.
- **Failure Conditions:** Conformance gap, missing evidence, or scope drift.
- **Escalation Conditions:** Genuine authority conflict or owner decision.
- **Forbidden Actions:** Implement reviewed work; rewrite requirements to pass.
- **Related Skills:** `validate-and-evaluate-dod`, `update-project-state`.
- **State Update Requirement:** Record independent Guardian result.
- **Handoff Requirement:** Findings include severity, evidence, and responsible Agent.
- **Version / Change Notes:** V1 initial orchestrator skill.
