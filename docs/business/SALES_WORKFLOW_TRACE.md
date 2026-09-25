# Sales workflow trace for development and QA

## Scope and verified topology

The current implementation exposes two independent sales aggregates. This is a source/contract finding, not a proposed design:

```text
Core sale:       SalesView -> /api/v1/sales -> SalesService -> Sale
                    DRAFT -> CONFIRMED -> COMPLETED

Document flow:   SalesDocumentsPanel -> SalesOrdersPanel -> SalesDeliveriesPanel
                 -> SalesInvoicesPanel -> SalesReceiptsPanel
                    quotation -> order -> delivery -> invoice -> receipt/allocation
```

`SalePayment` belongs to `Sale` and is the value used by `SalesService.complete`. `SalesReceipt` is allocated to an issued `SalesInvoice`; neither public contract nor service link makes that receipt a `SalePayment`. A receipt therefore does not make a core sale eligible for completion.

## Cross-cutting behaviour

- UI base client: Axios `/api`, therefore the browser calls relative `/api/v1/...` through nginx.
- Tenant/company/branch: backend services validate active organization and authorization scope. UI forms load the active organization context and include the corresponding IDs in mutation payloads or query parameters.
- Mutation protection: the Sales panels own a `saving`/busy guard and disable the initiating control while a request and its refresh are in progress. The mutation owner, not only visual state, guards rapid duplicate clicks.
- Error path: `useApiError` normalizes API failures, then the sales mapper returns Persian user messages for business, access, validation, conflict, resource, server, network and unknown failures. Technical diagnostics may remain in developer logs but are not user-facing.
- Persistence/refresh: every list/detail mutation reloads its list/detail state before the busy lock is released. Receipt confirmation returns a result map; the public receipt API has no list/get endpoint, so independent receipt read-back cannot currently be performed through that public contract.

## Core Sale

| UI/action | Handler and validation | API | Backend/service transition | Persisted result and next action |
|---|---|---|---|---|
| SalesView: New sale, Save | `save`; company, branch, customer, item product and quantity must be present/valid; mutation guard | `POST /api/v1/sales` | `SalesController.create` -> `SalesService.create`; validates scope/customer/products and recalculates totals | `Sale` is `DRAFT`; detail/list reload. Confirm or edit/cancel/discount are next valid actions. |
| Sale detail: Confirm | `act('confirm')`; selected sale must be `DRAFT` | `POST /api/v1/sales/{id}/confirm` | `SalesService.confirm`; reserves stock and checks discount/low-profit decisions | `CONFIRMED`; reload. Payment, complete (when eligible) or cancel are next actions. |
| Sale detail: Add payment | `SaleActionPanel`; amount/method form validation plus busy guard | `POST /api/v1/sales/{id}/payments` | `SalesService.addPayment`; allowed for `DRAFT`/`CONFIRMED`, amount may not exceed final amount; cash/card/transfer/wallet captured, cheque/installment pending | payment is persisted on `Sale`; reload. Only captured payments count for complete. |
| Sale detail: Complete | `act('complete')`; `getSaleCompletionEligibility` requires `CONFIRMED` and captured-payment sum equal to `finalAmount` in minor units | `POST /api/v1/sales/{id}/complete` | `SalesService.complete`; independently sums captured payments and compares exactly with final amount; throws `OPERATION_NOT_ALLOWED` on mismatch; issues stock/accounting on success | `COMPLETED`; reload. Return is the subsequent supported lifecycle path. |
| Sale detail: Cancel / Return | status-specific guard | `POST /api/v1/sales/{id}/cancel` or `/return` | cancel is permitted in `DRAFT`/`CONFIRMED`; return only after completion | `CANCELLED`, `PARTIALLY_RETURNED`, or `RETURNED`; reload. |

### Core Sale state machine

```text
DRAFT --confirm--> CONFIRMED --complete, paid exactly--> COMPLETED --return--> PARTIALLY_RETURNED/RETURNED
  |                    |
  +--cancel----------->+--cancel-----------------------> CANCELLED
```

`DRAFT` is the only normal editable state. Completion requires `sum(CAPTURED payment.amount) == finalAmount`; pending cheque/installment payments do not satisfy it. Authorization is enforced by `sales:create`, `sales:edit`, `sales:confirm`, `sales:payment`, `sales:complete`, `sales:cancel`, and `sales:return` as applicable.

## Quotation / Proforma

| UI/action | Handler and validation | API | Backend/service transition | Persistence/next action |
|---|---|---|---|---|
| SalesDocumentsPanel: create/edit | customer, company, branch, document type and non-empty valid product lines; busy guard | `POST /api/v1/sales-documents`, `PUT /api/v1/sales-documents/{id}` | `SalesDocumentService.create/update`; validates canonical customer contact role, scope and lines | `DRAFT`; list/detail reload. Edit, issue or cancel. |
| Issue | selected document must be `DRAFT` | `POST /api/v1/sales-documents/{id}/issue` | `SalesDocumentService.issue` | `ISSUED`; can be converted to order. |
| Expire / cancel | status guard | `POST /api/v1/sales-documents/{id}/expire` or `/cancel` | service transition | `EXPIRED` or `CANCELLED`; no conversion. |

State machine: `DRAFT -> ISSUED -> EXPIRED`, and `DRAFT|ISSUED -> CANCELLED`. Conversion accepts only an issued quotation/proforma.

## Sales Order

