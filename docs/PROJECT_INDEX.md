# SAMI ERP Project Index

## Start here

- Workspace rules: `AGENTS.md`
- Current technical snapshot: `.agents/SAMI_PROJECT_CONTEXT.md`
- AI workflow: `docs/21-ai-agent-guide.md`
- Next-session state: `docs/HANDOFF_NEXT_SESSION.md`
- Project overview and repository map: `docs/00-project-overview.md`, `docs/02-repository-map.md`

## Architecture and engineering

- System/domain: `docs/03-system-architecture.md`, `docs/04-domain-model.md`
- Backend/frontend: `docs/14-backend-architecture.md`, `docs/13-frontend-architecture.md`
- Database/Flyway: `docs/07-database-and-migrations.md`
- APIs/integrations: `docs/08-api-and-integrations.md`
- Authentication/RBAC/security: `docs/09-authentication-and-authorization.md`, `docs/18-security.md`
- Tenancy/organization: `docs/10-multi-tenancy-and-organization.md`
- Events/automation/scheduler: `docs/11-events-automation-and-scheduling.md`
- Files/communications: `docs/12-files-and-communications.md`
- Testing/configuration/operations: `docs/15-testing-and-quality.md`, `docs/16-configuration-and-environments.md`, `docs/17-deployment-and-operations.md`, `docs/19-observability.md`
- Risks/roadmap/history: `docs/23-known-risks-and-technical-debt.md`, `docs/24-roadmap-and-open-decisions.md`, `docs/22-ai-development-history.md`

## Module map

| Module | Backend package / frontend surface | Primary evidence |
|---|---|---|
| Automation | `com.sami.app.automation`, `AutomationsView.vue` | `AUTOMATION_RELEASE_REPORT.md` |
| Licensing | `com.sami.app.licensing`, `LicensingView.vue` | `LICENSING_RELEASE_REPORT.md` |
| Sales | `com.sami.app.sales`, `SalesView.vue` | `SALES_RELEASE_REPORT.md`, `SALES_DOMAIN_DESIGN.md` |
| Inventory | `com.sami.app.inventory`, `InventoryView.vue` | `INVENTORY_RELEASE_REPORT.md` |
| Purchasing | `com.sami.app.purchasing`, `PurchasesView.vue` | `PURCHASING_RELEASE_REPORT.md`; open customer work in handoff |
| CRM/Customers | `com.sami.app.crm`, `CustomersView.vue` | module catalog and final implementation reports |
| Suppliers | `com.sami.app.supplier`, `SuppliersView.vue` | module catalog |
| Scheduler | `com.sami.app.common.scheduler`, `SchedulerView.vue` | events/scheduling architecture |
| Notifications/PWA | `com.sami.app.notification`, Settings/shell/SW | `SETTINGS_MOBILE_NAVIGATION_NOTIFICATION_REPORT.md`, `PUSH_NOTIFICATION_FOUNDATION.md`, `PWA_IMPLEMENTATION_REPORT.md` |
| Files/Knowledge/Data Quality/Metadata | corresponding backend packages and module views | `docs/05-module-catalog.md`, coverage report |
| Repairs/Appointments/remaining domains | see module catalog and routes | `docs/05-module-catalog.md`, `FULL_STACK_COVERAGE_AND_TEST_REPORT.md` |

## Deployment and release

- Canonical automation guide: `DEPLOYMENT_AUTOMATION_GUIDE.md`
- Commands and Linux guide: `DEPLOYMENT_COMMANDS.md`, `LINUX_SERVER_DOCKER_DEPLOYMENT_GUIDE.md`
- Integration evidence: `DEVELOPMENT_INTEGRATION_REPORT.md`
- Release/readiness evidence: `FINAL_IMPLEMENTATION_REPORT.md`, `FULL_STACK_COVERAGE_AND_TEST_REPORT.md`, `DEPLOYMENT_READINESS_REPORT.md`
- Rollback/verification scripts: `scripts/deploy-sami.ps1`, `scripts/verify-deployment.ps1`

## UX, localization, exports and PWA

- UI redesign: `SAMI_COMPLETE_UI_REDESIGN_REPORT.md`, `SAMI_FRONTEND_EXPERIENCE_REPORT.md`
- Localization/RTL: `LOCALIZATION_COMPLETION_REPORT.md`
- Excel/CSV encoding: `EXCEL_EXPORT_ENCODING_FIX_REPORT.md`
- PWA/push: `PWA_IMPLEMENTATION_REPORT.md`, `PUSH_NOTIFICATION_FOUNDATION.md`
- Settings/mobile navigation/in-app notifications: `SETTINGS_MOBILE_NAVIGATION_NOTIFICATION_REPORT.md`

## Repository-scoped implementation guides

- Skills live under `.agents/skills/`: project context, architecture audit, module/backend/frontend builders, contract validator, migration guardian, repository reconciler, UI/UX designer, reporter and release gate.
- Contribution conventions: `docs/20-contribution-guide.md`.
- Business workflows and glossary: `docs/06-business-workflows.md`, `docs/22-glossary.md`.
