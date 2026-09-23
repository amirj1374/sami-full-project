# Phase 5 Execution Plan — CRM Intelligence

This is a technical decomposition of the approved Phase 5 scope in
`docs/IMPLEMENTATION_ROADMAP.md`; it does not define CRM business truth.

## Objective

Extend the existing tenant-scoped CRM/Contact foundation with approved Lead,
Opportunity, follow-up, reminder, history and advisory-intelligence capability
without changing ownership of Sales, Accounting, Inventory, Purchasing or
Treasury.

## Dependency-ordered Steps

| Step | Objective | Dependencies | Validation |
|---|---|---|---|
| P5-S1 | Additive CRM workflow foundation: Lead, Opportunity, follow-up/outcome records, APIs, tenant/RBAC/audit/idempotency boundaries. | Phase 4 complete; no policy TBD required. | PostgreSQL persistence, tenant/role isolation, idempotent writes, compatibility regression. |
| P5-S2 | Integrate the approved three-day satisfaction reminder through existing Scheduler/Automation/Notification infrastructure; separately define the technical event/task boundary before activation. | P5-S1 and existing Sales event facts; missed-purchase behavior remains policy-dependent. | Cross-module PostgreSQL journey, exactly-once task/notification creation, retry/idempotency. |
| P5-S3 | Persist issue/satisfaction outcomes into Contact/customer history and manager issue reporting. | P5-S1, P5-S2. | Append-only history, role visibility, reconciliation and API tests. |
| P5-S4 | Implement advisory inactivity, scoring and churn/decline explanation views. | P5-S1–S3 plus approved DEC-P5-001 policy. | PostgreSQL read models, explainability, no financial mutation. |
| P5-S5 | User-facing follow-up/intelligence workspace and customer integration with rendered UI Quality evidence. | Stable P5-S1–S4 contracts; policy-dependent views follow S4. | Frontend/API/i18n/RTL/responsive/loading/empty/error/read-only evidence. |
| P5-S6 | Final fresh migration, supported upgrade, cross-module acceptance, regression, Guardian and Phase 5 closure. | P5-S1–S5. | All applicable gates green and state/Git closure. |

## Phase DoD

Every applicable approved outcome is implemented, validated and accepted;
tenant/RBAC/audit/idempotency/history invariants pass; reminders and issue
flows reconcile with existing domains; advisory intelligence remains
non-mutating; UI Quality and regression evidence are recorded; Guardian finds
no Critical/Major issue; and state, Contracts, runs and Git are synchronized.

## Decision closure

The former P1 blocker is resolved by approved `docs/decisions/DEC-P5-001-crm-
intelligence-policy.md`. Existing open-decision history remains unchanged;
this decision is the superseding execution authority for Phase 5 CRM
intelligence behavior.
