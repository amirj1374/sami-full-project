# SAMI ERP PWA implementation report

## Implementation summary

SAMI ERP now has an installable, production-only Progressive Web App layer that preserves the existing Vue, routing, authentication, permission, localization, and deployment architecture.

The implementation adds:

- a standards-based web app manifest;
- a same-origin service worker for the application shell and static assets;
- Android, iOS, favicon, maskable, and splash-friendly brand assets;
- a lightweight branded startup screen;
- install, offline, retry, and update-available experiences;
- a provider-neutral Web Push permission, capability, subscription, payload, and lifecycle foundation;
- safe service-worker push display and same-origin notification-click routing;
- English and Persian messages with existing RTL behavior;
- service-worker and manifest cache headers in the production Nginx configuration.

No backend contract, business behavior, authentication flow, token storage, route, permission, dependency, or application data model was changed.

## Audit findings

Before this work, the frontend had responsive Vue/Vuetify foundations and basic mobile metadata, but it did not have a service worker, complete manifest, install lifecycle, PWA icons, offline shell, update flow, or connection-state UX. Routes were already lazy-loaded and the production Nginx configuration already supported SPA history fallback and immutable hashed assets.

Authentication currently stores access and refresh tokens through the existing local-storage token utility. The app restores a session through the existing authenticated profile request and retains its established refresh and unauthorized-response handling. This work deliberately does not duplicate, move, cache, or weaken that mechanism. Local-storage token exposure remains an existing security limitation compared with an HttpOnly-cookie design; changing it would alter the public authentication contract and is outside this PWA scope.

## Files changed

- `sami-frontend/index.html`
- `sami-frontend/nginx.conf`
- `sami-frontend/public/manifest.webmanifest`
- `sami-frontend/public/service-worker.js`
- `sami-frontend/public/icons/sami-app-icon.svg`
- `sami-frontend/public/icons/sami-32.png`
- `sami-frontend/public/icons/sami-180.png`
- `sami-frontend/public/icons/sami-192.png`
- `sami-frontend/public/icons/sami-512.png`
- `sami-frontend/public/icons/sami-maskable-512.png`
- `sami-frontend/src/App.vue`
- `sami-frontend/src/main.ts`
- `sami-frontend/src/components/AppPwaStatus.vue`
- `sami-frontend/src/composables/usePwa.ts`
- `sami-frontend/src/composables/usePushNotifications.ts`
- `sami-frontend/src/services/notificationPermissionService.ts`
- `sami-frontend/src/services/pushNotificationService.ts`
- `sami-frontend/src/types/pushNotifications.ts`
- `sami-frontend/src/locales/en.json`
- `sami-frontend/src/locales/fa.json`
- `PUSH_NOTIFICATION_FOUNDATION.md`
- `PWA_IMPLEMENTATION_REPORT.md`

## Manifest and branding

The manifest uses:

- application name: `SAMI ERP`;
- short name: `SAMI`;
- stable application ID and scope: `/`;
- start URL: `/?source=pwa`;
- display mode: `standalone`;
- orientation: `any`;
- theme color: `#26375F`;
- background color: `#F6F7F9`;
- default language/direction metadata: Persian/RTL;
- business and productivity categories.

The icon is a deterministic extension of the existing SAMI navy, brass, and “S” monogram treatment. PNG assets cover 32, 180, 192, and 512 pixels. A dedicated 512-pixel maskable declaration is included. The same visual is used by the pre-mount startup screen so first paint and installed-app launch remain visually consistent.

## Service-worker and caching strategy

The service worker is registered only in production builds. Installation precaches the minimal application shell: root document, SPA entry document, manifest, and primary icon.

Navigation uses network-first behavior. A successful navigation refreshes the cached SPA entry; if the network is unavailable, the service worker returns that entry so the router and existing authentication guards can start normally.

Same-origin hashed assets, fonts, icons, scripts, and styles use cache-first behavior after retrieval. Cache versions are named and old SAMI PWA caches are removed during activation. A waiting worker is activated only after the user chooses the localized update action.

The following are never handled or cached by the service worker:

- `/api/**`;
- non-GET requests;
- cross-origin requests;
- authenticated business records or API responses;
- access tokens, refresh tokens, and authorization headers.

Consequently, offline mode provides the static application shell and a clear connection warning, not offline ERP data access or mutation.

## Push-notification foundation

