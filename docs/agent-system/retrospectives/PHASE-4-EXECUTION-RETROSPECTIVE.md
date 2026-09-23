# Phase 4 Agent-System Retrospective

This retrospective is execution-system evidence only. It does not define SAMI
business truth and does not reopen Phase 4.

| Observed issue | Root cause | Existing protection | Correction / regression protection | Remaining limitation |
|---|---|---|---|---|
| Intermediate Step completion repeatedly returned control before the parent Phase continued. | Early orchestration path treated child completion as terminal; policy existed before runtime continuation was enforced. | Governance, `AGENTS.md`, `orchestrate-authorized-work`, and `ORCHESTRATION-REGRESSION.md` now define parent-scope continuation. | No new correction required in this retrospective; the continuation regression rule is the durable guard. | A future runtime driver still needs to emit evidence, not only policy text, when execution is hosted outside the repository. |
| Execution windows interrupted Maven/bootstrap work. | Long Spring context startup and Maven dependency/compile phases exceeded individual execution windows. | `PROJECT-STATE`, Contracts and run artifacts provide resumable checkpoints. | No new generic change; resume procedure requires reconciling the exact incomplete boundary. | An external runner must preserve checkpoints before forced termination. |
| Environment failures were initially confused with product blockers. | Nested Maven containers lacked repository paths or Docker/Testcontainers access. | Governance failure classes distinguish `ENVIRONMENT` from Owner blockers; DoD forbids treating unavailable gates as PASS. | External PostgreSQL acceptance path was added to the permanent fixture; the final run proved 19/0/0 without Testcontainers-in-container. | Browser viewport/state limitations remain environment-dependent and must stay explicitly reported. |
| Maven-container Testcontainers could not reach Docker. | Docker daemon/socket is unavailable inside the Maven container. | External PostgreSQL is the approved acceptance path. | `PostgresApplicationFixture` now accepts explicit external JDBC credentials while retaining Testcontainers by default; final acceptance passed. | Testcontainers remains unavailable in nested containers unless the host supplies a supported socket. |
| UI Quality evidence was initially source/test-heavy. | No authorized rendered authenticated session and no desktop/tablet viewport controls in the harness. | UI Quality Skill requires rendered evidence and records unavailable states/viewports rather than fabricating PASS. | P4-S7 run records authenticated rendered Persian/RTL evidence and limitations; no generic correction justified. | Populated/error and desktop/tablet rendered evidence remain harness-dependent. |
| Certification gates risked unnecessary reruns after valid evidence. | Checkpoints and state references were historically stale or broad. | `resume-project` and `update-project-state` require provenance and reconciliation. | Retrospective confirms the rule; current state now points to the final P4-S8 run and commit. | Manual discipline is still required when external evidence is generated outside CI. |
| Unrelated Sales changes were present throughout execution. | Shared integration worktree contained pre-existing user work. | AGENTS/governance forbid destructive cleanup and require scope review. | No unrelated files were committed; final Git status preserves them. | Shared worktrees remain operationally risky without isolated worktrees. |
| Final state/Contract/run metadata drifted during long execution. | State was updated at checkpoints but some older entries remained stale. | `update-project-state` defines meaningful-boundary updates. | Reconciled `PROJECT-STATE.md` to HEAD `5080c94`, Phase 4 COMPLETE/ACCEPTED, and closed P4-S8 artifacts. | Historical run text remains historical evidence and is not rewritten. |

## Conclusion

No additional generic Agent-System mechanism is justified by Phase 4 evidence.
The durable protections now in place are continuation semantics, checkpointed
resume, explicit environment classification, external acceptance configuration,
rendered UI evidence requirements, Guardian review, and state/run reconciliation.

Phase 5 remains NOT STARTED and NOT AUTHORIZED.
