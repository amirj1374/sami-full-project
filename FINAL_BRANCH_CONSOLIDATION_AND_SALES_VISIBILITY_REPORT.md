# Final Branch Consolidation and Sales Visibility Report

## Final integration state

- Final branch: `codex/final-sami-release`
- Validated implementation SHA: `93e89c0da9813f009ec3bc6ecc521074f957f9b5`
- Sales integration commits created by this audit:
  - `2a9ae7fe4f9f6f90808137844a7e85f53530e8ae` — `fix(sales): restore menu visibility and detail access`
  - `93e89c0da9813f009ec3bc6ecc521074f957f9b5` — `fix(sales): respect report permission on list`
- Push/deployment: not performed.

The implementation SHA above is the exact revision used for the final backend and frontend images and runtime verification. The documentation-only commit containing this report necessarily has a later SHA; that final branch SHA is reported by `git rev-parse HEAD` after the report commit and in the task handoff.

## Repository inventory and consolidation

`git fetch origin --tags`, local and remote ref enumeration, worktree enumeration, status checks, stash inspection, reflog inspection, merge-base checks, branch divergence checks and unreachable-object inspection were performed before integration.

| Branch/ref | Inspected tip | Result relative to final branch before this report |
| --- | --- | --- |
| `codex/crm-complete` | `730ad50734e0256b405ab883ca4934e9a5dbafe2` | Contained; 0 unique commits |
| `codex/feature-complete-sales` | `56562d9c96b97aa7dabb0fa2c18b4e0ac828e8dc` | Contained; 0 unique commits |
| `codex/feature-core-infrastructure` | `b41432d1ba1e637aeb9981f6f6b74e2e6a5cd72e` | Contained; 0 unique commits |
| `codex/inventory-complete` | `311e7a691e046f38ff91dab7f140313baf650c7c` | Contained; 0 unique commits |
| `codex/licensing-complete` | `8eae996a957953ecec81d1e0038821a5a3150b37` | Contained; 0 unique commits |
| `codex/purchasing-complete` | `514b20af64e13f83a9916c1307f18c7bb00a16c9` | Contained; 0 unique commits |
| `codex/sales-complete` | `b0ddb217a0f77c211fedc2d78070c2a9f491f2d2` | Contained; 0 unique commits |
| `codex/sami-final-consolidation` | `140d56787b387eccf56951a969c6a40202926b12` | Contained; 0 unique commits |
| `development`, `origin/development` | `dc97b764ffc67cb7cb6b3c887f5a8de8bc3d1c66` | Contained; 0 unique commits |
| `origin/HEAD` | `origin/development` | Inspected; no separate work |

The final branch history already contained the completed core infrastructure, Automation, Licensing, both Sales implementation lines, Inventory, Purchasing, CRM, UI/UX, localization, PWA/push foundation, export encoding, Docker and release-hardening work. Effective source probes confirmed those features in the final tree; no incomplete branch was blindly merged and no additional branch-only implementation required recovery.

No merge conflict occurred during this audit. The two fixes above were made directly on the final integration branch after verifying the current source and history.

### Worktrees

| Worktree | Branch | Status | Unique work | Disposition |
| --- | --- | --- | --- | --- |
| `sami-full-project` | `codex/final-sami-release` | Clean before this report | Final integration work | Keep as active worktree |
| `sami-core-infrastructure-worktree` | `codex/feature-core-infrastructure` | Clean | None; branch is contained | Safe to remove later |
| `sami-licensing-worktree` | `codex/licensing-complete` | Clean | None; branch is contained | Safe to remove later |
| `sami-sales-worktree` | `codex/feature-complete-sales` | Clean | None; branch is contained | Safe to remove later |

No secondary worktree must remain to protect unique commits or uncommitted files. They were deliberately retained because this task did not authorize deletion.

### Preserved local work

- No worktree had uncommitted tracked or untracked changes.
- `stash@{0}` (`local: preserve demo seed preference before stabilization`) remains untouched. It preserves the local `application-dev.yml` demo-seed preference and was not applied or deleted.
- Reflog/unreachable commit review found only superseded Automation WIP, the stash index object and a pre-amend report revision; none contained valid missing implementation.
- No reset, clean, force operation, branch deletion, stash deletion, push or deployment was performed.

