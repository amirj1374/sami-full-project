# Sales Menu Visibility Fix Report

## Outcome

The Sales module is visible and usable in the production build at `/sales`.
An authorized Persian user sees `فروش` in the dynamic menu. The same workspace
provides the Sales dashboard, invoice list, create and edit forms, detail dialog,
payments, returns, reports, exports, lost-sales workflow, audit history and
permission-scoped lifecycle actions.

## Root cause

The failure was in the backend-driven menu namespace, not in the Sales route or
page implementation:

1. `V28__sales.sql` transformed the original module row with code `orders` into
   the Sales module and replaced its permissions with `sales:*`, but retained the
   module code `orders`.
2. `MenuService` authorizes a module using `<module-code>:view`. It consequently
   checked `orders:view`, while authorized Sales roles held `sales:view`.
3. The frontend server-label resolver also received `orders`, so it rendered the
   legacy Orders label instead of Sales where the super-admin bypass made the row
   visible.

The Sales feature branch was already an ancestor of `codex/final-sami-release`.
It contained no unique unmerged Sales work. The active Vite processes were also
running from this main repository, so neither an unmerged worktree nor a wrong
working directory caused the problem.

During real browser verification, a second defect was found: persisted sale
details returned HTTP 500 because `SaleRepository.findByIdAndTenantId` used one
entity graph to join-fetch the `items`, `payments` and `services` list
associations. Hibernate rejects that as `MultipleBagFetchException`. Removing
that multi-list entity graph lets the already-transactional Sales service
initialize those collections safely.

The PWA cache was not the cause. Business API requests are network-only,
navigation is network-first, old shell cache versions are deleted on activation,
and production assets are fingerprinted. Verification also used fresh browser
contexts with service workers disabled, so a stale service worker could not mask
the result.

## Changes

- `sami-backend/src/main/resources/db/migration/V36__align_sales_module_namespace.sql`
  renames the existing transformed module registry code from `orders` to `sales`
  without replacing its stable row, permission IDs or role grants. It fails
  closed if a conflicting `sales` row exists or the expected transformed row is
  missing.
- `sami-backend/src/main/java/com/sami/app/sales/repository/SaleRepository.java`
  removes the invalid multi-list entity graph from the tenant-scoped detail
  lookup.
- `sami-backend/src/test/java/com/sami/app/sales/SalesTenantOperationsTest.java`
  adds a regression contract for the safe detail lookup.
- `sami-frontend/src/locales/en.json` and `sami-frontend/src/locales/fa.json`
  add the canonical `server.module.sales` label and retain a compatible legacy
  `orders` alias (`Sales` / `فروش`) for clients briefly connected to an older
  backend during rolling deployment.
- `sami-frontend/src/layouts/DefaultLayout.vue` adds the Sales route-title
  fallback.
- `sami-frontend/tests/release-contracts.test.mjs` prevents route, permission,
  migration and localization namespaces from diverging again.
- `.agents/SAMI_PROJECT_CONTEXT.md` refreshes the authoritative migration range
  through V36.

No authentication contract, public Sales API, tenant/company/branch scope,
business rule or historical migration was changed.

## Permissions

The menu and route require `sales:view`. Actions remain independently guarded by
the existing permissions:

- `sales:create`, `sales:edit`
- `sales:confirm`, `sales:complete`, `sales:cancel`
- `sales:payment`, `sales:return`
- `sales:approve-discount`
- `sales:report`, `sales:export`
- `sales:view-accounting`, `sales:view-audit`
- `sales:manage-lost-sales`

A non-super-admin audit role granted the existing Sales permissions received the
`sales | Sales | /sales` menu row after V36; before V36, the same role received no
Sales row. Super-admin behavior remains the repository's existing protected
bypass. The frontend continues to hide every action unless its exact permission
is present.

## Routes and workflows

The customer-facing direct URL is:

- `http://localhost:18080/sales`

The repository intentionally implements Sales as one responsive workspace, not
as placeholder or duplicate deep-link pages:

| Capability | UI location |
| --- | --- |
| Dashboard and Sales list | `/sales`, invoice tab |
| Create Sale | `/sales`, `فروش جدید` dialog |
| Edit Draft Sale | `/sales`, selected draft action |
| Sale details | `/sales`, selected invoice detail dialog |
| Payments | detail dialog payment tab and permission-scoped payment action |
| Returns | permission- and lifecycle-scoped return action |
| Reports | `/sales`, reports tab |

The corresponding API URLs remain under `/api/v1/sales`, including
`/dashboard`, `/{id}`, `/{id}/payments`, `/{id}/return`, `/reports` and
`/reports/export.csv`.

## Runtime verification

- Flyway applied V36 successfully; the live registry row is
  `sales | Sales | /sales | enabled | available | production-ready`.
- Backend and frontend production containers are healthy through nginx at
  `http://localhost:18080/health` (HTTP 200).
- Both fix images are `linux/amd64`.
- A fresh Persian desktop session displayed `فروش`, navigated through the menu to
  `/sales`, loaded the dashboard and two real draft invoices, opened the complete
  create form, submitted a real draft during verification, opened persisted sale
  details, and exposed edit/payment controls according to permissions.
- Persisted detail and accounting requests return HTTP 200 after the repository
  correction.
- At 390 x 844, the Sales workspace had no horizontal page overflow and the
  create form opened as a full-screen RTL dialog with customer, sale type, item,
  quantity, price, IMEI, service and action controls accessible.
- Desktop and mobile evidence was captured in
  `%TEMP%/sami-sales-desktop.png` and `%TEMP%/sami-sales-mobile.png`.

## Validation results

- Backend focused Sales tests: 4/4 passed.
- Backend full `mvn verify`: 196/196 passed, zero failures/errors; Spring Boot JAR
  packaging succeeded.
- Frontend tests: 7/7 passed.
- Frontend `npm run type-check`: passed.
- Frontend `npm run build`: passed with Vite 8.1.5; the production Sales chunk was
  emitted.
- Localization parity: exact English/Persian key parity passed.
- Static route/lazy import/RBAC seed contracts: passed.
- Browser desktop navigation and CRUD-surface verification: passed.
- Browser mobile RTL/responsive verification: passed at 390 x 844.
- Production Docker builds: passed for backend and frontend; both runtime
  containers are healthy.

## Git state

- Branch: `codex/final-sami-release`
- Pre-fix base commit SHA: `c129c827759841248777f43a22437d8c99d3d8ce`
- The fix was validated in production-equivalent images before integration.
- Its integration commit and the final consolidated branch SHA are recorded in
  `FINAL_BRANCH_CONSOLIDATION_AND_SALES_VISIBILITY_REPORT.md`.
- No push was performed.
