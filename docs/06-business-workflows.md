# Business workflows

Detailed flow summaries are maintained in [workflows/README.md](workflows/README.md).

| Workflow | Verified state | Primary entry points |
|---|---|---|
| Staff login and refresh | Confirmed | `AuthController`, `AuthService`, frontend auth store and `api/http.ts` |
| Dynamic authorization/menu | Confirmed | `Authz`, authz controllers/services, menu store/router |
| User lifecycle | Confirmed | `UserController`, `UserService`, users view |
| Customer lifecycle | Confirmed | `CustomerController`, CRM services, customers view |
| Supplier lifecycle | Confirmed | supplier controllers/services, suppliers view |
| Purchase approval/receiving/return | Confirmed for purchasing scope | `PurchaseController`, purchasing services, purchases view |
| Dashboard loading | Confirmed | dashboard controllers/services and widget registry |
| Managed-file lifecycle | Backend only | `FileController`, files services/providers |
| Automation execution | Confirmed | automation controller/engine, shared scheduler handlers, automation view and API client |
| Scheduled-job execution | Backend only | scheduler controller, runner and handlers |
| Portal authentication | Partially implemented | portal services/security; no verified public end-to-end UI/controller |
| Communication delivery | Backend foundation | durable message rows and dispatcher; provider handlers absent |

Purchase receiving can record quantities and IMEI/serial identifiers. It does
not reserve, value, transfer, or issue stock and therefore must not be used as
evidence that Inventory is implemented.
