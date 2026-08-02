# SAMI ERP — Next Session Handoff

Updated: 2026-08-02 (Asia/Tehran)

## Current project status

- Authoritative branch: `development`
- Completed implementation HEAD before this handoff document: `9a64389` (`feat(settings): add PWA access mobile preferences and demo notifications`)
- Remote status at document creation: local `development` was 46 commits ahead of `origin/development`; the final documentation commit and push result are reported by the closing session.
- Production deployment: not performed or independently verified today. The repository does not contain an authoritative record proving which commit currently runs on the customer VPS.
- Latest verified development deployment: the pre-Settings integrated revision `3e83ff6` had verified Linux/amd64 images and healthy production Compose services per `DEVELOPMENT_INTEGRATION_REPORT.md`. The Settings feature was additionally verified in an isolated local Compose stack, not deployed.

## Completed today

### Settings, mobile navigation, and notification foundation

- Purpose: permanent PWA install access, per-user mobile bottom navigation, and safe hourly demo notifications.
- Main files: `sami-frontend/src/views/ProfileView.vue`, `DefaultLayout.vue`, `usePwa.ts`, `StaffNotificationMenu.vue`, user-experience API/store/types; backend `notification/**`, `UserExperiencePreference*`, configuration, Compose examples, and Flyway V37.
- Architecture: existing PWA composable and Scheduler are the only owners; preferences are authenticated-user and tenant scoped; menu codes are permission/lifecycle validated server-side; in-app notifications are real, while unavailable Web Push is not simulated.
- APIs: `GET/PUT /api/v1/users/me/preferences`; notification list, unread-count, read, and read-all under `/api/v1/notifications`.
- Database: `V37__user_experience_preferences_and_notifications.sql` creates per-user preferences and tenant/user notifications with hourly idempotency.
- UI: three Settings sections, live mobile-nav preview, reorder/remove/defaults, notification opt-in, real inbox badge, Persian RTL and English LTR.
- Tests: frontend type-check, 9 tests, production build; backend compile, 6 focused tests, 202-test package; fresh PostgreSQL/Flyway, Docker images/Compose health, authenticated browser, and mobile widths 360/375/390/412/430 passed.
- Limitations: server Web Push/VAPID delivery is not implemented; future tenant provisioning must create the system demo job through Scheduler.
- Detailed evidence: `SETTINGS_MOBILE_NAVIGATION_NOTIFICATION_REPORT.md` and `PUSH_NOTIFICATION_FOUNDATION.md`.

### Deployment automation milestone already present on development

- Purpose: deterministic Windows-to-VPS Docker deployment using SSH keys, revision-labelled Linux/amd64 images, manifests, health verification, and rollback.
- Files: `scripts/deploy-sami.ps1`, `scripts/verify-deployment.ps1`, `DEPLOYMENT_AUTOMATION_GUIDE.md`, deployment reports and Compose files.
- Decision: no passwords are persisted or passed on command lines; routine deployment is non-interactive after OpenSSH key setup.
- Today: no deployment was run.

## Open features

### Customer-origin purchases — intentionally incomplete, preserved

- Branch/worktree: `codex/feature-customer-purchases`, commit `744ca38`, `.worktrees/customer-purchases`.
- Progress: additive customer/supplier counterparty model; customer/sale linkage; condition, inspection, declaration, valuation and settlement snapshots; tenant/customer validation; CRM timeline events; inventory receipt settlement guard; IMEI Luhn validation; seller filters; customer-aware create/detail UI; focused service tests.
- Validated so far: backend compile passed; 3 focused tests passed; frontend tests, type-check, localization parity, and production build passed. A full backend run reached 199 tests with no feature failure, then failed only because the temporary backend-only build lacked the sibling Vite config. The corrected rerun was blocked by interrupted Maven artifact downloads and is not a release gate.
- Remaining work: rebase onto current `development`; rename its `V37__customer_origin_purchases.sql` to V38 because Settings now owns V37; resolve contract conflicts; complete inspection depth, payment/accounting boundary, reversal behavior, reports/detail presentation, permissions review, comprehensive backend/frontend tests, fresh Flyway, Docker, browser/mobile workflows, report, merge, and validation.
- Important decision: a trade-in may link a Purchase to one same-customer Sale, with a unique tenant/sale constraint. Sale-offset payment is deliberately not faked because current Sales payment methods have no purchase-offset accounting contract.
- Affected modules: Purchasing, CRM, Inventory, Data Quality IMEI, Sales linkage, reporting, frontend purchasing.

### Mobile font system — requested, not started

- Source request: attachment `07d56b23-50aa-40b2-aa07-7c7d4420018c/pasted-text.txt` from this session.
- Remaining: inventory font assets/declarations, computed mobile styles, Vuetify inheritance, production CSS/font URLs, nginx MIME/cache, PWA revisions, five mobile widths, Docker/browser checks, smoke test, and `FONT_SYSTEM_AND_MOBILE_RENDERING_REPORT.md`.
- Suggested branch: `codex/fix-mobile-font-system` after customer-purchase work is either integrated or deliberately deferred.

