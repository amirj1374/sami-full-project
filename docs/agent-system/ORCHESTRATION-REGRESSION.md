# Orchestration Continuation Regression

This check is a durable execution rule, not SAMI business truth.

Given an ACTIVE `PHASE` or `WAVE` authorization, a completed child Step, and a
dependency-valid next Step:

1. record the completed Step's evidence and DoD verdict;
2. commit/push when the Contract permits and gates pass;
3. activate the next Step and Contract;
4. continue execution in the same run;
5. do not emit an intermediate user-facing completion report.

The only terminal outcomes are parent-scope DoD completion, a genuine
Owner-level blocker, or explicit Owner interruption/reprioritization. Ordinary
technical failures remain routed execution work.

Evidence locations: `AGENTS.md`, `docs/agent-system/GOVERNANCE.md`,
`.agents/skills/orchestrate-authorized-work/SKILL.md`, and the active
`PROJECT-STATE.md`/run artifact.
