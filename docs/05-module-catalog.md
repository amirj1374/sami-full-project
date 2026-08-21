# Module catalog

This is the canonical current-state capability catalog. Feature and release
reports are historical evidence; they do not override this table. Status is
based on executable backend ownership, frontend reachability, permissions,
localization and available validation—not on a migration, menu row or Vue file
alone.

- **COMPLETE:** usable end-to-end for its stated scope.
- **ACTIVE_DEVELOPMENT:** a released scope exists and a named extension is the
  current priority.
- **PARTIAL:** meaningful implementation exists, but a required path or release
  gate is missing.
- **PLANNED:** documentation, registry metadata or integration seams only.
- **DISABLED:** intentionally unavailable through current lifecycle metadata.

| Module | Status | Verified ownership | Current boundary or remaining gap |
|---|---|---|---|
| Authentication | COMPLETE | `auth`, `security`; auth store/interceptors and login flows | Staff tokens remain in browser `localStorage`; security hardening is tracked separately |
| RBAC, modules and menu | COMPLETE | `authz`; admin views; backend-driven menu and route guards | Frontend checks are presentation only |
| Users | COMPLETE | `user`; Users admin and profile/settings | New tenant-scoped work must use `TenantContext` |
| Dashboard and KPI | COMPLETE | `dashboard`; dashboard, KPI, viewer and module-report views | General cross-module reporting is separate |
| CRM and Customers | COMPLETE | `crm`; `CustomersView.vue`; V34 completion | Domain events use the current process-local event model |
| Suppliers | COMPLETE | `supplier`; `SuppliersView.vue` | Supplier documents still overlap legacy storage ownership |
| Products | COMPLETE | `product`; `ProductsView.vue` | Complete for basic CRUD; Inventory owns stock; variants/lots are not implemented |
| Inventory | COMPLETE | `inventory`; `InventoryView.vue`; `InventoryStockOperations`; V32 | Moving weighted-average model; no speculative lot/variant model |
| Purchasing | COMPLETE | `purchasing`; `PurchasesView.vue`; Customer Purchase dialogs; V33/V38 | Supplier and CRM Customer sellers, inspection, valuation, settlement, linked-Sale and Inventory receipt are implemented; Accounting posting remains outside the approved scope |
| Sales | COMPLETE | `sales`; `SalesView.vue`; V31/V32/V36 | Inventory integration is canonical; no purchase-offset accounting contract |
| Legacy Asan Import and Comparison | ACTIVE | `legacyimport`; `LegacyImportsView.vue`; V40, V45, V46 | Tenant-safe Asan archive and accounting-XLSX staging, reconciliation, acceptance evidence, and explained differences. Canonical final import remains intentionally blocked pending an approved accounting owner. |
| HAMTA activation codes | COMPLETE | `hamta`; `HamtaView.vue`; Purchasing/Inventory/Sales integration; V41 | Implementation/source/fixture gates pass; fresh PostgreSQL/Flyway, container stack and live bilingual smoke remain blocked |
| Market Sync and Auto Pricing | PARTIAL | `marketsync`; `MarketSyncView.vue`; Product/Inventory/Sales/Scheduler integration; V42 | Local implementation gates pass; authorized Rond contracts, real website connector, fresh Flyway and container/live validation remain missing |
| Automation | COMPLETE | `automation`; `AutomationsView.vue`; V29 | Events are process-local; timeout cancellation is cooperative |
| Licensing | COMPLETE | `licensing`; `LicensingView.vue`; V30 | External signing/billing providers remain deployment-specific |
| Background Scheduler | COMPLETE | `common.scheduler`; `SchedulerView.vue`; V24 | Timeout does not guarantee termination of a non-cooperative handler |
| Data Quality | COMPLETE | `dataquality`; typed client and `DataQualityView.vue`; V13/V39 | Active at `/data-quality`; local contract, localization, build and responsive validation pass; production-like infrastructure gate remains pending |
| Files and Media | PARTIAL | `files`; typed client and `FilesView.vue`; V18 | V35 disables navigation; central/legacy storage ownership and release validation remain incomplete |
| Dynamic Forms and Metadata | PARTIAL | `metadata`; V15 | V35 disables navigation; backend/API exists but no complete routed management experience |
| Notifications | PARTIAL | `notification`; staff inbox/menu; V37 | In-app notifications are implemented; backend Web Push subscriptions/VAPID delivery are not |
| Appointments and Resources | PARTIAL | `scheduling`, `calendar`; typed client and `AppointmentsView.vue`; V24 | V35 disables navigation; only a focused booking surface exists and release validation is incomplete |
| Knowledge | PARTIAL | `knowledge`; V20 | V35 disables navigation; backend API exists but no production UI workflow |
| Customer Portal | PARTIAL | `portal`; V21 | V35 disables navigation; provider/UI completion is missing |
| Organization | PARTIAL | shared organization mappings; V22/V26 | V35 disables navigation; runtime scope and management UI remain incomplete |
| Communication | PARTIAL | `comm`; V27 | V35 disables navigation; concrete provider delivery is absent |
| PWA and Settings | COMPLETE | service worker, `usePwa`, `ProfileView`, user preferences | Source font/PWA configuration exists; physical-device font behavior is not yet proven |
| Reports | PARTIAL | Dashboard and module-specific reports | No canonical general reporting module or unified analytics workflow |
| Repairs | PLANNED | scheduling/portal/knowledge references only | No owning aggregate, API or UI |
| Warranty | PLANNED | reference data/text only | No owning module |
| Installments | PLANNED | portal/workflow references only | Ownership and accounting boundary are undecided |
| Accounting | PLANNED | Sales-local accounting entries only | No canonical General Ledger, chart of accounts or posting contract |

The frontend source contains partial views for Files and Appointments. They are
deliberately not static routes: V35 disables those modules and
`sami-frontend/tests/release-contracts.test.mjs` protects that boundary.

See [module notes](modules/README.md) for package entry points and
[IMPLEMENTATION_BACKLOG.md](IMPLEMENTATION_BACKLOG.md) for prioritized work.
