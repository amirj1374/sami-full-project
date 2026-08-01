# Module catalog

Status means repository evidence, not roadmap intent:

- **Implemented:** meaningful backend and frontend path exists.
- **Backend only:** executable backend/API exists without corresponding UI.
- **Partially implemented:** meaningful foundations exist but a required path is missing.
- **Planned:** metadata, placeholder, or requirements only.

| Module | Status | Backend | Frontend | Migration(s) | Important limitation |
|---|---|---|---|---|---|
| Authentication | Implemented | `auth`, `security` | login/auth store/interceptors | V1, V5 | Tokens stored in `localStorage` |
| RBAC/modules/menu | Implemented | `authz` | admin views, dynamic menu | V3–V5, V25 | Client checks are presentation only |
| Users | Implemented | `user` | users admin | V1, V5 | Tenant transition applies |
| CRM/customers | Implemented | `crm` | customers | V6–V7 | Integration events are process-local |
| Suppliers | Implemented | `supplier` | suppliers | V9 | Documents use legacy storage path |
| Products | Implemented for basic CRUD | `product` | products | V2, V16, V32 | Legacy integer stock is an Inventory compatibility projection; variants/lots remain out of scope |
| Purchasing | Implemented for current scope | `purchasing` | purchases | V8, V32 | Receipt and return stock post to the canonical Inventory ledger |
| Inventory | Implemented | `inventory` | inventory dashboard and operational panels | V32 | Moving weighted-average valuation; no speculative lot/variant model |
| Sales | Implemented | `sales` | sales | V28, V31, V32 | Uses Inventory reservations, issue and customer-return integration |
| Dashboard/KPI | Implemented | `dashboard` | dashboard views/widgets | V10 | Operational metric coverage is separate |
| Automation | Implemented | `automation` | automation rules, execution history and monitoring | V11, V16, V29 | Domain-event delivery is process-local; action timeout cancellation is cooperative |
| Licensing/tenant | Backend only | `licensing` | none verified | V12, V14, V16–V17 | Tenant context remains transitional |
| Data quality | Backend only | `dataquality` | none verified | V13 | Coverage varies by registered rules |
| Metadata/forms | Backend only | `metadata` | none verified | V15 | Dynamic-field consumers require validation |
| Files | Backend only | `files` | no dedicated UI | V18 | Overlaps legacy `common.storage` |
| Scheduler | Backend only | `common.scheduler` | none verified | V24 | Timeout does not guarantee task termination |
| Scheduling/appointments | Backend only | `scheduling`, `calendar` | none verified | V19, V23 | No user workflow UI |
| Knowledge | Backend only | `knowledge` | none verified | V20 | Large API surface, limited tests |
| Customer portal | Partially implemented | `portal` | no portal UI/controller verified | V21 | OTP provider implementations absent |
| Organization | Partially implemented | shared mappings | no management UI verified | V22, V26 | Ownership/runtime scope needs consolidation |
| Communication | Backend only foundation | `comm` | none verified | V27 | Provider handlers absent |
| Pricing / Accounting | Planned | no canonical owner | placeholders may exist | none authoritative | Business and architecture decisions open |

See [module notes](modules/README.md) for entry points and extension guidance.