The frontend now exposes `usePushNotifications`, `pushNotificationService`, and `notificationPermissionService`. Together they provide feature detection, explicit permission requests, VAPID public-key subscription creation, browser-subscription discovery and removal, device-local preference intent, lifecycle refresh, and transport-neutral TypeScript contracts.

Permission is never requested during application startup. Subscription creation requires a direct future UI action and a VAPID public key supplied by the future authenticated backend configuration contract. The browser remains authoritative for permission and active subscription state; local storage contains only the versioned `{ "enabled": boolean }` preference and never stores endpoints or encryption keys.

The service worker accepts a generic payload containing title, body, icon, badge, same-origin URL, type, data, and optional generic actions. It rejects malformed payloads, unsafe assets, external routes, protocol-relative routes, and API routes. Notification clicks focus an existing SAMI client and navigate through Vue Router, or open a new same-origin application window.

No business notification types, backend APIs, database entities, VAPID secrets, provider dependencies, module events, or fake notifications were added. The required future backend contract, multiple-device persistence, VAPID configuration, security controls, and Notification Center → Communication Hub integration boundary are defined in `PUSH_NOTIFICATION_FOUNDATION.md`.

## Offline limitations

- Live dashboards, lists, forms, reports, authentication requests, and other API-backed features require connectivity.
- Previously fetched business responses are not exposed from a PWA cache.
- Unsaved form changes are not queued or synchronized.
- A route's lazy bundle is available offline only after that static bundle has previously been retrieved and cached; the core shell remains available.
- First installation requires a successful network load.

These limitations are intentional. Offline business-data support would require an approved security, encryption, conflict-resolution, retention, and synchronization architecture.

## Session behavior

Installed and browser modes use the same Pinia/authentication implementation:

- session restoration follows the existing token and `/users/me` flow;
- refresh behavior remains in the existing HTTP interceptors;
- token expiry and unauthorized responses retain the existing logout/login redirect behavior;
- logout continues to clear the existing token store;
- the service worker does not intercept API traffic or persist credentials.

## Browser support

- Chromium-based desktop and Android browsers: manifest, installation prompt, standalone mode, service worker, offline shell, and controlled updates.
- iOS/iPadOS Safari: manifest metadata, Apple touch icon, standalone home-screen launch, safe viewport behavior, and offline shell. Installation is initiated through Safari's Share → Add to Home Screen action because iOS does not expose Chromium's install-prompt event.
- Browsers without service-worker support continue to run the normal web application; registration failure is non-fatal.

Production deployment must use HTTPS, except for browser-supported localhost development.

## Validation results

- `npm.cmd run type-check`: passed.
- `npm.cmd run build`: passed; 939 modules transformed.
- Manifest and locale JSON parsing: passed.
- Service-worker JavaScript syntax check: passed.
- Localization parity: passed, 924 keys in each locale and zero missing keys.
- Icon inspection: passed for 32×32, 180×180, 192×192, and both 512×512 PNG assets.
- Production output inspection: manifest, service worker, and all icons present.
- Production Chrome simulation at 360, 375, 390, 412, and 430 px: manifest loaded, service worker registered, no page errors, and document width equal to viewport width.
- Persian RTL simulation at 390 px: `lang="fa"`, `dir="rtl"`, no page errors, and no horizontal overflow.
- Light and dark Vuetify themes verified at 390 px with no horizontal overflow.
- Push capability detection verified in production Chrome; startup retained `default` permission and created no subscription or preference entry.
- Offline production navigation: passed with HTTP 200 application shell and localized connection warning.
- Runtime cache inspection: two versioned shell/static caches, 56 static entries after the test session, and zero `/api` entries.
- Git diff/status review: performed; existing unrelated authorized work remains uncommitted and was not modified as part of PWA implementation.

Lighthouse was not available in the repository and was not added as a dependency. Browser installability should be rechecked over the final HTTPS deployment because secure-origin and response-header behavior are deployment-dependent.

## Remaining recommendations

These are non-blocking and intentionally left outside the implementation:

1. Run Chrome Lighthouse against the final HTTPS URL and retain the generated report as release evidence.
2. Perform physical-device checks on current Android Chrome and iOS Safari, including home-screen installation, standalone launch, virtual keyboard behavior, and safe-area rendering.
3. If offline business workflows become a requirement, define and approve a separate security and data-synchronization architecture before caching any API data.
4. Consider an authentication hardening project using server-managed HttpOnly cookies only if the backend authentication contract is explicitly approved for change.