### Preserved stash

- `stash@{0}`: `local: preserve demo seed preference before stabilization`.
- It was intentionally left untouched. Treat it as local preference/demo data, not completed product code. Inspect with `git stash show --stat 'stash@{0}'` before deciding whether to drop or apply it.

## Architecture notes

- Sales: real tenant-scoped aggregate with separate payments, stock issuance, returns, accounting entries and audit. Do not model trade-in as a discount; extend the payment/accounting contract before offsetting a purchase against a sale.
- Customer purchases: extend the existing Purchase aggregate with an exact-one counterparty constraint. Customers remain CRM customers and are not converted to suppliers. Reuse receipt, identifier, inventory, audit, approval and CRM timeline infrastructure.
- Mobile navigation: preference stores menu codes, not arbitrary routes. Backend menu permissions and lifecycle state are authoritative; frontend re-sanitizes stale choices before rendering.
- Notifications: Notification Center/in-app inbox is the decision and presentation boundary. Business modules must not directly create browser push. Demo delivery is Scheduler-owned, opt-in, idempotent and disabled by default.
- PWA: install-prompt capability and banner dismissal are separate states. Settings remains the permanent recovery surface. Hashed production assets and the existing update prompt own cache refresh.
- Fonts: no decision made today. The next session must prove actual loaded glyph assets and computed styles before selecting a canonical family.
- Deployment: `development` is the source for images. SSH-key authentication only; image revision labels and health checks must match the exact Git commit.

## Next recommended tasks

| Priority | Task | Effort | Dependencies | Suggested branch |
|---|---|---:|---|---|
| 1 | Rebase and finish customer-origin purchases | 2–4 days | Current `development`, PostgreSQL/Docker, Sales/Inventory/CRM contracts | `codex/feature-customer-purchases` |
| 2 | Fix and verify mobile font system | 1–2 days | Stable frontend after Purchasing merge | `codex/fix-mobile-font-system` |
| 3 | Add real Web Push server delivery, only if approved | 2–4 days | VAPID secret management, subscription API/entity, provider | `codex/feature-web-push-delivery` |
| 4 | Verify/deploy a named development revision | 0.5 day | Clean development, SSH key, VPS window | deployment workflow, no feature branch |

## Known issues

- Critical: none verified today.
- High: customer-origin purchases are incomplete and must not be merged/deployed from commit `744ca38`; its migration number conflicts with development V37 until rebased/renamed.
- Medium: closed-app Web Push has no backend delivery provider; only the real in-app inbox is available. The current production/VPS revision is not recorded authoritatively in Git. Mobile font fallback issue remains uninvestigated.
- Low: one preserved demo-preference stash needs an explicit keep/drop decision; Maven dependency downloads were intermittently truncated during the final customer-purchase full-suite rerun.

## Test status

- Backend development/Settings: compile, focused tests and full 202-test package passed before commit.
- Frontend development/Settings: tests, exact locale parity, type-check and production build passed.
- Docker: Settings backend/frontend images built; fresh Compose/PostgreSQL healthy in isolated local validation. No new post-documentation image is claimed.
- Browser/mobile: authenticated Settings and notification flows passed; widths 360/375/390/412/430 passed without horizontal overflow; Persian RTL and English LTR verified.
- Flyway: development V1–V37 passed on fresh PostgreSQL for Settings. Customer-purchase V37 has not been integrated and must become V38.
- Customer-purchase branch: partial validation only, as detailed above.

## Deployment status

- Latest deployed commit: unknown/unverified; do not infer it from local Docker containers or reports.
- Latest development implementation commit before handoff docs: `9a64389`.
- Verified pre-Settings image revision: `3e83ff6`, documented in `DEVELOPMENT_INTEGRATION_REPORT.md`.
- Image naming/workflow: `sami-backend:test` and `sami-frontend:test`; build/export/deploy with `scripts/deploy-sami.ps1` and verify with `scripts/verify-deployment.ps1`.
- Rollback: deployment history under `/root/sami-deployment-history/<UTC timestamp>` retags preserved application image IDs. Flyway is forward-only; image rollback does not reverse schema migrations.

## Codex context

- Monorepo: Java 21/Spring Boot/PostgreSQL/Flyway backend and Vue 3/TypeScript/Vite/Vuetify/Pinia frontend. Root `development` is authoritative.
- Start every task with `AGENTS.md`, `.agents/SAMI_PROJECT_CONTEXT.md`, `docs/21-ai-agent-guide.md`, and the relevant `.agents/skills` workflow.
- Source code is implementation truth; docs are context; Git history is supporting intent. Reuse existing owners before creating artifacts and preserve tenant/company/branch, permissions, audit, translations, contracts and migrations end to end.
- Completed core modules include Automation, Licensing, Sales, Inventory, Purchasing supplier flows, CRM, deployment automation, UI/localization/PWA foundation, and the new Settings/in-app notification work.
- Active development is limited to customer-origin Purchasing and the requested font correction. Do not begin another module first.