## Sales implementation verified

The final branch contains the Sales controllers, service, DTOs, entities, repositories, tenant-scoped queries, audit history, domain events, lifecycle transitions, inventory integration, payments, returns, accounting, lost-sales support, reports, CSV export, permissions and Flyway schema. It also contains the Sales API client and types, `/sales` route, dynamic menu integration, responsive workspace, dashboard metrics, invoice list, create/edit form, detail/payment/return/report UI, permissions, and exact English/Persian localization parity.

Representative implementation owners include:

- `sami-backend/src/main/java/com/sami/app/sales/`
- `sami-backend/src/main/resources/db/migration/V28__sales.sql`
- `sami-backend/src/main/resources/db/migration/V36__align_sales_module_namespace.sql`
- `sami-frontend/src/api/sales.ts`
- `sami-frontend/src/types/sales.ts`
- `sami-frontend/src/views/SalesView.vue`
- `sami-frontend/src/components/SaleActionPanel.vue`
- `sami-frontend/src/components/SalesReportsPanel.vue`
- `sami-frontend/src/components/LostSalesPanel.vue`

## Root causes and exact fixes

### Missing menu

`V28__sales.sql` converted the legacy Orders module row to Sales and replaced its permission namespace with `sales:*`, but retained module code `orders`. `MenuService` derives menu authorization as `<module-code>:view`, so a normal role with `sales:view` was checked against nonexistent `orders:view`. A super administrator could bypass that check, but received the legacy module-code label.

`V36__align_sales_module_namespace.sql` now changes the existing row from `orders` to `sales` in place, preserving its stable ID and role grants. It fails closed when the expected transformed row is missing or a conflicting `sales` row already exists. English and Persian `server.module.sales` labels were added, the legacy label remains as rolling-deployment compatibility, and route-title fallback now recognizes Sales.

### Persisted detail failure

The repository detail query used one entity graph to fetch the `items`, `payments` and `services` list associations. Hibernate rejects simultaneous join fetching of multiple bags with `MultipleBagFetchException`. The invalid entity graph was removed; the transactional service now initializes the collections safely. A focused regression test protects the tenant-scoped detail lookup.

### View-only permission failure

`SalesView.load()` previously requested both the invoice list (`sales:view`) and dashboard metrics (`sales:report`) unconditionally. A legitimate user with only `sales:view` therefore received an unrelated 403 while opening the list. Dashboard loading is now conditional on `sales:report`. A release-contract test protects the namespace and permission behavior.

No public authentication contract, Sales API contract, business rule, tenant/company/branch authority, or historical migration was rewritten.

## Permissions and menu registration

- Menu and route: `sales:view`
- Creation/edit: `sales:create`, `sales:edit`
- Lifecycle: `sales:confirm`, `sales:complete`, `sales:cancel`
- Financial actions: `sales:payment`, `sales:return`, `sales:approve-discount`
- Reporting/export: `sales:report`, `sales:export`
- Protected detail: `sales:view-accounting`, `sales:view-audit`
- Lost sales: `sales:manage-lost-sales`

The live V36 module registry row is `sales | Sales | /sales`, enabled and production-ready. A release-only non-super-admin database role with exactly `sales:view` saw the Persian Sales menu and list, while create, export, report and lost-sales controls stayed hidden. The temporary role and browser fixture exist only in the isolated validation database, not in migrations or source.

## Exact Sales URLs

The repository intentionally uses one responsive Sales workspace; create, edit and detail are permission/lifecycle-scoped dialogs and reports are a workspace tab rather than duplicate routes.

| Capability | Direct URL / interaction |
| --- | --- |
| Dashboard and invoice list | `/sales` |
| Create Sale | `/sales` → `New sale` / `فروش جدید` |
| Edit draft | `/sales` → invoice details → edit draft |
| Sale details | `/sales` → invoice details |
| Payments | `/sales` → invoice details → payments/action |
| Returns | `/sales` → completed invoice details → return action |
| Reports | `/sales` → reports tab |

The public API remains under `/api/v1/sales`, including `/{id}`, `/{id}/payments`, `/{id}/return`, `/dashboard`, `/reports` and `/reports/export.csv`.

## Validation evidence

### Backend and Flyway