| UI/action | Handler and validation | API | Backend/service transition | Persistence/next action |
|---|---|---|---|---|
| SalesOrdersPanel: direct create | customer/product lines and organization context; busy guard | `POST /api/v1/sales-orders` | `SalesOrderService.create` | `DRAFT`; reload. |
| SalesOrdersPanel: convert document | source selected from issued quotation/proforma | `POST /api/v1/sales-orders/from-quotation/{sourceId}` | `SalesOrderService.convert` | `DRAFT`; reload. |
| Confirm | selected order must be `DRAFT` | `POST /api/v1/sales-orders/{id}/confirm` | `SalesOrderService.confirm`; reserves available stock and may report partial reservation/backorder | `CONFIRMED`; reload. Delivery becomes valid. |
| Cancel | status guard | `POST /api/v1/sales-orders/{id}/cancel` | service transition | `CANCELLED`; reload. |

State machine: `DRAFT -> CONFIRMED` or `DRAFT|CONFIRMED -> CANCELLED`. Delivery must originate from a confirmed order.

## Delivery

| UI/action | Handler and validation | API | Backend/service transition | Persistence/next action |
|---|---|---|---|---|
| SalesDeliveriesPanel: create from order | confirmed order; each submitted quantity is positive and no more than remaining reserved quantity; busy guard | `POST /api/v1/sales-deliveries/from-order/{orderId}` | `DeliveryService.create`; validates fulfilled/reserved quantities | `DRAFT`; reload. Confirm or cancel. |
| Confirm | selected delivery must be `DRAFT` | `POST /api/v1/sales-deliveries/{id}/confirm` | `DeliveryService.confirm`; idempotently accepts already-confirmed delivery and issues partial stock | `CONFIRMED`; reload. Its lines become invoiceable. |
| Cancel | draft-only guard | `POST /api/v1/sales-deliveries/{id}/cancel` | service transition | `CANCELLED`; reload. |

State machine: `DRAFT -> CONFIRMED` or `DRAFT -> CANCELLED`. There is no public ordinary reversal endpoint for a confirmed delivery.

## Invoice

| UI/action | Handler and validation | API | Backend/service transition | Persistence/next action |
|---|---|---|---|---|
| SalesInvoicesPanel: load invoiceable lines | selected confirmed order/company/branch | `GET /api/v1/sales-invoices/invoiceable/{orderId}` | `SalesInvoiceService.invoiceable` returns confirmed-delivery, not-yet-invoiced capacity | no mutation; UI pre-fills eligible line quantities. |
| Create invoice | organization, order, currency and at least one positive delivery-line quantity; busy guard | `POST /api/v1/sales-invoices` | `SalesInvoiceService.create`; verifies delivery lines belong to the confirmed order and available quantity | `DRAFT`; reload. |
| Issue | draft-only guard | `POST /api/v1/sales-invoices/{id}/issue` | `SalesInvoiceService.issue`; posts receivable via accounting port | `ISSUED`; reload. Receipt allocation becomes valid. |
| Cancel | draft-only guard | `POST /api/v1/sales-invoices/{id}/cancel` | service transition | `CANCELLED`; reload. |

State machine: `DRAFT -> ISSUED`, or `DRAFT -> CANCELLED`. Issued invoices require an approved reversal process not exposed by this UI/API flow.

## Receipt and settlement

| UI/action | Handler and validation | API | Backend/service transition | Persistence/next action |
|---|---|---|---|---|
| SalesReceiptsPanel: create | customer, company/branch, amount, payment method, `Idempotency-Key`; busy guard | `POST /api/v1/sales-receipts` with query parameters | `SalesReceiptService.create` | receipt `DRAFT`; returned receipt ID is held for the next action. |
| Allocate | issued invoice, positive allocation no greater than both receipt amount and invoice outstanding amount | `POST /api/v1/sales-receipts/{id}/allocations` | `SalesReceiptService.allocate` | allocation persists; no separate UI state list API. |
| Confirm / confirm-now | active treasury account in same company/branch and total allocation > 0 | `POST /api/v1/sales-receipts/{id}/confirm-now?treasuryAccountId=...` | `ReceiptConfirmationOrchestrator.confirm`; confirms receipt, treasury inflow and accounting receivable settlement | receipt `CONFIRMED`; response includes treasury/accounting references. |

State machine: `DRAFT -> CONFIRMED`; the enum also contains `CANCELLED`, but the current public receipt controller does not expose a cancel action. The UI treats allocate-and-confirm as one user operation. Settlement is the accounting effect of confirming an allocated receipt, not a separate button.

## Contract and QA findings that remain open

1. **No unified aggregate:** Core Sale completion uses only core Sale payments; invoice receipts do not satisfy it. This is a business-decision/release-blocking workflow gap for any promised single chain `Sale -> ... -> Receipt -> Complete`.
2. **Receipt read-back gap:** No public list/get endpoint exists for receipts. QA can observe the confirmation response and downstream treasury/accounting references, but cannot independently refresh and retrieve a receipt through the receipt API.
3. **Payment methods:** core Sale payment methods have explicit lifecycle semantics. Receipt UI currently submits CASH. The allowed receipt payment method policy is not explicit in its controller/service contract and needs a business decision if non-cash receipts are required.

The frontend `InvoiceableLine` type has been reconciled with the backend response (`deliveryId` and `previouslyInvoiced`) and is protected by a contract test.

## Required runtime evidence per mutation

For every QA action, record all of the following before marking it passed:

```text
CLICK -> enabled button -> owning handler -> exactly one HTTP mutation -> successful response
      -> state/list/detail refresh -> GET/read-back where public API exists -> browser refresh persistence
```

Rapid second click while the first mutation is pending must produce no second mutation for create/issue/confirm/payment/receipt/complete operations. A disabled legitimate next action, an enabled click with no request, an unexpected API error, or a missing persistence read-back is a failure or not-verified result; it is not a pass.
