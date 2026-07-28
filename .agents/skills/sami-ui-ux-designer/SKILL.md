---
name: sami-ui-ux-designer
description: Design, review, and improve production-grade SAMI ERP user experiences using appropriate enterprise UI, interaction, navigation, form, data-table, dashboard, workflow, responsive, accessibility, localization, and RTL patterns. Use when asked for UI/UX, product design, screen flows, wireframes, design systems, usability reviews, information architecture, visual hierarchy, interaction patterns, or frontend experience decisions.
---

# SAMI UI/UX Designer

Design task-efficient enterprise experiences, not decorative mockups. Select patterns from user goals, data density, frequency, risk and device constraints. Do not apply fashionable patterns without a functional reason.

## Start from repository context

1. Read applicable `AGENTS.md` and use `$sami-project-context` when available.
2. Inspect existing layouts, components, theme tokens, typography, spacing, icons, breakpoints, forms, tables, navigation, localization and RTL behavior.
3. Identify current design-system rules and reusable components before proposing new ones.
4. Map backend-driven permissions, lifecycle states, configuration, validation and workflows that affect the experience.
5. Distinguish design-only requests from implementation. Do not edit code unless explicitly asked; use `$sami-frontend-builder` for implementation.

## Frame the experience

Define:

- primary personas, expertise and permissions;
- jobs-to-be-done and success criteria;
- task frequency, urgency, risk and reversibility;
- required information, decisions and handoffs;
- desktop, tablet and mobile contexts;
- Persian/English content and RTL/LTR implications;
- loading, empty, partial, error, offline, forbidden and concurrent states.

When evidence is missing, state assumptions and make them easy to revise.

## Select patterns deliberately

Read [references/pattern-catalog.md](references/pattern-catalog.md) when choosing interaction patterns. For every significant choice, state:

- problem being solved;
- chosen pattern;
- why it fits;
- rejected alternatives and trade-offs;
- behavior across roles, states, screen sizes and directions.

Use familiar enterprise conventions for frequent work. Optimize for recognition, scanability, keyboard efficiency, error prevention and recovery.

## Information architecture

- Organize around business tasks and domain language rather than database tables.
- Keep global, module, contextual and in-record navigation distinct.
- Preserve location and context with clear titles, breadcrumbs or history where depth warrants them.
- Use progressive disclosure for advanced or infrequent controls.
- Keep filters, search, saved views and bulk actions predictable across modules.
- Never create static navigation when menus and permissions are backend-driven.

## Data-heavy ERP interfaces

- Choose table, list, cards, tree, board, timeline, calendar or dashboard based on comparison and action needs.
- Favor tables for dense comparison; preserve readable row identity and stable columns.
- Separate selection from navigation and destructive actions.
- Support sorting, filtering, pagination, column preferences, export and bulk operations only when task evidence requires them.
- Keep status representation text-accessible; never communicate meaning with color alone.
- Design dashboards around decisions and exceptions, not maximum chart count.
- Show freshness, units, scope, thresholds and drill-down affordances for KPIs.

## Forms and workflows

- Match form structure to mental models and business stages.
- Use single-page forms for short cohesive tasks; use sections, steppers or master-detail flows only when complexity justifies them.
- Prefer safe defaults and constrained inputs over instructions.
- Validate close to the field while preserving authoritative server errors.
- Distinguish draft, save, submit, approve, reject, cancel, archive and delete.
- Make irreversible actions explicit and require proportional confirmation.
- Preserve entered data on recoverable failures.
- Show ownership, status, next action and blockers in multi-stage workflows.

## Feedback and state

Design explicit:

- initial/loading/skeleton states;
- empty-first-use versus empty-filter-result states;
- inline validation and global failures;
- success acknowledgment without unnecessary interruption;
- disabled controls with a discoverable reason;
- permission-denied and read-only states;
- optimistic update, conflict and stale-data recovery;
- long-running job progress and resumable outcomes.

Use modal dialogs for focused blocking decisions, drawers for contextual work that benefits from preserved background context, and pages for deep or linkable workflows.

## Visual and interaction quality

- Use the existing SAMI/Vuetify token system; do not invent isolated colors, shadows or spacing.
- Establish hierarchy through layout, typography and spacing before decoration.
- Maintain consistent density appropriate to ERP use.
- Keep touch targets, focus indicators, contrast and keyboard order accessible.
- Use semantic labels, descriptive actions and accessible chart/table alternatives.
- Respect reduced motion and avoid animation that delays frequent tasks.
- Avoid dark patterns, hidden destructive actions and ambiguous icons.

## Localization, Persian and RTL

- Design with real Persian and English strings, not equal-length placeholders.
- Use logical start/end alignment and direction-aware icons.
- Keep numbers, currency, dates, codes and mixed-direction identifiers legible.
- Verify tables, charts, steppers, breadcrumbs, drawers and navigation in both directions.
- Avoid hardcoded user-facing copy; specify localization keys/content intent.

## Responsive strategy

Do not merely shrink desktop layouts:

- preserve the primary task and critical data;
- collapse secondary controls progressively;
- provide explicit mobile filter/action surfaces;
- transform dense tables only when comparison remains possible;
- keep dialogs, drawers and virtual keyboards usable;
- define behavior at existing project breakpoints.

## Deliverables

Produce the smallest useful artifact:

- UX audit with severity and evidence;
- user/task flow;
- information architecture;
- annotated wireframe or layout specification;
- component/state matrix;
- responsive and RTL behavior;
- interaction and validation rules;
- accessibility acceptance criteria;
- implementation handoff mapped to existing components/tokens.

For implementation handoff, report:

- patterns selected and rationale;
- existing components/tokens to reuse;
- page structure and responsive behavior;
- all states and role/permission variations;
- Persian/English and RTL considerations;
- accessibility requirements;
- open assumptions and usability risks.
