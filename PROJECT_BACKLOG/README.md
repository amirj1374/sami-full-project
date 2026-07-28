# SAMI ERP project backlog

This directory records evidence-based incomplete work. It was generated from
current source, migrations V1–V27, tests, CI, Docker/configuration, and
`docs/`. Backlog statements do not override source code, [AGENTS.md](../AGENTS.md),
or the canonical [technical documentation](../docs/README.md).

## Use

- Developers: select an item only after its dependencies and readiness are clear.
- AI agents: read AGENTS, docs, this index, then independently verify every
  `source_refs` entry before editing.
- Product owners: resolve entries in [OPEN_DECISIONS.md](OPEN_DECISIONS.md).
- Technical leads: maintain links and metadata when reprioritizing.

Priorities: **critical** protects security/isolation/data integrity;
**high** blocks the ERP core or production; **medium** improves substantial
quality/capability; **low** is non-blocking.

Statuses are limited to `proposed`, `needs-decision`, `ready`, `in-progress`,
`blocked`, `done`, and `cancelled`. Move an item to `completed/` only after its
acceptance criteria and [Definition of Done](DEFINITION_OF_DONE.md) are verified.

To add an item, copy [TASK_TEMPLATE.md](templates/TASK_TEMPLATE.md), choose the
next stable ID in its priority, add it to [BACKLOG_INDEX.md](BACKLOG_INDEX.md),
and validate dependencies. Status changes require evidence in the task notes;
`in-progress` requires an actual owner.
