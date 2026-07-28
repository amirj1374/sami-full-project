# API and integrations

## Conventions

- Backend endpoints use `/api/v1`.
- Frontend base URL defaults to `/api`; clients append `/v1/...`.
- Successful JSON responses commonly use `ApiResponse<T>`.
- Validation and domain exceptions are centralized by
  `common/exception/GlobalExceptionHandler`.
- List endpoints use module-specific pagination/filter DTOs; inspect the target
  controller/client rather than assuming one universal query syntax.
- Staff requests send a bearer access token. A 401 initiates single-flight
  refresh and request replay in the frontend HTTP client.
- OpenAPI UI is available at `/swagger-ui.html` when exposed.

## Major endpoint groups

Confirmed groups include `auth`, `users`, `user-statuses`, `profile-fields`,
`roles`, `permissions`, `modules`, `menu`, `customers`, CRM configuration,
`suppliers`, supplier configuration, `products`, `purchases`, purchasing
configuration, dashboards/KPIs/widgets, automation, files, metadata, data
quality, knowledge, licensing, appointments, and scheduler administration.

## Integration boundaries

- Spring events integrate domain changes with automation, but are not durable.
- Communication defines `CommProviderHandler`; no implementation is registered.
- Portal defines `OtpDeliveryChannel`; no implementation is registered.
- Files define provider handlers and include a local provider.
- The frontend has typed clients only for the currently surfaced UI modules.

There is no verified webhook platform, message broker, general outbox, payment
gateway, accounting connector, or inventory service contract.
