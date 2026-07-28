---
name: sami-frontend-builder
description: Build and review production-ready SAMI ERP frontend features using the repository's Vue 3, TypeScript, Vuetify, Pinia, routing, API, localization, RTL, and design-system conventions. Use for pages, forms, tables, dashboards, widgets, dialogs, drawers, routes, stores, API clients, responsive UI, accessibility, permissions, and frontend integration.
---

# SAMI Frontend Builder

Build requested frontend behavior by extending verified repository patterns. Do not treat the expected stack as proof that a library or convention exists; inspect the project first.

## Preconditions

1. Locate the frontend Git root and read every applicable `AGENTS.md`.
2. Inspect the active branch, worktree status, package manager, lockfile, scripts, TypeScript/Vite configuration and runtime environment contract.
3. Preserve unrelated and uncommitted user changes.
4. Distinguish implementation requests from review-only requests. Remain read-only for reviews.
5. Confirm required backend contracts from code or documentation. Do not invent API behavior or duplicate missing backend logic in the browser.

## Mandatory reuse discovery

Before designing or editing, search:

1. reusable components and company UI-kit wrappers;
2. composables;
3. utilities, formatters and helpers;
4. SCSS variables, CSS custom properties, Vuetify theme tokens and global styles;
5. established prop, slot, model and emit patterns;
6. comparable forms, dialogs, drawers, tables, filters and pagination;
7. permission directives/composables and forbidden states;
8. loading, skeleton, empty, error, retry and disabled states;
9. API clients, envelopes, interceptors, types and error normalization;
10. Pinia stores, router metadata, menu loading and feature-flag handling;
11. validation schemas and backend constraint mapping;
12. English/Persian localization and RTL-responsive patterns;
13. component, store, route and browser tests.

Inspect at least one comparable feature end to end. Before implementation, report the exact components, composables, utilities, styles, API/store patterns and interaction states that will be reused. Create a new abstraction only when no suitable implementation exists; explain that evidence and keep the abstraction narrowly scoped.

Use [references/frontend-checklist.md](references/frontend-checklist.md) as a requirement trace.

## Implementation rules

- Use Vue 3 Composition API and strict TypeScript.
- Follow the repository's existing `<script setup>` or component style; do not introduce a competing pattern.
- Reuse company UI-kit components and established Vuetify defaults, spacing, density, typography, colors and breakpoints.
- Follow current API-client, response-envelope, error, type and Pinia conventions.
- Keep backend business calculations, lifecycle decisions, authorization, feature flags and configuration on the backend.
- Reflect backend-driven statuses, types, permissions, menus and flags instead of hardcoding them.
- Do not add static menu entries when navigation is backend-driven. Follow the existing menu/module registration path.
- Do not infer module completion from a route, menu item, placeholder, mock or type definition.
- Avoid hardcoded user-facing strings, including labels, hints, errors, empty states, tooltips and accessibility text.
- Add English and Persian keys together and keep interpolation/plural behavior aligned.
- Use reactive/computed translations when locale changes must update existing UI.
- Respect document direction and logical layout. Verify icons, alignment, order, navigation and charts in RTL.
- Align client validation with known backend constraints while retaining server error handling as authoritative.
- Provide loading, failure, retry, empty, disabled, forbidden and stale/concurrent-operation states as applicable.
- Keep layouts responsive at established breakpoints and avoid fixed sizes that break translated text.
- Preserve keyboard access, focus behavior, labels, semantic structure, contrast and reduced-motion expectations.
- Revoke object URLs, cancel watchers/requests/timers and remove listeners when applicable.
- Preserve existing public props, emits, routes and API behavior unless a breaking change is explicitly required.

## Forms and mutations

- Reuse established validation libraries and form wrappers.
- Normalize optional/empty values exactly as existing API clients do.
- Disable duplicate submissions and expose progress.
- Map field and global server errors through existing error handling.
- Require explicit confirmation for destructive or irreversible actions.
- Refresh or reconcile state after success using the existing cache/store strategy.
- Enforce permissions server-side; UI permission checks only hide or disable unavailable actions.

## Routing and integration

- Use existing route naming, lazy loading, metadata, permission and navigation-history conventions.
- Preserve deep links, back behavior and query/filter serialization when relevant.
- Consume typed API clients instead of calling Axios directly from views when the project centralizes clients.
- Keep stores focused on shared state; do not move local component state into Pinia without an established need.
- Use the current development server port. If no port is configured, use `7474`; do not override an existing project setting.

## Verification

Discover scripts from `package.json` and use the repository's package manager and lockfile. Run, when applicable:

1. dependency installation only when needed;
2. `vue-tsc --noEmit` or the configured type-check script;
3. configured lint and frontend tests;
4. `npm run build` or the package-manager equivalent;
5. browser verification at representative desktop/mobile widths;
6. Persian and English flows, RTL/LTR layout and key parity;
7. browser console and network checks for errors, failed requests and contract mismatches.

Do not upgrade dependencies or regenerate a different package-manager lockfile. Record exact commands and outcomes. Report unavailable browser/backend checks honestly.

## Final report

Report:

- existing patterns and concrete pieces reused;
- files created and modified;
- routes and backend-driven menu integration;
- APIs consumed and contract assumptions;
- validation and interaction states implemented;
- English/Persian localization keys and RTL verification;
- type-check, lint, test, build and browser results;
- remaining limitations, missing backend evidence and manual steps.
