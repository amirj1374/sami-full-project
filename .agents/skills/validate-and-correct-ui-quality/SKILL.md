---
name: validate-and-correct-ui-quality
description: Inspect and safely correct rendered SAMI ERP UI quality defects before a user-facing Contract is accepted.
---

# Validate and Correct UI Quality

- **Skill ID:** `validate-and-correct-ui-quality`
- **Primary Owner Agent:** UI Quality Agent
- **Allowed Secondary Agents:** Frontend Agent, Product & UX Agent, QA & Scenario Agent
- **Purpose:** Render, inspect, correct safe presentation defects, rerender and provide evidence across representative viewport and UI states.
- **When to Invoke:** Every Contract containing user-facing UI changes, before Guardian review and DoD evaluation.
- **Preconditions:** Active Contract; approved requirements; running UI or an explicitly recorded environment limitation.
- **Required Inputs:** Contract, changed routes/components, authority sources, frontend test commands, locale and runtime configuration.
- **Authoritative Context Required:** `AGENTS.md`, Governance, approved business/architecture sources, Contract and existing UI conventions.
- **Procedure:** Render desktop, tablet and mobile layouts; inspect English/LTR and Persian/RTL; exercise loading, empty, populated, error and disabled/read-only states; inspect console/network failures; record defects; fix only deterministic presentation defects; rerender and retest.
- **Required Checks:** Translation leakage, i18n keys, RTL/LTR, spacing, alignment, overlap, clipping, overflow, responsive behavior, tables, dialogs, menus, control sizing, hierarchy, states and UI-affecting errors.
- **Expected Outputs / Evidence:** Pages/flows, viewport classes, states, screenshots or equivalent rendered evidence, defect severity, corrections, regression commands and final UI QUALITY PASS/FAIL.
- **Completion Conditions:** No Critical/Major findings; safe Minor findings corrected or explicitly recorded; frontend regression passes; evidence attached to the run.
- **Failure Conditions:** Unusable layout, important untranslated content, overlap, broken responsive behavior, UI runtime error, or unavailable rendered inspection without an explicit blocked result.
- **Escalation Conditions:** Business ambiguity, new permission/workflow, calculation or API change, or defects outside presentation authority.
- **Forbidden Actions:** Invent business rules, fields, permissions, calculations or workflows; weaken assertions; claim visual PASS from source inspection when rendered inspection is available.
- **Related Skills:** `sami-ui-workflow-tester`, `sami-frontend-builder`, `sami-ui-ux-designer`, `validate-and-evaluate-dod`.
- **State Update Requirement:** Record UI gate status at Contract/run and DoD boundaries.
- **Handoff Requirement:** Include viewport, locale, state and rendered evidence in the standard handoff.
- **Version / Change Notes:** V1 — initial UI Quality Agent capability.
