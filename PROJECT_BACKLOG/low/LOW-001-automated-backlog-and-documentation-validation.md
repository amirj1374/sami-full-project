---
id: LOW-001
title: Automate backlog and documentation integrity checks
status: ready
priority: low
type: documentation
area: documentation
owners: []
depends_on: []
blocks: []
estimated_size: S
risk: low
source_refs: [PROJECT_BACKLOG, docs, .github/workflows/ci.yml]
---

# Automate backlog and documentation integrity checks

## Summary
Turn the read-only link, metadata, dependency and path checks used to create
this backlog into a maintained CI validation script.

## Why this is needed
Manual indexes can drift as tasks and documentation evolve.

## Current evidence
No repository documentation/backlog validation command exists.

## Existing implementation
Structured front matter, canonical indexes and CI workflow.

## Missing work and scope
A dependency-free checker for links, IDs, enums, metadata/index parity,
dependencies, cycles and referenced paths. Markdown style enforcement is optional.

## Dependencies
None.

## Business decisions required
None; select script runtime already available in CI.

## Proposed implementation approach
Add a small read-only script and CI step with focused fixtures/tests.

## Impacts
Documentation tooling and CI only.

## Security and tenant-isolation considerations
Do not print matched secret content; report paths only.

## Testing requirements
Fixtures for duplicate ID, missing dependency, cycle, broken link and bad enum.

## Documentation requirements
Document command and remediation output.

## Acceptance criteria
- [ ] Checker detects all required metadata/link/dependency failures.
- [ ] It has no new project dependency.
- [ ] CI runs it and output does not reveal secrets.

## Validation commands
Run checker against valid backlog and each invalid fixture.

## Risks and rollback considerations
Keep parsing narrow and documented to avoid false failures.

## Relevant files
`PROJECT_BACKLOG`, `docs`, CI.

## Notes for the next developer or AI agent
This item is ready because scope and acceptance criteria require no business decision.
