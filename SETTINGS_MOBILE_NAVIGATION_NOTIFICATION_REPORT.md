# Settings, Mobile Navigation, and Notification Report

## Delivered

- The existing PWA install composable is the single installation authority. Banner dismissal is stored separately from install capability, and Settings permanently exposes installed, promptable, iOS-guidance, and unsupported states.
- `/profile` now contains mobile-first Application Installation, Mobile Bottom Navigation, and Notification Preferences sections in Persian/RTL and English/LTR.
- Mobile navigation choices are persisted per authenticated user through `GET/PUT /api/v1/users/me/preferences`. The backend validates a maximum of four unique, enabled, routed, permission-allowed menu codes; stale entries are sanitized and defaults fill available slots.
- The notification capability is classified as **PARTIAL**: SAMI now has a real tenant/user-scoped in-app inbox and unread badge. Client Web Push foundations exist, but no server subscription/VAPID delivery provider exists, so closed-app push is not faked.
- The existing scheduler owns the `notification.demo-hourly` handler. Delivery is disabled by default, requires per-user opt-in, uses an hourly idempotency key, and records localized message keys rather than hardcoded business events.

## Persistence and APIs

- Flyway `V37__user_experience_preferences_and_notifications.sql`
- `user_experience_preferences`: one row per tenant/user, mobile menu JSON and demo opt-in.
- `staff_notifications`: tenant/user-scoped inbox with read state and unique hourly idempotency key.
- APIs: `/api/v1/users/me/preferences`, `/api/v1/notifications`, unread count, read, and read-all.

## Configuration

Enable only in an explicit demo deployment:

```text
DEMO_HOURLY_NOTIFICATION_ENABLED=true
```

Production defaults to `false` in application and Compose configuration.

## Verification completed

- Frontend type-check, 9 contract/localization tests, and production build passed.
- Backend Java 21 compile passed; focused preference/notification tests passed (6/6); full backend package passed (202 tests) when run with the monorepo root mounted.
- Production backend/frontend Docker images built and Compose configuration validated.
- Fresh PostgreSQL startup applied all 37 migrations and all services became healthy.
- Authenticated browser verification covered Settings, unsupported install guidance, preference reorder/remove/save/reload, real scheduler delivery, inbox badge, Persian RTL, English LTR, and widths 360/375/390/412/430 without horizontal overflow.

## Limitations

- Closed-application Web Push remains unavailable until a backend subscription contract, VAPID configuration, provider, and delivery worker are approved and implemented.
- The migration seeds the demo scheduler for tenants existing at migration time. Future tenant provisioning must create the system job through the existing Scheduler service.
