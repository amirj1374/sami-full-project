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
| Products | Implemented for basic CRUD | `product` | products | V2 | No canonical variant/SKU inventory model |
| Purchasing | Implemented for current scope | `purchasing` | purchases | V8 | Receiving is not a stock ledger |
| Dashboard/KPI | Implemented | `dashboard` | dashboard views/widgets | V10 | Operational metric coverage is separate |
| Automation | Backend only | `automation` | none verified | V11 | In-process event bridge |
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
| Inventory / Sales / Pricing / Payments / Accounting | Planned | no owner | placeholders may exist | none authoritative | Business and architecture decisions open |

See [module notes](modules/README.md) for entry points and extension guidance.
