# SAMI ERP push-notification foundation

## Scope

This foundation prepares the existing staff-facing PWA for standards-based Web Push. It does not send notifications, define business notification rules, create fake events, or connect Workflow, Installment, Repair, CRM, or other business modules.

Notification Center remains the future decision layer. Business modules will publish domain events to Notification Center; Notification Center will decide whether a notification is appropriate and ask Communication Hub to deliver it.

## Current implementation

### Service worker

The existing production service worker now:

- handles JSON `push` events;
- requires a non-empty `title`;
- accepts the provider-neutral payload described below;
- uses SAMI icons when an icon or badge is omitted or unsafe;
- displays notifications through `ServiceWorkerRegistration.showNotification`;
- handles `notificationclick`;
- focuses an existing SAMI window and sends it a route-navigation message;
- opens a new same-origin SAMI window when no client is open;
- rejects external, protocol-relative, malformed, and non-application routes;
- reports `pushsubscriptionchange` to open application clients.

Malformed or empty messages are ignored. The worker does not synthesize demo or fallback business notifications.

### Frontend abstractions

- `usePushNotifications()` exposes capabilities, permission, subscription, preference, busy, error, refresh, enable, and disable state.
- `notificationPermissionService` reads and requests browser notification permission.
- `pushNotificationService` detects browser support, waits for the existing service worker, creates/reads/removes browser subscriptions, serializes subscription keys, and persists the local opt-in preference.
- `PushNotificationPayload` defines the future transport-neutral payload.
- `PushSubscriptionModel` defines the exact browser subscription material a future authenticated backend API must accept.

The application initializes capability and subscription observation at startup, but never requests permission automatically. A future settings control must call `requestPermission()` or `enable()` from a direct user interaction.

### Future payload

```json
{
  "title": "Localized title",
  "body": "Localized body",
  "icon": "/icons/sami-192.png",
  "badge": "/icons/sami-32.png",
  "url": "/notifications/opaque-id",
  "type": "notification.available",
  "data": {
    "notificationId": "opaque-id"
  },
  "actions": [
    {
      "action": "open",
      "title": "Open"
    }
  ]
}
```

Only `title` is required by the worker. `url`, `icon`, and `badge` must be same-origin application paths. Optional actions are transport-neutral identifiers and labels; Notification Center will define their future meaning. Payload `data` must remain minimal and must not contain access tokens, secrets, financial details, personal records, or other sensitive ERP data.

## Browser support

Support is based on feature detection, not user-agent detection. The foundation requires:

- a secure context;
- Service Worker;
- Push Manager;
- Notifications API.

Expected platforms:

- Android Chrome and compatible Chromium PWA installations;
- Chromium desktop browsers where Web Push is enabled;
- Firefox desktop/mobile implementations that expose the required standards;
- macOS Safari versions supporting standards-based Web Push;
- iOS/iPadOS 16.4 or later for web apps installed on the Home Screen.

On iOS/iPadOS, permission must be requested from a direct interaction inside the installed Home Screen web app. Browser and operating-system policy may still deny or revoke permission. SAMI therefore treats capability and permission as runtime state rather than a guaranteed platform feature.

References:

- [WebKit: Web Push for Web Apps on iOS and iPadOS](https://webkit.org/blog/13878/web-push-for-web-apps-on-ios-and-ipados/)
- [MDN: Push API](https://developer.mozilla.org/en-US/docs/Web/API/Push_API)
- [MDN: Web Push best practices](https://developer.mozilla.org/en-US/docs/Web/API/Push_API/Best_Practices)

## Storage strategy

### Browser

The browser owns the actual `PushSubscription`. SAMI reads it from `ServiceWorkerRegistration.pushManager`; it does not copy endpoint or encryption keys into local storage.

Local storage contains only:

```json
{
  "enabled": true
}
```

under the versioned key `sami.push.preferences.v1`. This is a device-local user preference, not proof that permission or a valid server subscription exists. Runtime permission and `PushManager.getSubscription()` remain authoritative.

### Future backend

The backend must persist subscription endpoint and encryption key material because it is required to deliver Web Push. These values are sensitive delivery credentials and must never be logged, audited verbatim, exposed through device-list responses, or included in notification payloads.

## Required backend APIs

These contracts are documentation only; they are not implemented by this change.

### Configuration

`GET /api/v1/notifications/push/config`

Authenticated response:

```json
{
  "enabled": true,
  "applicationServerKey": "URL-safe VAPID public key"
}
```

The private key must never be returned.

### Register or refresh this device

`POST /api/v1/notifications/push/subscriptions`

```json
{
  "endpoint": "https://push-service.example/...",
  "expirationTime": null,
  "keys": {
    "p256dh": "...",
    "auth": "..."
  },
  "deviceLabel": "optional user-visible label"
}
```

The server must derive `userId` and `tenantId` exclusively from the authenticated server-side principal. Registration should be idempotent by endpoint hash and update last activity.

### List current user's devices

`GET /api/v1/notifications/push/subscriptions`

Responses should contain opaque subscription ID, safe device label, created time, last activity, status, and revocation time. They must omit endpoint and encryption keys.

### Revoke a device

`DELETE /api/v1/notifications/push/subscriptions/{subscriptionUuid}`

The operation must be idempotent and restricted to the authenticated user's trusted tenant scope.

Future frontend synchronization will:

1. obtain backend configuration;
2. request permission only from explicit user interaction;
3. create the browser subscription;
4. register it with the backend;
5. refresh it on authenticated startup, foreground activity, and subscription change;
6. revoke it during explicit device removal or logout;
7. retain a server revocation retry when the browser is offline.

## Required database entity

A future Communication Hub-owned push-subscription entity should include:

- UUID public identifier;
- trusted tenant ID;
- authenticated staff user ID;
- endpoint;
- SHA-256 endpoint hash;
- `p256dh` key;
- `auth` key;
- browser expiration time;
- optional safe device label;
- created and updated timestamps;
- last activity timestamp;
- last successful delivery timestamp;
- revoked timestamp;
- last provider failure code;
- optimistic-lock version.

Required integrity:

- foreign keys to tenant and user;
- global uniqueness of active endpoint hash;
- indexes for active subscriptions by tenant/user;
- all reads, writes, updates, revocations, and uniqueness checks tenant-scoped;
- expired provider responses such as HTTP 404/410 revoke the subscription;
- no `TenantDefaults` fallback.

Communication Hub message/attempt records should remain the delivery history. A parallel push-delivery history table should not be introduced without a demonstrated need.

## Required VAPID configuration

The future backend requires:

- push enabled flag;
- VAPID subject, normally a controlled `mailto:` or HTTPS URI;
- VAPID public key;
- VAPID private key;
- message TTL;
- maximum payload size;
- delivery timeout.

The private key must be injected through production secret management, never committed to source, `.env.example`, frontend assets, images, or API responses. The public key should come from the authenticated configuration API so key rotation does not require rebuilding the frontend.

Push must fail closed when explicitly enabled with incomplete or invalid VAPID configuration. Development should remain disabled by default unless a deliberate local key pair is supplied.

## Future integration points

The future flow is:

```text
Business module domain event
    -> Notification Center policy and recipient decision
    -> persisted in-app notification
    -> Communication Hub delivery request
    -> Web Push provider handler
    -> active device subscriptions
```

Business modules must not:

- request browser subscriptions;
- read push endpoints;
- construct Web Push requests;
- select push providers;
- call a Web Push handler directly;
- hardcode notification templates or channel policy.

Notification Center will own types, templates, preferences, deduplication, recipient resolution, routes, and channel decisions. Communication Hub will own provider mechanics, retries, delivery status, and expired-device handling.

## Security considerations

- Permission requests require explicit user intent and must remain easy to revoke.
- Subscription endpoint URLs are bearer-like capabilities and must be protected.
- Push encryption keys and endpoints must not enter logs or audit details.
- The backend must not trust client-provided tenant or user identifiers.
- Push payloads should contain only enough information to notify and deep-link.
- The application must re-authorize all data access after a notification click; possession of a notification is not authorization.
- Routes must be same-origin and validated again by normal router guards and backend permissions.
- The service worker must never cache authenticated API responses as part of push support.
- Server registration APIs require rate limiting, validation, length limits, and tenant/user ownership checks.
- Subscription revocation and 404/410 provider cleanup must be idempotent.
- Notification preferences must be enforced by Notification Center before delivery.
- Silent background tracking must not be implemented; push messages are user-visible.

## Deliberately deferred

- backend subscription APIs and migrations;
- VAPID keys or Web Push provider dependencies;
- Notification Center business policy;
- in-app notification history;
- management announcements;
- approval, workflow, installment, repair, warranty, CRM, or task events;
- fake notification generators;
- test business events.
