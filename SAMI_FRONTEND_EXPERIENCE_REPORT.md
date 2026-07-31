# SAMI Frontend Experience Report

## Executive summary

The Frontend Excellence Sprint consolidated the existing SAMI theme work into a
more complete enterprise experience layer. It added reusable empty, loading,
error, notification, and audit-history primitives; standardized legacy
administration pages; introduced a real profile/preferences page; improved
mobile forms and dialogs; added safe installable-app metadata; and repaired
mock-mode contract failures that previously crashed major demo pages.

No backend business logic, dependency, migration, commit, push, or deployment
was performed.

## Improvements completed

### Visual consistency

- Retained and extended the centralized light/dark SAMI design tokens.
- Products, Customers, Suppliers, Purchases, Users, Roles, Permissions, and
  Modules now share `AppPageHeader` and `app-data-surface`.
- Global table, form, dialog, menu, card, state, focus, RTL, and responsive
  rules remain the visual source of truth.

### Empty-state system

`AppEmptyState` now supports:

- empty data;
- empty search;
- missing permission;
- unavailable feature;
- first-time use;
- primary and secondary actions;
- compact and mobile presentation.

The 404 page now uses this system with home and back recovery actions.

### Error experience

`useApiError` now maps stable error codes to localized, user-friendly messages:

- authentication/session expiry;
- permission denial;
- missing information;
- conflict/stale version;
- validation;
- server failure;
- network failure;
- timeout;
- unknown failure.

Raw Axios and server messages are no longer the default user presentation.
Backend field errors remain exposed to forms. `AppErrorState` provides a
consistent retry surface and is applied to Scheduler.

### Loading experience

`AppLoadingState` provides page, table, card-grid, and detail skeleton patterns.
Scheduler now uses record-card skeletons instead of only an indeterminate bar.
Existing dashboard skeletons remain in place.

### Notifications

Added an application-level notification queue and host supporting success,
error, warning, information, loading, action/retry, timeout, and dismissal.
Change-password success now uses the global host instead of a local snackbar.

### Audit timeline

Added `AppAuditTimeline`, a typed renderer for real caller-provided creation,
update, status, approval, and permission events. It does not create or infer
events. Existing feature-specific audit APIs can adopt it incrementally.

### Profile and identity

Added `/profile`, reachable from the account menu, with:

- authenticated identity and email;
- role;
- membership date;
- permission count or administrator access;
- light/dark/system theme selection;
- English/Persian language and direction selection;
- existing supported change-password workflow.

Unsupported profile mutation and invented session APIs were not added.

### Mobile forms and dialogs

- Dialog action rows are sticky below 600px.
- Actions respect bottom safe areas and remain at least 44px high.
- Mobile theme selection becomes a vertical control group.
- Existing fullscreen form behavior and coarse-pointer 52px inputs remain.

### PWA/mobile polish

Added:

- a valid web manifest;
- standalone display metadata;
- mobile-web-app capability metadata;
- theme and color-scheme metadata.

A service worker, offline behavior, and icons were not invented. Those require
approved assets and a separately tested caching policy.

### Demo and first impression

Explicit MSW catalogue handlers now return the same array/object/page shapes as
the frontend contracts. This removed runtime crashes in Customers, Suppliers,
Purchases, Users, Permissions, Automation, Scheduler, and Data Quality while
keeping demo content empty rather than fabricating business data.

## Components created

- `AppLoadingState.vue`
- `AppErrorState.vue`
- `AppNotificationHost.vue`
- `AppAuditTimeline.vue`
- `useNotifications.ts`

## Components improved

- `AppEmptyState.vue`
- `ChangePasswordDialog.vue`
- `App.vue`

## Pages improved

- Authentication suite and Dashboard (preserved earlier sprint work)
- Products
- Customers
- Suppliers
- Purchases
- Users
- Roles
- Permissions
- Modules
- Automation
- Scheduler
- Data Quality
- Forbidden
- Not Found
- Profile and Preferences (new)

## Mobile issues fixed

- Inconsistent legacy page headers and action wrapping.
- Missing sticky dialog actions.
- Single-action-only empty states.
- Spinner/bar-only Scheduler loading.
- Profile theme selector overflow/density.
- Demo-mode route crashes caused by incorrect mock response shapes.

