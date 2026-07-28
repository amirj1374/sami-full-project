# Module entry guide

Use this table to begin source inspection. Status details are in the
[catalog](../05-module-catalog.md).

| Area | Start at | Then inspect | Safe extension point |
|---|---|---|---|
| Auth/RBAC/users | `auth/AuthController`, `security/SecurityConfig` | `AuthService`, `Authz`, frontend auth store/router | permission-protected service/controller |
| CRM | `crm/web/CustomerController` | `CustomerService`, event and extension services, customers API/view | CRM public services and domain events |
| Suppliers | `supplier/web/SupplierController` | supplier/config services and documents | supplier services, not repositories |
| Products | `product/web/ProductController` | `ProductService`, frontend products client/view | product service/API |
| Purchasing | `purchasing/web/PurchaseController` | purchase, approval, receiving and return services | purchasing services/events |
| Dashboard | dashboard controllers | KPI/widget/config services and frontend registry | KPI providers/widget contracts |
| Files | `files/web/FileController` | `FileService`, provider package, retention | storage-provider SPI |
| Automation | `automation/web/AutomationController` | engine, rule executor, event bridge | registered trigger/action providers |
| Scheduler | common scheduler controller | runner, registry, job handlers | `JobHandler` |
| Scheduling | appointments controller | availability, waiting list, calendar | scheduling services/events |
| Communication | `comm/service/CommunicationService` | dispatcher, provider registry, V27 | `CommProviderHandler` |
| Portal | portal services/security | OTP registry, principal/data provider SPI | portal provider SPIs |
| Knowledge | knowledge controller | article, approval, SOP and relation services | knowledge services/events |
| Metadata/data quality | respective controllers | services, validation engine | registered metadata/rule providers |
| Licensing/tenancy | licensing controller | tenant/license/usage services | public service/event contracts |

Do not create module pages solely from directory names. Add a dedicated page
when a module gains enough workflows, invariants and operational ownership to
justify independent maintenance.