- Full `mvn clean verify`: **196/196 tests passed**, zero failures/errors; compile, test and Spring Boot JAR packaging succeeded on Java 21.
- Sales permission, tenant isolation, lifecycle and detail regression coverage passed within the full suite.
- Fresh PostgreSQL 16 database: Flyway applied **V1–V36** successfully.
- Restart validation: all 36 migrations validated, current schema version 36, schema up to date.
- Backend build metadata exposed branch `codex/final-sami-release`, implementation commit `93e89c0da9813f009ec3bc6ecc521074f957f9b5` and build timestamp through Actuator build info.

### Frontend

- `npm test`: **7/7 passed**.
- `npm run type-check`: passed.
- `npm run build`: passed with Vite 8.1.5; **1013 modules** transformed and the Sales chunk emitted.
- Localization: exact English/Persian key parity passed.
- Static route permissions, backend authorization permission seeds, lazy route files, tenant identity and Sales namespace release contracts passed.
- No separate lint command is configured in `package.json`; this check is therefore not applicable.

### Real browser and responsive verification

The in-app browser used the production nginx frontend at `http://localhost:18082` with the real Spring Boot backend, real PostgreSQL database and real Flyway migrations; no mock server was used.

- Admin login, Persian `فروش` menu, English `Sales` menu and direct `/sales` navigation passed.
- Persian RTL and English LTR document direction/language passed with no horizontal page overflow.
- A real customer and product were created in the isolated database, then the complete Sale form created invoice `SAL-2026-000001`.
- The persisted invoice reopened without lazy-load or multi-bag failure; edit-draft, items, payments and accounting surfaces rendered.
- A real payment was recorded; the Sale was confirmed and completed; the return form then exposed refundable quantity, reason and refund method.
- Reports rendered revenue, gross profit, average ticket, invoice count, daily sales, top product and payment method from the real verification record.
- A user with only `sales:view` saw the Sales menu/list and no raw permission code or error, but did not see create, export, reports or lost-sales actions.
- Final responsive matrix for the full-screen create form passed at **360, 375, 390, 412 and 430 px**: exact viewport width, no page overflow, no dialog overflow, Persian `fa`/`rtl` preserved.
- Desktop validation passed with the data table, dialogs and action panels; no blank route, placeholder Sales screen or lazy-import error occurred.

### Docker and proxy

- Backend image: `sami-backend:final-consolidated`, `linux/amd64`, OCI revision `93e89c0da9813f009ec3bc6ecc521074f957f9b5`.
- Frontend image: `sami-frontend:final-consolidated`, `linux/amd64`, OCI revision `93e89c0da9813f009ec3bc6ecc521074f957f9b5`.
- Both images built successfully from their production Dockerfiles.
- Production Compose interpolation/config validation passed when the required secrets were provided as process-local validation values; no `.env` file was created.
- Fresh PostgreSQL, backend and frontend containers are healthy.
- nginx root and `/health` returned HTTP 200; authenticated `/api/v1/sales` through the nginx `/api` proxy returned HTTP 200.
- The production bundle contains the fingerprinted Sales assets and exact build commit. Navigation/business API requests are not served from a stale shell cache; service-worker activation also removes old shell caches.

## Remaining limitations and risks

- Sales is a single-workspace route. Create, edit, details and reports do not have independent bookmarkable deep URLs; this is the existing verified frontend architecture, not a placeholder or missing integration.
- Browser verification covered the application-provided Chromium surface rather than a separate cross-browser farm.
- Hibernate logs `HHH90003004` for an existing paginated collection-fetch query. It did not fail Sales workflows or validation and is recorded as a performance/maintainability observation, not expanded into this integration fix.
- The three secondary worktrees remain on disk even though they contain no unique work. They may be removed later with `git worktree remove` after explicit cleanup approval.
- The preserved demo-seed preference remains in `stash@{0}` and must not be dropped unintentionally.

## Release conclusion

All valid completed branch/worktree work inspected by this audit is reachable from `codex/final-sami-release`. The Sales menu namespace, persisted detail lookup and view-only permission behavior are corrected and protected by tests. The exact implementation revision passed backend, frontend, Flyway, browser, responsive, Docker, healthcheck and nginx proxy validation. The branch is ready for the next module or an explicitly authorized push/deployment after the report commit leaves the worktree clean.