No document-level overflow was detected across the audited production routes at
360, 375, 390, 412, or 430px.

## UX issues fixed

- Technical errors reaching user-facing alerts.
- No global retry-capable notification owner.
- Inconsistent 404 recovery.
- No consolidated profile/preferences surface.
- Inconsistent admin page hierarchy.
- Mock-mode crashes undermining demo usability.

## Performance review

- Routes remain lazy-loaded.
- The new Profile route is a separate production chunk.
- No dependency or eager feature bundle was added.
- Shared experience components replace repeated future page implementations.
- Production build transformed 931 modules.

The largest current production costs remain Vuetify/global CSS, MDI font assets,
MSW browser code when mock mode is bundled, and existing feature page chunks.
Dependency or icon-system changes were not made because they would expand
architecture and deployment scope.

## Validation results

| Check | Result |
|---|---|
| `npm.cmd run type-check` | PASS |
| `npm.cmd run build` | PASS |
| Localization parity | PASS: English 918 / Persian 918 |
| `git diff --check` | PASS; line-ending notices only |
| Responsive route audit | PASS: no document overflow |
| Mock-mode console recheck | PASS: no errors on repaired routes |

Responsive browser inspection covered login, dashboard, products, customers,
suppliers, purchases, users, roles, permissions, modules, Automation,
Scheduler, Data Quality, Profile, and 404 at representative mobile and desktop
widths. Dark Persian/RTL and light English/LTR were exercised during the
combined theme and experience passes.

## Files created in this sprint

- `sami-frontend/src/components/AppLoadingState.vue`
- `sami-frontend/src/components/AppErrorState.vue`
- `sami-frontend/src/components/AppNotificationHost.vue`
- `sami-frontend/src/components/AppAuditTimeline.vue`
- `sami-frontend/src/composables/useNotifications.ts`
- `sami-frontend/src/views/ProfileView.vue`
- `sami-frontend/public/manifest.webmanifest`
- `SAMI_FRONTEND_EXPERIENCE_REPORT.md`

## Files modified in this sprint

- `sami-frontend/index.html`
- `sami-frontend/src/App.vue`
- `sami-frontend/src/components/AppEmptyState.vue`
- `sami-frontend/src/components/ChangePasswordDialog.vue`
- `sami-frontend/src/composables/useApiError.ts`
- `sami-frontend/src/layouts/DefaultLayout.vue`
- `sami-frontend/src/locales/en.json`
- `sami-frontend/src/locales/fa.json`
- `sami-frontend/src/mocks/handlers.ts`
- `sami-frontend/src/router/index.ts`
- `sami-frontend/src/styles/global.css`
- `sami-frontend/src/views/NotFoundView.vue`
- `sami-frontend/src/views/SchedulerView.vue`
- `sami-frontend/src/views/admin/RolesView.vue`
- `sami-frontend/src/views/admin/PermissionsView.vue`
- `sami-frontend/src/views/admin/ModulesView.vue`

Previously dirty theme/module files were preserved.

## Remaining problems

### High

- File Management, Knowledge/SOP, Licensing, Metadata, and Appointments still
  require full backend-supported frontend modules.
- Existing dense tables use intentional contained scrolling on mobile; the most
  frequent workflows should receive dedicated record-card views.

### Medium

- Global notifications are established but older local snackbars should migrate
  incrementally when their pages are next modified.
- `AppAuditTimeline` is not yet wired into every available history API.
- Advanced filter bottom sheets and persisted filters are not yet consistent
  across all data pages.
- No automated unit, component, browser, accessibility, or contrast test stack
  is configured.
- The manifest has no approved product icons and no offline/service-worker
  strategy.

### Low

- Some older dialog padding and page-specific radius declarations remain.
- Chart components should consume the new semantic chart tokens consistently.

## Recommended next steps

1. Implement File Management with the refined theme and shared state primitives.
2. Implement Knowledge/SOP, Licensing, Metadata, and Appointments.
3. Add workflow-specific mobile record cards and filter sheets to Customers,
   Suppliers, Purchases, and Users.
4. Wire `AppAuditTimeline` to verified user/customer/automation history APIs.
5. Migrate remaining local snackbars to the global notification host.
6. Add a separately approved Vitest/Playwright/axe quality infrastructure slice.
7. Add approved application icons and test an explicit PWA caching strategy.
