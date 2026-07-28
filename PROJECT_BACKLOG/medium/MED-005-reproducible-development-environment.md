---
id: MED-005
title: Enforce a reproducible local development toolchain
status: proposed
priority: medium
type: technical-debt
area: developer-experience
owners: []
depends_on: []
blocks: []
estimated_size: S
risk: medium
source_refs: [README.md, docs/01-getting-started.md, sami-backend/pom.xml, sami-frontend/package.json, sami-frontend/Dockerfile]
---

# Enforce a reproducible local development toolchain

## Summary
Make Node, Java, Maven and Docker requirements machine-checkable and local
startup reproducible without relying on undocumented host state.

## Why this is needed
Java 21 is required but the inspected host had Java 17; Maven/Docker and
frontend dependencies were unavailable. No `.nvmrc`, engines field or Maven wrapper exists.

## Current evidence
Versions are documented/configured but only partially enforced by CI/container images.
The former CORS `5173` mismatch is already corrected in the current worktree.

## Existing implementation
Dockerfiles, Compose, CI setup-java/setup-node and environment examples.

## Missing work and scope
Select version files/wrapper/dev-container policy, verify setup paths and add a
non-mutating doctor command. Dependency upgrades are out of scope.

## Dependencies
None.

## Business decisions required
Supported host platforms and preferred Docker versus native workflow.

## Proposed implementation approach
ADR; pin Node policy; add Maven Wrapper or require Maven explicitly; create
read-only environment diagnostics and CI consistency check.

## Impacts
Developer tooling/documentation/CI; no runtime business behavior.

## Security and tenant-isolation considerations
Doctor output must redact environment values.

## Testing requirements
Clean Windows/Linux setup validation and version-mismatch tests.

## Documentation requirements
Update getting-started and troubleshooting with executed results.

## Acceptance criteria
- [ ] Supported tool versions are machine-readable.
- [ ] Clean setup succeeds on supported platforms.
- [ ] Diagnostics fail clearly without printing secrets.
- [ ] CI and Docker use the same supported major versions.

## Validation commands
Environment doctor, backend verify and frontend install/type-check/build.

## Risks and rollback considerations
Version enforcement can disrupt developers; announce and provide migration steps.

## Relevant files
Root/backend README, CI, pom, package metadata and Dockerfiles.

## Notes for the next developer or AI agent
Do not claim a setup path works until executed on a clean environment.
