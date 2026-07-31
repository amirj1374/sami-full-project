# SAMI ERP Localization Completion Report

**Date:** 2026-07-31  
**Branch:** `codex/sami-final-consolidation`

## Root cause

The English and Persian catalogues already had identical key structures (924
leaf keys each), but backend-driven navigation used an incomplete
`server.module` map. Modules added by later migrations therefore fell back to
their English database names. Several administration and workflow screens also
rendered backend names or enum codes directly, and some Vuetify tables captured
translated headers only once during setup. Physical left/right spacing classes
also produced reversed spacing in RTL.

## Changes

- Added localized names for every module seeded by the backend migrations,
  including Automation, Scheduler, Data Quality, Files, Knowledge, Licensing,
  Metadata, Appointments, Portal, Calendar, Organization, and Communication.
- Added shared translations for permission actions, lifecycle states, execution
  states, quality statuses/severities, scheduler kinds, and common enum values.
- Extended `useServerLabel()` with reusable permission, action, and enum label
  resolution while preserving custom backend values as safe fallbacks.
- Localized backend-driven menu, breadcrumb, command-palette, placeholder,
  permission matrix, permission list, module lifecycle, profile role, customer,
  purchase, Automation, Scheduler, and Data Quality labels.
- Made Roles, Permissions, and Modules table headers reactive so switching the
  active language updates existing screens without a reload.
- Made navigation filtering search the translated menu label, allowing Persian
  users to search using Persian text.
- Replaced direction-specific Vuetify spacing utilities (`ml`/`mr`/`pl`/`pr`)
  with logical start/end utilities throughout Vue components.
- Added translated route-title fallbacks for dashboards, reports, KPI screens,
  viewer screens, and forbidden access.

The catalogues now contain 996 aligned leaf keys each: 72 synchronized entries
were added to each locale. No Persian-only or English-only keys remain.

## Files changed

- `sami-frontend/src/locales/en.json`
- `sami-frontend/src/locales/fa.json`
- `sami-frontend/src/composables/useServerLabel.ts`
- `sami-frontend/src/layouts/DefaultLayout.vue`
- Administration views for Modules, Permissions, Roles, and Users
- Automation, Scheduler, Data Quality, Dashboard, Customer, Profile, and KPI views
- Shared customer, supplier, purchase, user, dashboard, permission, chart, and KPI components

No backend API, persisted contract, permission rule, migration, or business
behavior was changed.

## Verification

| Check | Result |
|---|---|
| English/Persian key parity | PASS — 996 / 996 leaf keys; zero missing |
| Frontend type-check | PASS — `npm.cmd run type-check` |
| Production build | PASS — Vite 8.1.5, 940 modules |
| Whitespace/conflict check | PASS — `git diff --check` |
| Persian login rendering | PASS — `lang=fa`, `dir=rtl`, Persian labels |
| English login rendering | PASS — `lang=en`, `dir=ltr`, English labels |
| Backend-driven mock menu in Persian | PASS — translated menu labels, no raw codes |
| Mobile Persian layout (390 × 844) | PASS — no document overflow |
| Browser console errors | PASS — none observed in verified flows |

## Remaining limitations

- Administrator-created names and descriptions are user data, not catalogue
  strings. They remain exactly as entered. System-seeded values use the shared
  mappings; an unknown future backend code safely displays its backend label
  until a translation is added.
- Full real-backend role-by-role browser coverage requires a running database
  and representative authenticated accounts. Mock-auth navigation and public
  authentication flows were verified in both directions.
- The frontend has no configured lint or unit-test script; type-check,
  production build, static audits, and browser verification were used.
