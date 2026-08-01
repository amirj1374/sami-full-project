# Confirmed workflow details

## Login and token refresh

```mermaid
sequenceDiagram
  actor Staff
  participant SPA
  participant Auth as Auth API
  participant DB
  Staff->>SPA: Submit credentials
  SPA->>Auth: Login
  Auth->>DB: Verify user and roles
  Auth-->>SPA: Access + refresh tokens
  SPA->>Auth: Protected request
  Auth-->>SPA: 401 when access expires
  SPA->>Auth: Single-flight refresh
  Auth->>DB: Validate refresh token
  Auth-->>SPA: Rotated/valid token response
  SPA->>Auth: Replay queued request
```

Failure paths include invalid credentials, disabled user, invalid/expired
refresh token and insufficient permissions. Tokens are currently stored in
`localStorage`.

## Purchase lifecycle

The purchase controller delegates creation/update and lifecycle work to
`PurchaseService`; approval, receipt and return behavior is separated into
specialized services. Receipt records received quantities and serial/IMEI data.
Permissions under the purchasing namespace guard operations; logs/events record
changes. Completed receipt and supplier-return operations post canonical balance,
movement, serial and valuation changes through Inventory in the same transaction.

## Inventory operations

Inventory resolves tenant ownership from the authenticated server-side context.
Adjustments and imports post signed, idempotent ledger entries. Transfers follow
draft → shipped → received/cancelled stages; shipping removes source stock and
receiving posts destination stock. Counts snapshot expected stock, capture physical
results and post only the variance. Sales reserves available stock, releases it on
cancellation, issues it on completion and posts customer returns. Every mutation
records tenant-correct audit evidence and emits a process-local domain event.

## Scheduled job execution

The poller claims eligible persisted jobs, resolves a `JobHandler`, records an
execution, runs with a configured timeout, then records success/failure and
updates scheduling state. Missing handler, lock contention, exception and
timeout are failure paths. A timeout record does not guarantee the handler has
stopped.

## Managed-file lifecycle

Upload validates metadata/content and stores managed metadata plus provider
content. Later operations support versions, retrieval, rollback, tags/folders,
soft deletion/restoration and retention. Authorization and tenant ownership
must be checked on every read/write. Provider/storage failure can leave cleanup
work; inspect service transaction boundaries before changing the flow.

Other workflow summaries are indexed in
[business workflows](../06-business-workflows.md).
