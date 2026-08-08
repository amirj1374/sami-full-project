# SAMI ERP next-phase implementation report

## Current state

- Starting synchronized revision: `9c436a82bf23932965c15c1a55edc582364b2395` on `development`.
- No deployment was performed.
- Customer Purchase recovery: commit `744ca38` was absent from local refs, remote refs, reflogs, the commit graph, dangling objects, and available bundles/worktrees. The capability was reimplemented from the authoritative branch.

## Workstation readiness

- Existing: Git 2.49, Node.js 22.19, npm 10.9.3.
- Installed from official distributions under `%LOCALAPPDATA%\SamiDevTools`: Microsoft OpenJDK 21.0.12 and Apache Maven 3.9.16. User `JAVA_HOME`, `MAVEN_HOME`, and `PATH` were updated.
- Docker Desktop/Compose/Buildx and Windows OpenSSH Client remain unavailable because Windows requires an elevated installer/Optional Feature operation. A logoff may be required for already-running shells to inherit the user PATH changes.

## Customer Purchase / Trade-in

- Migrations: `V38__customer_origin_purchases.sql`.
- A purchase now records exactly one seller: a Supplier or an existing CRM Customer. Customers are not converted to suppliers.
- Customer-origin purchases record tenant-valid company/branch ownership, condition, inspection notes, ownership declaration, valuation, settlement status/details, and an optional linked Sale.
- A linked Sale must belong to the same tenant, customer, company, and branch and must not be cancelled. A Sale can be linked to at most one purchase per tenant.
- Supplier purchasing remains backward compatible. Customer purchase lifecycle events are appended to CRM history.
- Inventory receipt uses the existing inventory boundary and is refused while customer settlement is pending. `SETTLED` and explicitly `WAIVED` are auditable alternatives.
- IMEIs must contain 15 digits and pass the Luhn checksum; serial/IMEI uniqueness remains database enforced.
- No accounting postings were invented. Settlement is an auditable purchasing snapshot pending an approved accounting integration contract.

## Font and form regression

- Vazirmatn Variable remains the single global application font. The production bundle emits Arabic, Latin and Latin Extended WOFF2 assets; the service worker includes font assets in runtime caching.
- Browser computed styles confirmed Vazirmatn inheritance on the application, body and native inputs. Purchasing and Data Quality were checked at 360, 375, 390, 412, 430 and desktop widths without settled horizontal overflow.
- The global Vuetify defaults, form rhythm, dialog structure, mobile sticky actions and typography ownership remain centralized. No per-component font overrides were added.

## Completed partial module

- Data Quality was selected because its schema, tenant-aware backend, permissions, API client, localized UI, issue workflow, rule workflow, responsive states and tests already had canonical owners.
- `V39__activate_data_quality_module.sql` marks only Data Quality active/production-ready, and `/data-quality` now resolves to its real permission-gated screen.
- Files/Media and Appointments/Resources remain intentionally unrouted partial modules.

## Web Push decision and proposed contract

Real Web Push was not implemented because it introduces a new security-sensitive canonical owner and secret contract that requires architecture approval. Existing code supplies browser capability detection, service-worker push/click handling, and local opt-in state, but no backend subscription persistence or VAPID delivery owner.

Proposed approval contract:

1. The staff notification module owns `web_push_subscriptions` keyed by tenant, user and endpoint hash, storing endpoint, P-256 DH key, auth secret, expiration, user agent, enabled/revoked timestamps and optimistic version. Endpoint/key material is encrypted at rest where the deployment platform supports it.
2. Authenticated endpoints: `GET /api/v1/push/public-key`, `PUT /api/v1/push/subscriptions`, and `DELETE /api/v1/push/subscriptions/{endpointHash}`. Trusted tenant/user identity comes only from the security context.
3. `SAMI_WEB_PUSH_VAPID_PUBLIC_KEY`, `SAMI_WEB_PUSH_VAPID_PRIVATE_KEY`, and subject/contact are environment/secret values. The application refuses delivery when incomplete; no private key or generated keypair enters Git.
4. Staff notification creation remains the business boundary. An after-commit delivery adapter fans out to active subscriptions; business modules never invoke browser push directly.
5. Delivery treats HTTP 404/410 as terminal revocation, retries 429/5xx with bounded exponential backoff, records attempts without key material, and enforces idempotency by notification/subscription.
6. The frontend obtains the public key, persists subscriptions only after explicit browser permission, deletes the server record on opt-out, and renews through `pushsubscriptionchange`. Notification click routes are restricted to same-origin application paths.

## Validation and limitations

- Backend baseline: 202 tests passed before changes. Final verify includes focused IMEI tests.
- Frontend: release contracts, localization parity, type-check and production build pass; Data Quality is emitted as a production chunk and WOFF2 assets are present.
- Fresh PostgreSQL/Flyway validation and Linux Docker image/Compose health checks remain blocked by the elevated Docker installation requirement.
- iOS/Safari and installed-PWA device testing are not available on this Windows workstation; service-worker behavior was validated by source/build contract and browser preview where feasible.

## Recommended next tasks

1. An administrator installs Docker Desktop and the Windows OpenSSH Client, then signs out/in if required.
2. Run the fresh PostgreSQL Flyway V1-through-V39 gate and Linux/amd64 image/Compose health validation.
3. Approve or revise the Web Push ownership and secret contract before implementation.
4. Add a canonical settlement/accounting adapter only after Accounting publishes its posting contract.
