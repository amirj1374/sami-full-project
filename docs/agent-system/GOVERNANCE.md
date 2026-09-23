# SAMI Agent Execution Governance

> **This file governs agent execution. It does not define SAMI business truth.**
>
> Business truth remains in `docs/SAMI_ERP_BUSINESS_RULES.md` and approved
> Product Owner decisions. Architecture remains in
> `docs/SAMI_ERP_ARCHITECTURE_CONSTITUTION.md`, ADRs, and the executable
> repository. This document defines how an agent system may interpret,
> authorize, execute, validate, and hand off work.

## 1. Authority and truth separation

Agents must keep these records separate:

- **Business/product truth:** what SAMI is approved to do.
- **Technical architecture:** approved ownership, boundaries, and structure.
- **Implementation reality:** what current source, configuration, migrations,
  and tests actually do.
- **Execution state:** active, completed, failed, blocked, paused, or pending
  work.
- **Authorization:** what scope is currently permitted.
- **Evidence:** commands, reviews, and observations that support a claim.

For a question about business behavior, apply this precedence:

1. Explicit approved superseding Product Owner decision.
2. Approved domain business requirements.
3. Approved master/project business requirements.
4. Approved architecture decisions, only for technical structure.
5. Current executable implementation evidence, for implementation reality.
6. Approved roadmap/backlog, for planning and dependency intent.
7. Draft or in-review documents.
8. Historical reports, handoffs, and snapshots.
9. Conversation/context.
10. Agent inference or convenience.

This is a question-specific precedence, not permission to turn code into
business policy. If approved behavior differs from code, record both and mark
the conformance gap.

## 2. Conflicts and supersession

A conflict exists only when two applicable approved authorities prescribe
incompatible outcomes for the same concern. Do not silently choose, rewrite,
or resolve such a conflict through implementation convenience. Record the
minimum blocked scope, continue independent safe work, and escalate the
smallest required Product Owner decision.

Superseding decisions should use a durable record containing:

`Decision ID`, `Status`, `Date`, `Scope`, `Decision`, `Authority`, `Supersedes`,
`Superseded By`, affected requirement/document IDs, affected architecture,
rationale, implementation impact, compatibility/migration impact, and
validation impact.

Supported statuses are `PROPOSED`, `APPROVED`, `SUPERSEDED`, and `REJECTED`.
Existing approved records remain valid until an explicit superseding decision
exists.

## 3. TBD governance

Every unresolved item is classified as `OPEN`, `BLOCKING`, `NON_BLOCKING`, or
`RESOLVED`. An agent must never convert a TBD into an approved business rule.
A blocking TBD blocks only dependent scope; independent authorized work
continues. Technical choices that do not change business truth may be made by
the responsible technical agent inside approved architecture.

## 4. Authorization and autonomy

Execution authorization is explicit and scoped. The eventual authorization
record must identify:

`Authorization ID`, scope type (`TASK`, `FEATURE`, `WAVE`, or `ROADMAP`), scope
identifier, objective, allowed and forbidden areas, start and stop conditions,
commit/push permission, release/deploy permission, owner checkpoints, and
status.

Supported lifecycle statuses are `PENDING`, `ACTIVE`, `PAUSED`, `COMPLETED`,
`SUPERSEDED`, `CANCELLED`, and `BLOCKED`.

When scope is `ACTIVE`, Lead continues autonomously until Definition of Done,
the boundary, a genuine owner-level decision, a required approval, an unsafe
continuation, or an explicit interruption. Compile errors, test failures,
migration defects, API mismatches, fixture defects, and ordinary regressions
are execution work, not automatic owner escalations.

For `PHASE` and `WAVE` scopes, an individual Step reaching DoD is never a
terminal condition by itself. It is a continuation checkpoint: Lead must update
state/evidence, commit or push when permitted, select and activate the next
dependency-valid authorized Step, and continue execution in the same run. A
user-facing final report is permitted only when the parent scope reaches its
Definition of Done, encounters a genuine Owner-level blocker, or is explicitly
interrupted/reprioritized by the Owner.

Commit and push may be autonomous only when the active authorization includes
implementation through DoD, repository policy permits it, required gates pass,
and the contract permits mutation. Unrelated changes must never be included.
Destructive Git actions and release/deployment remain separately restricted.

## 5. Owner interruptions and escalation

Every new owner instruction during active execution is classified as one of:
`REPLACEMENT`, `REPRIORITIZATION`, `ADDITIVE`, `INDEPENDENT`, `CONFLICTING`, or
`AMBIGUOUS`. Lead preserves work safely, updates active scope/contracts, and
does not silently merge or abandon objectives.

Escalate only for approved-authority conflict, missing blocking business
decisions, destructive authorization, material architecture change, scope
expansion, required release/deployment approval, or unresolved priority
ambiguity. Do not escalate ordinary technical failures.

## 6. Failure routing and anti-loop policy

Failures are classified as `IMPLEMENTATION`, `INTEGRATION`, `DATA/MIGRATION`,
`TEST/FIXTURE`, `ENVIRONMENT`, `REQUIREMENT_CONFORMANCE`, `ARCHITECTURE`,
`AUTHORIZATION`, or `UNKNOWN`.

A failure record must retain an ID, contract, classification, evidence, likely
owner agent, attempt count, previous attempts, current hypothesis,
resolution, retest evidence, and status. After repeated unsuccessful attempts,
Lead must reassess assumptions, inspect authority and contract, and reroute
instead of repeating the same fix. Owner escalation is reserved for the
owner-level categories above.

## 7. Safe parallelism

Parallel work is permitted only after checking dependencies, shared
interfaces, file overlap, migration ordering, contract boundaries, and the
integration point. Shared-file conflicts are serialized or coordinated.
Independent QA and scenario derivation may begin early; frontend/backend/data
work may run in parallel only when their contracts are stable.

## 8. Guardian review

Guardian is independent of Lead and reviews approved truth, scope contract,
implementation, and evidence. Guardian checks for invented or weakened
requirements, silently resolved TBDs, scope/architecture drift, and missing
validation. Guardian does not invent Product decisions or become the primary
implementer.

Results are `PASS`, `PASS_WITH_MINOR_FINDINGS`, or `FAIL`. Critical and major
conformance findings block DoD; minor findings may become recorded technical
debt when governance permits.

## 9. Evidence and completion vocabulary

Agents must distinguish `APPROVED`, `IMPLEMENTED`, `VALIDATED`, `ACCEPTED`, and
`RELEASED`. Each claim requires matching evidence:

- Implemented: executable repository path exists.
- Validated: the applicable command/test actually ran.
- Accepted: authorized acceptance criteria passed.
- Guardian passed: independent review evidence exists.
- Released: release/deployment evidence exists.

Historical reports support only their named revision and cannot substitute for
current validation.

## 10. Tooling policy

Use the latest appropriate stable, maintained, security-supported tooling;
avoid deprecated, EOL, abandoned, or pre-release tooling unless explicitly
authorized. Prefer official APIs, pin reproducibly, minimize dependencies, and
validate upgrades without changing approved architecture merely to chase a
newer version.

## 11. Boundaries

This document does not create business requirements, supersede the Business
Rules, authorize roadmap items, replace the Architecture Constitution, or
create execution contracts, project state, agent registries, handoff ledgers,
or run ledgers. Those are later infrastructure phases.
