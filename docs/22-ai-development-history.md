# AI Development Policy History

This append-only record explains meaningful changes to SAMI's version-controlled
AI development policy. Future entries should preserve earlier rationale.

## 2026-07-28 — Permanent repository development workflow

### Added

- Mandatory read-only preparation and approval gate
- Source-first, evidence-backed module inspection
- Ordered reuse-first repository search
- Explicit justification for new architectural artifacts
- Smallest-correct-change and scope-expansion policy
- Repository consistency as an acceptance criterion
- Cross-cutting impact analysis
- Version-controlled AI knowledge policy
- Required implementation and validation reporting

### Reason

Prevent architecture drift, duplicate implementations, uncontrolled scope
expansion, inconsistent behavior across AI sessions and machines, and
undocumented workflow changes.

### Files affected

- `AGENTS.md`
- `docs/21-ai-agent-guide.md`
- `docs/22-ai-development-history.md`
- `PROJECT_SETUP_AND_DEPLOYMENT.md`

The root operational guide was added in the same documentation change so future
agents and operators use repository-confirmed run and deployment commands.

## 2026-07-29 — Evolving business requirements

### Added

- Simplest-design preference that keeps future approved changes inexpensive
- Separation of changeable business policy from technical infrastructure
- Configuration and extension where change is reasonably expected
- Documented, revisitable assumptions
- Impact-based escalation for consequential unresolved decisions
- Progress-first execution: use engineering judgment for ordinary implementation
  details and request approval only for material security, contract, migration,
  externally observable behavior or long-term architecture decisions

### Reason

Allow changing customer and product requirements to be incorporated without
major replacement work while preserving the smallest-correct-change rule and
preventing speculative functionality or unnecessary abstractions.

### Files affected

- `AGENTS.md`
- `docs/21-ai-agent-guide.md`
- `docs/22-ai-development-history.md`
