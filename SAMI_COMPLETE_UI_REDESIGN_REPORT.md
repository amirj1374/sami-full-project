# SAMI Complete UI Redesign Report

**Date:** 2026-07-31  
**Branch:** `codex/final-sami-release`  
**Direction:** Operational Clarity

## Outcome

SAMI now uses a cohesive premium enterprise workspace rather than a collection
of default administrative screens. The redesign preserves Vue 3, TypeScript,
Vuetify, Pinia, Vue Router, Axios, real backend contracts, backend-driven menu
and permissions, validation, English/Persian localization, and RTL/LTR behavior.

The selected **Operational Clarity** direction balances executive confidence
with the density required for prolonged ERP work: an authoritative navigation
anchor, quiet canvas, crisp operational surfaces, restrained brass accents,
clear typography, predictable actions, and low-distraction motion.

## Design Exploration

Three grounded directions were evaluated and recorded in
`sami-frontend/.21st/design.json`:

1. **Operational Clarity — selected.** Dark navigation anchor, quiet workspace,
   strong hierarchy, comfortable enterprise density.
2. **Executive Canvas — rejected.** Attractive for presentations but too
   spacious for frequent operational tasks.
3. **Dense Command Center — rejected.** Fast scanning for experts but excessive
   cognitive load for mixed-experience users and small screens.

The official 21st CLI initialized the project design context. Catalog search
was attempted but requires an authenticated 21st account, so no external React
or shadcn component was copied into the Vue/Vuetify application.

## Redesigned Areas

### Design system

- Refined the light palette into a quiet cool-gray canvas, crisp white surfaces,
  authoritative navy controls, and restrained brass accents.
- Designed the dark palette independently using deep ink backgrounds and lifted
  navy surfaces instead of mechanically inverting the light theme.
- Expanded shared tokens for content width, top-bar height, navigation surfaces,
  spacing, radii, charts, shadows, and functional motion.
- Improved default cards, buttons, form controls, chips, dialogs, tables,
  pagination, snackbars, menus, and focus states.
- Preserved reduced-motion support and accessible visible focus.

### Application shell

- Rebuilt the desktop navigation as a high-contrast enterprise anchor while
  preserving backend-provided modules and permissions.
- Replaced the letter avatar with the existing SAMI application mark.
- Improved active navigation hierarchy, icons, favorites, recents, rail mode,
  menu search, breadcrumbs, global command search, theme controls, language,
  notifications, branch context, and profile access.
- Added a dynamic mobile bottom navigation sourced from the authorized backend
  menu. The final action opens the complete drawer; no static module links were
  introduced.
- Added translucent but restrained top-bar treatment and consistent maximum
  workspace width for large monitors.

### Authentication

- Rebuilt authentication into a premium split experience on large screens with
  product positioning, capability summaries, and an abstract ERP workspace
  preview.
- Kept login, registration, forgotten-password, reset-password, and
  change-password API behavior and validation unchanged.
- Added light/dark switching directly to the authentication shell.
- Preserved the compact bottom-sheet-like mobile card experience, password
  visibility controls, autocomplete attributes, server errors, and loading
  feedback.

### Dashboard

- Added an operational command surface with a localized date, personalized
  greeting, concise workspace description, and permission-aware links to real
  Customers, Purchases, and Products screens.
- Retained real backend KPIs, purchase status, purchase trend, recent purchases,
  and low-stock data. No decorative or fabricated business metric was added.
- Improved card hierarchy, responsive action wrapping, mobile density, status
  scanning, and visual separation between overview and detailed evidence.

### Shared UI and module consistency

- Upgraded `AppPageHeader` into a stronger shared module identity with a
  high-contrast icon marker and better mobile action layout.
- Refined `AuthCard` hierarchy and brand treatment.
- Standardized operational cards, filters, data surfaces, dialogs, table
  headers/rows, pagination, menus, snackbars, forms, validation and hover states
  globally.
- Redesigned `PlaceholderView` so planned or backend-only modules communicate
  lifecycle status honestly and professionally rather than appearing broken.
- Existing Products, Customers, Suppliers, Purchases, Dashboards, Users, Roles,
  Permissions, Modules, Automation, Licensing, Scheduler, Data Quality, Files,
  Appointments, Profile, KPI and reporting screens inherit the new system
  without changing their API or business behavior.

## Components Replaced or Refined

- `DefaultLayout.vue`: navigation, header, responsive shell, mobile navigation.
- `AuthLayout.vue`: split enterprise authentication shell and theme access.
- `AppPageHeader.vue`: module hierarchy and responsive action treatment.
- `AuthCard.vue`: authentication surface and brand hierarchy.
- `DashboardView.vue`: operational command surface and quick navigation.
- `PlaceholderView.vue`: honest module lifecycle presentation.
- `vuetify.ts` and `global.css`: shared theme and cross-application component
  presentation.

No competing component framework, Tailwind/shadcn layer, React package,
animation dependency, or duplicate application architecture was introduced.

## Localization and RTL

- Added matching English and Persian content for the authentication product
  story and dashboard command surface.
- English/Persian key parity passed with 1,188 leaf keys in each locale.
- Live English LTR and Persian RTL switching passed.
- Direction-sensitive navigation active markers, authentication preview angle,
  content alignment, menu order, and mobile navigation were verified.

## Mobile Improvements

- Added an app-like bottom navigation with touch-friendly targets and safe-area
  positioning.
- Preserved drawer access for the complete permission-filtered module list.
- Reserved bottom safe-area space so content is never obscured by navigation.
- Maintained fullscreen mobile dialogs, sticky dialog actions, responsive cards,
  stacked page actions, readable forms, and mobile table alternatives.
- Verified no horizontal document overflow at 360, 375, 390, 412, and 430px.

## Verification

- `npm run type-check` — passed.
- `npm run build` — passed; Vite transformed 965 modules.
- Locale parity — passed; English 1,188 / Persian 1,188.
- `git diff --check` — passed.
- 21st review — scanned 66 files. Its sole error is a false positive on a JavaScript
  API comment containing the phrase “plain `<img>` tags”; no image element is
  missing an `alt` attribute. Remaining findings are informational color-token
  suggestions.
- Browser: light and dark theme switching passed.
- Browser: English LTR and Persian RTL passed.
- Browser: Dashboard, Products, Customers, Purchases, Automation, Licensing,
  Data Quality, Files, and Appointments passed responsive shell checks.
- Browser console: no errors.

## Remaining Limitations

- Sales, Inventory, Repairs, Warranty, and several other roadmap modules do not
  have verified executable frontend/backend workflows. No fake pages, data, or
  API contracts were created. They receive the redesigned lifecycle placeholder
  when exposed by backend module metadata.
- Metadata, Knowledge, Communication, and Portal have varying backend/frontend
  completeness. This redesign does not reinterpret their lifecycle status or
  create missing business contracts.
- Authentication pages were verified through source, type-check, and production
  build; the active authenticated browser session redirected guest-only routes,
  so destructive session logout was not used solely for visual testing.
- 21st catalog search was unavailable without login. Project implementation used
  the generated design context and existing Vue/Vuetify primitives.
- `21st-design-sync` was assessed but not executed: it only publishes
  shadcn/Tailwind CSS-variable themes publicly, SAMI uses Vuetify tokens, and the
  user did not authorize public theme publication or provide the required API
  key.

## Scope and Repository Safety

No backend source, database migration, API contract, security rule, permission,
tenant behavior, audit behavior, or business logic was changed. Existing
unrelated worktree modifications were preserved. Nothing was staged, committed,
pushed, or published externally.

