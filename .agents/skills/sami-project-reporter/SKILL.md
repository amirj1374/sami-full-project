---
name: sami-project-reporter
description: Generate accurate business-facing and technical SAMI ERP overview, progress, roadmap, implemented-module, status-summary, and overview.md reports from actual repository evidence. Use for client reports, technical progress reports, module inventories, capability summaries, roadmaps, or project overviews.
---

# SAMI Project Reporter

Report from repository evidence only. Default to read-only; create or update `docs/overview.md` only when explicitly requested.

## Evidence collection

1. Read applicable `AGENTS.md`, existing documentation and lifecycle definitions.
2. Record frontend/backend roots, branches, commits and worktree status.
3. Inspect actual source trees, migrations, module registry, lifecycle metadata, entities, services, controllers, endpoints, frontend routes/views/clients, tests and build configuration.
4. Verify executable feature paths. Never treat menu scaffolds, placeholders, packages, migrations or planned DTOs as implementation.
5. Run safe configured inventory/build/test checks when requested or necessary; disclose unavailable checks.

## Module classification

Use repository-defined semantics when documented; otherwise apply:

- `ACTIVE`: verified end-to-end usable capability for its claimed scope;
- `BACKEND_READY`: verified backend/API with required persistence/security/tests, frontend not required or not ready;
- `FRONTEND_READY`: verified frontend implementation whose required backend contract is available, but end-to-end activation is incomplete;
- `IN_DEVELOPMENT`: meaningful incomplete implementation;
- `PLANNED`: documentation, registry, placeholder or scaffold only;
- `SCHEMA_ONLY`: tables/migrations without verified application implementation.

Do not mark schema-only work `BACKEND_READY`. Flag lifecycle metadata that overstates repository evidence.

## Report audiences

When requested, produce both:

### Client/business

- Write clear Persian unless another language is requested.
- Focus on business capabilities and value.
- Avoid unnecessary jargon.
- Separate available capabilities, in-progress work, planned work and limitations.
- Explain dependencies and risks honestly without exposing secrets or internal noise.

### Technical/development

Include exact repository evidence, migration range, entity/service/controller/API/test counts with counting method, dependencies, configuration, blockers, verification results and compatibility risks.

Do not mention AI generation.

## Documentation updates

Only when explicitly asked, create or update `docs/overview.md`. Preserve accurate existing content, reconcile stale claims, and avoid replacing useful sections blindly. Follow repository documentation style and localization conventions.

Recommended sections:

- Executive Summary
- Product Vision
- Current Architecture
- Implemented Business Modules
- Implemented Infrastructure Modules
- Capability Analysis
- Module Status Matrix
- Current Limitations
- Risks and Dependencies
- Recommended Next Modules
- Roadmap
- Client Value
- Technical Verification Appendix

## Final response

State:

- report audience and scope;
- repository roots, commits, migrations, source and tests used as evidence;
- module classification method;
- files changed only if explicitly requested;
- claims that remain unverified and why.
