---
name: sami-verification-workflow
description: Experimental, recommendation-only workflow for selecting verification levels and recording evidence for SAMI ERP tasks; it does not gate releases or block work.
---

# SAMI Verification Workflow (EXPERIMENTAL)

This skill organizes evidence for a requested change or investigation. It is
report-only: it must not introduce a release gate, make `NOT RUN` mean failure,
or prevent a task from continuing. Existing repository and task-specific rules
still apply.

## Verification levels

- **LEVEL 1 — Source inspection:** trace the executable owner, inputs, outputs,
  validation, error handling and relevant configuration.
- **LEVEL 2 — Unit / contract:** run focused component, service, API-contract or
  static regression tests.
- **LEVEL 3 — Integration:** exercise real service boundaries, persistence,
  migrations, authorization and transaction behavior (normally PostgreSQL when
  persistence is in scope).
- **LEVEL 4 — Browser / UI workflow:** perform the user journey in a real
  browser and inspect rendered state, console and network responses.
- **LEVEL 5 — Production-like environment:** validate the built containers,
  reverse proxy, runtime configuration, health checks and representative
  end-to-end behavior in an isolated environment.
- **LEVEL 6 — Real server verification:** verify the deployed revision, live
  URL, server configuration and externally observable behavior.

Select the minimum level that can prove the requested behavior and recommend a
higher level when the behavior crosses a boundary. For example, a persisted
write needs Level 3; a save-button or navigation defect needs Level 4; a live
deployment defect needs Level 6. The selection is guidance, not a blocker.

## Evidence rules

Record each check independently as `PASS`, `FAIL`, `NOT RUN`, `BLOCKED`, or
`NOT APPLICABLE`, with the command or interaction, environment, timestamp/run
marker, and observable evidence. Never infer a runtime result from source,
routes, types, a build, or a mocked response. Distinguish:

`Source → tests → runtime → network → persistence → production-like → server`.

An unavailable level remains explicitly unverified while other levels may still
be reported. Do not synthesize an overall PASS/FAIL and do not alter release,
deployment, authorization, or product policy as a consequence of this skill.

## Workflow

1. Define the user-visible behavior and its owning component/service.
2. Inspect the repository owner, contract, configuration and reusable tests.
3. Choose required and recommended levels using the boundary rules above.
4. Run available checks in an order that makes failures diagnosable.
5. Capture exact evidence, gaps and environment limitations for every level.
6. Report what improved, what remains unverified, and the next useful check.

Use specialized SAMI skills to perform domain work: backend, migration,
contract, frontend, architecture, release, project-context and browser skills
remain the owners of their respective checks. This skill coordinates their
evidence; it does not replace them.

## Report template

```text
Behavior: <requested behavior>
Required levels: <levels and rationale>
Recommended levels: <optional higher levels>

| Evidence | Status | Command/interaction | Observable result |
| Source | PASS/FAIL/NOT RUN/BLOCKED/NOT APPLICABLE | ... | ... |
| Unit/contract | ... | ... | ... |
| Integration | ... | ... | ... |
| Browser | ... | ... | ... |
| Network/API | ... | ... | ... |
| Persistence | ... | ... | ... |
| Production-like | ... | ... | ... |
| Server | ... | ... | ... |

What improved: ...
What remains unverified: ...
Next recommendation: ...
```

