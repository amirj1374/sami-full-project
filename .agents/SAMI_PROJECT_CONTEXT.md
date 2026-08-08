# SAMI ERP — reusable development context

Last repository inspection: 2026-08-08

Current schema extends through `V41`. CR-001 HAMTA activation-code custody is
owned by canonical serialized inventory and integrates eligible used-phone
Purchasing, Sales invoice/delivery, reporting, permissions and audit.

This is the concise Codex technical snapshot. Current source/configuration is
authoritative. Start with `AGENTS.md`, `docs/21-ai-agent-guide.md`, the relevant
repository skill and `docs/HANDOFF_NEXT_SESSION.md`.

## Repository

- Full-project monorepo; frontend/backend are tracked directories, not submodules.
- Integration branch: `development`; cleanup baseline `da44091`.
- Current status and immediate work: `docs/HANDOFF_NEXT_SESSION.md`.
- Documentation ownership: `docs/PROJECT_INDEX.md`.

## Stack and runtime

- Backend: Java 21, Spring Boot 3.5.3, Spring Security/JPA/Validation/Actuator,
  PostgreSQL 16, Flyway, Maven 3.9+, JWT/JJWT 0.12.6, springdoc 2.8.9,
  Jackcess 4.0.10 and Junrar 7.6.0. RAR5 legacy imports additionally require
  an UnRAR executable configured by `app.legacy-import.unrar-executable`.
- Frontend: npm lockfile v3; Node 22 production image; Vue 3.5.40,
  TypeScript 5.7.3, Vite 8.1.5, Vuetify 3.12.10, Pinia 3.0.4,
  Vue Router 4.6.4, Axios 1.18.1, VeeValidate/Zod and Vue I18n.
- Ports: PostgreSQL 5432, backend 8080, Vite 7474. Frontend defaults to `/api`
  and Vite proxies to `http://localhost:8080`.
- Containers: PostgreSQL 16, JDK/JRE 21 backend image, Node 22/nginx frontend;
  production Compose is under `sami-backend`.

## Architecture owners and rules

- Backend modules live under `com.sami.app`; controllers are transport,
  services own business/transaction rules, DTOs protect API boundaries.
- `ApiResponse<T>` and the global exception handler own API envelopes/errors.
- RBAC is database-driven and enforced with backend `@PreAuthorize`; frontend
  route/button checks are presentation only.
- Tenant scope comes from authenticated `TenantContext` and fails closed. Do
  not propagate transitional `TenantDefaults`.
- Flyway owns schema; migrations are immutable. Current unique range is V1–V40;
  the next new migration is V41.
- Inventory owns warehouse/location balances, append-only movements,
  reservations and serial/IMEI state. Purchasing/Sales integrate through
  `InventoryStockOperations`.
- Frontend ownership: typed clients in `src/api`, contracts in `src/types`,
  Pinia for shared state, router permissions plus backend-driven menu, English
  and Persian together, RTL through Vue I18n/Vuetify.
- Design/form ownership: `src/plugins/vuetify.ts`, `src/styles/global.css`,
  `AppFormSection.vue`. PWA ownership: `usePwa`, service worker and Settings.
- Notification Center/in-app inbox is implemented. Real Web Push delivery is not.

## Current module status

- **COMPLETE:** Authentication, RBAC/Users, Dashboard, CRM, Suppliers, Products
  basic CRUD, Inventory, supplier Purchasing, Sales, Automation, Licensing,
  background Scheduler, PWA/Settings.
- **ACTIVE_DEVELOPMENT:** customer-origin Purchasing/trade-in, but its named old
  branch/commit is absent from current refs and must be recovered externally.
- **PARTIAL/DISABLED:** Data Quality, Files/Media, Metadata, Appointments,
  Knowledge, Portal, Organization and Communication. Existing partial views do
  not authorize route activation; V35 intentionally disables them.
- **PARTIAL:** in-app Notifications versus absent Web Push; general Reports.
- **PLANNED:** Repairs, Warranty, Installments and canonical Accounting.

Canonical detail: `docs/05-module-catalog.md`; priorities:
`docs/IMPLEMENTATION_BACKLOG.md`.

## Commands and environment

- Frontend: `npm ci`, `npm test`, `npm run type-check`, `npm run build`, `npm run dev`.
- Backend: `mvn test`, `mvn clean package`, `mvn clean verify`, `mvn spring-boot:run`.
- Documentation: `node scripts/validate-documentation.mjs` and
  `node --test scripts/validate-documentation.test.mjs`.
- Deployment: `scripts/deploy.ps1`; verification: `scripts/verify-deployment.ps1`.
- Exact gates: `docs/15-testing-and-quality.md`; setup:
  `docs/NEW_WORKSTATION_SETUP.md`; local run: `PROJECT_SETUP_AND_DEPLOYMENT.md`.

## Current constraints and priorities

- This workstation initially had Java 17 only and lacked Maven, Docker and
  OpenSSH; do not claim blocked backend/container gates passed.
- Recover customer-purchase source before implementation; its old proposed V37
  conflicts with current Settings V37, so recovered work needs V38+.
- Vazirmatn is configured in source but mobile/computed/offline behavior is not proven.
- Do not infer the deployed customer revision from local history or release reports.
- Never implement speculative Repairs/Warranty/Installments/Accounting or fake
  Web Push/provider behavior.
# Current context refresh — 2026-08-08

- Required local runtime remains Java 21. This workstation uses Microsoft OpenJDK 21.0.12 and Maven 3.9.16 under `%LOCALAPPDATA%\SamiDevTools`.
- Latest migration is V39. V38 adds customer-origin Purchase seller identity, organization scope, inspection/valuation/settlement and linked-Sale integrity. V39 activates the completed Data Quality UI lifecycle.
- Purchasing's seller invariant is exactly one of Supplier or CRM Customer. Customer-origin inventory receipt requires settlement `SETTLED` or explicitly `WAIVED`; linked Sales must match tenant/customer/company/branch.
- Data Quality is now statically routed at `/data-quality` with `data-quality:view`.
- Real Web Push is not a current backend capability. Do not add VAPID keys or subscriptions without approval of the contract in `FULL_NEXT_PHASE_IMPLEMENTATION_REPORT.md`.
