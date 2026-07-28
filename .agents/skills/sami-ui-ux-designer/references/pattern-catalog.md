# SAMI enterprise UX pattern catalog

Use this catalog as selection guidance, not a mandatory component list.

## Navigation

- **Global sidebar/navigation drawer:** stable access to major modules; backend-driven and permission-filtered.
- **Breadcrumbs:** orientation in deep, hierarchical routes; avoid for shallow flows.
- **Tabs:** peer views within one context; avoid when steps must be sequential.
- **Stepper:** ordered, dependent stages with meaningful completion; avoid for short forms.
- **Master-detail:** rapid comparison and editing while preserving list context.
- **Command palette:** expert access to known actions; supplement rather than replace discoverable navigation.

## Data exploration

- **Data table:** dense comparison, sorting and bulk work.
- **Faceted filters:** multiple independent dimensions with visible active criteria.
- **Saved view:** repeated role/person-specific filter, sort and column configurations.
- **Tree:** genuine hierarchy; avoid for arbitrary grouping.
- **Kanban/board:** work progressing through a small set of meaningful states.
- **Timeline/activity feed:** chronological history, audit and collaboration.
- **Calendar:** date/time placement is the primary decision.

## Forms and editing

- **Inline edit:** fast low-risk edits with simple validation.
- **Dialog form:** short focused creation/editing without losing context.
- **Drawer form:** contextual medium-complexity work alongside a list/detail view.
- **Dedicated page:** complex, linkable, long-running or multi-section work.
- **Progressive disclosure:** reveal advanced controls only when relevant.
- **Dependent fields:** show and validate fields based on authoritative configuration.
- **Draft/autosave:** long work where loss is costly; make save state visible.

## Feedback and safety

- **Inline validation:** field-specific corrective guidance.
- **Banner/alert:** persistent page-level risk or blocking state.
- **Toast/snackbar:** transient confirmation that needs no decision.
- **Confirmation dialog:** consequential action with clear object and impact.
- **Undo:** immediately reversible low-risk action.
- **Progress/job center:** long-running import, export or batch processing.
- **Conflict resolution:** show stale version and recovery choices rather than overwriting.

## Dashboards and reporting

- **KPI card:** one decision-oriented metric with scope, unit, target and freshness.
- **Trend chart:** change over ordered time.
- **Bar chart:** categorical comparison.
- **Stacked bar:** composition plus total when both matter.
- **Table:** precise values and multi-attribute comparison.
- **Gauge/progress:** bounded target only; avoid for unbounded trends.
- **Exception panel:** prioritized items requiring action.
- **Drill-down:** preserve filter/scope context from summary to evidence.

## Pattern-selection dimensions

Evaluate:

- frequency: novice discovery versus expert speed;
- density: scanning versus focused comprehension;
- risk: reversible versus consequential;
- complexity: one decision versus staged workflow;
- context: preserve background versus deep focus;
- scale: few items versus large/virtualized datasets;
- device: pointer/keyboard versus touch;
- direction/language: RTL/LTR and text expansion;
- permissions: hidden, disabled, read-only or explainable denial;
- latency: immediate response versus asynchronous job.

## Anti-patterns

- modal chains or nested dialogs;
- color-only status and icon-only ambiguous actions;
- hidden save state or destructive defaults;
- dashboard decoration without decisions or drill-down;
- mobile layouts that retain unusable desktop tables;
- duplicate filters/actions implemented differently per module;
- hardcoded status, permission, menu or lifecycle behavior;
- placeholders presented as completed functionality.
