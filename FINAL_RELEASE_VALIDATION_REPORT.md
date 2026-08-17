# Final release validation report

## Status

NOT_READY_FOR_RELEASE

## Release-blocker remediation

- The original fresh PostgreSQL 16 failure was in `V39__activate_data_quality_module.sql`: it wrote `modules.is_active`, a column that has never existed in the canonical module schema. `modules.enabled` is the canonical activation flag established in V3 and retained by V25. V39 was corrected in `b534ce4`; no repository evidence shows a successful application of the broken V39 on the canonical schema.
- The two skipped `AsanLegacyImportAdapterTest` cases depended on a customer archive supplied through a JVM property and an external UnRAR binary. They were replaced with a deterministic, non-PII RAR5 fixture and a temporary test executable that exercises the adapter's RAR5 listing, staging, classification, and file-limit paths. The full backend gate now reports 227 tests, 0 failures, 0 errors, and 0 skipped.
- Fresh PostgreSQL 16 application startup then revealed a separate circular dependency in Market Sync scheduling. `a9fa584` changes the handler's dependency to Spring `ObjectProvider`, preserving execution behavior while deferring service resolution until a job runs. This breaks `JobHandlerRegistry -> MarketSyncJobHandler -> MarketSyncService -> JobService -> JobHandlerRegistry`.

## Evidence

- Fresh database: backend logs show `Successfully validated 42 migrations`, schema version `42`, and Hibernate initialization.
- Application startup: backend became `healthy`; scheduler registered seven handlers including `market-sync.source`.
- Backend full gate before the scheduler-only remediation: `mvn clean verify` passed with 227 tests, 0 failures, 0 errors, 0 skipped.
- Focused post-remediation Market Sync tests: 8 tests passed, 0 failures, 0 errors, 0 skipped.
- Fresh linux/amd64 backend image for `a9fa584df094ef8f3318a0785c91ab516d6b37c0` was built as `sami-backend:release-a9fa584`.

## Remaining release gates

The scheduler remediation has not yet been followed by the complete final gate on the final SHA: full backend verify, fresh frontend image tagged with the final SHA, frontend regression commands, production Compose frontend/nginx checks, and authenticated browser/mobile smoke. These are required before release approval.

## Disposable validation cleanup

- Removed containers: `sami-release-b534ce4-{db,backend,frontend}-1` and
  `sami-release-e497-{db,backend,frontend}-1` (six total).
- Removed networks: `sami-release-b534ce4_default` and
  `sami-release-e497_default`.
- Removed explicitly temporary, project-prefixed volumes: the `db-data`,
  `uploads`, `managed-files`, and `file-staging` volumes for each release test
  project (eight total).
- Preserved persistent volumes and all containers outside those two unique
  disposable Compose projects.

## Live RC continuation evidence — 85c990152d065b0f7ea5eaae15747ca96b255b2d

### Subject and environment

- Application release subject: `development` at
  `85c990152d065b0f7ea5eaae15747ca96b255b2d`; the worktree was clean before
  this continuation.
- Preserved linux/amd64 images: backend
  `sha256:c05941d34f9e3613ee1eaf97887ca20f029219565c4fad7f4472b07029059ff7`
  and frontend
  `sha256:d71daeadb70b79122dabf2c8826df5973849c4624dd036fbd01755e18b94c576`.
  Both carry the OCI revision label for the application release subject.
- Fresh disposable Compose project: `sami-rc-browser-85c9901`. PostgreSQL 16,
  backend and frontend were healthy. Backend logs record successful validation
  and application of Flyway V1 through V42.

### Executed live gates

| Gate | Status | Evidence |
| --- | --- | --- |
| Authenticated navigation / permission menu | PASS | The browser session logged in as the disposable bootstrap administrator and loaded Dashboard, Purchasing, HAMTA, Legacy Asan, Data Quality, Market Sync and Sales routes. |
| Customer-origin purchase draft | PASS | A disposable customer and HAMTA-eligible product were created through the UI. Purchase `PUR-2026-000001` persisted after refresh with seller `RC Trade-in Customer`, used condition, valuation `8,000,000`, settlement reference and serial/IMEI requirements. |
| Draft submission and approval | PASS | The same persisted purchase transitioned Draft -> Pending Approval -> Approved through normal UI controls. |
| Purchase receiving / serialized-unit persistence | FAIL | Receiving one required-IMEI item failed when the frontend requested receipt history; see blocker RCB-01. This prevents reliable completion, inventory receipt verification and serialized-unit/HAMTA linkage validation. |
| HAMTA report/UI | FAIL | The live HAMTA report request fails in PostgreSQL when optional filters are null; see blocker RCB-02. The no-code and valid-code completion gates cannot be completed reliably. |
| Downstream Sales, CRM history and delivery evidence | FAIL | Dependent on the failed receipt/serialized-unit workflow. No completion claim was made. |
| Legacy Asan persisted smoke | BLOCKED_BY_TEST_DATA | No real or sanitized RAR archive fixture is tracked in the repository; no customer archive was used. Route and staging-only UI were verified earlier in this RC session. |
| Remaining browser, locale, viewport, PWA-cache and screenshot gates | BLOCKED_BY_TOOLING | The live validation was deliberately stopped after the two reproducible production runtime failures above, as required by the continuation instruction. No pass is claimed for these remaining gates. |

### Release blockers

#### RCB-01 — CRITICAL: receipt-history query crashes after a receiving attempt

- Failed gate: Customer Purchase receiving / serialized IMEI flow.
- Exact runtime error: `org.hibernate.loader.MultipleBagFetchException: cannot
  simultaneously fetch multiple bags: [PurchaseReceiptItem.identifiers,
  PurchaseReceipt.items]`.
- Affected path: `PurchaseReceivingService.history` (line 66) calling
  `PurchaseReceiptRepository.findByPurchaseIdAndTenantIdOrderByCreatedAtDesc`,
  surfaced by `PurchaseController.receipts` (line 121).
- Classification: source-code defect, reproduced against fresh PostgreSQL 16
  in the final linux/amd64 RC image.
- Smallest safe remediation: change the receipt-history fetch strategy so it
  does not join-fetch both bag collections in one Hibernate query; add a
  PostgreSQL integration regression test which opens a receipt history after a
  serialized receiving attempt.

#### RCB-02 — HIGH: HAMTA report query fails with nullable filters

- Failed gate: live HAMTA report / activation-code verification.
- Exact runtime error: `org.postgresql.util.PSQLException: ERROR: could not
  determine data type of parameter $2` from the `HamtaService.report` JDBC
  query when its optional `delivered` filter is null.
- Affected path: `HamtaService.report` (line 146), surfaced by
  `HamtaController.report` (line 51).
- Classification: source-code defect, reproduced against fresh PostgreSQL 16
  in the final linux/amd64 RC image.
- Smallest safe remediation: build the optional predicates dynamically or use
  explicitly typed JDBC parameters; add integration coverage for absent,
  false, and true report filters.

### Continuation cleanup

- Removed containers: `sami-rc-browser-85c9901-{db,backend,frontend}-1`
  (three).
- Removed network: `sami-rc-browser-85c9901_default` (one).
- Removed explicitly temporary volumes:
  `sami-rc-browser-85c9901_{db-data,uploads,managed-files,file-staging}`
  (four).
- Preserved all images, including both RC images above, and all Docker
  resources outside the unique disposable project.

## Current decision

NOT_READY_FOR_RELEASE. The two failures above are reproducible application
runtime defects in a fresh PostgreSQL 16 production-like RC stack. No source
code was modified during this validation continuation.

## RC a766b10 authentication and browser-tooling classification

- Application release SHA: `a766b10b13efc7093d5751e5ed1bed3458cdaa20`.
- **AUTHENTICATION PRODUCT GATE: PASS.** A fresh disposable PostgreSQL 16
  stack seeded `admin@sami.local` through `DataInitializer`; its BCrypt hash,
  active status, login-enabled flag and Super Administrator role were verified
  through the real database. Proxied `POST /api/v1/auth/login` returned HTTP
  200 and protected API routing returned the expected authenticated result.
- **BROWSER AUTOMATION LOGIN: BLOCKED_BY_TOOLING.** The available browser
  sandbox submitted the same valid credentials as HTTP 403 and does not expose
  `localStorage` to its evaluation surface. The frontend's supported mechanism
  stores the legitimate access/refresh tokens as `sami.accessToken` and
  `sami.refreshToken`; API-issued tokens could not be injected because the
  sandbox reports `localStorage` as undefined. This is not an application or
  authentication defect.
- Authenticated UI/RTL/responsive business workflows therefore require the
  human checklist at `docs/release/MANUAL_RC_BROWSER_CHECKLIST.md`; each is
  **MANUAL_BROWSER_FOLLOW_UP**, not `NOT_RUN`.

## Deployment candidate — 2026-08-17

- **APPLICATION_SOURCE_SHA:** `8c019753e76153487bd9541e47ae8d77ae023d78`
  (`development`). This includes the final purchase and sales product-lookup
  reliability correction; it is distinct from any later documentation-only
  commit.
- Backend source gate: `mvn clean verify` completed in the official Maven
  Java 21 container with **240 tests, 0 failures, 0 errors, 0 skipped**. The
  initial backend-only bind mount was not counted because a cross-monorepo
  contract test intentionally reads the sibling frontend; the rerun mounted
  the repository root and passed.
- Frontend source gate: `npm ci`, `npm test` (**25 tests**),
  `npm run type-check`, and `npm run build` passed. The source-contract tests
  include exact English/Persian localization parity.
- Fresh PostgreSQL 16 Compose gate: Flyway validated and applied **V1–V44**;
  Hibernate/Spring Boot started successfully; PostgreSQL, backend and frontend
  health checks were healthy. Nginx SPA, `/health`, login, and authenticated
  `/api/v1/menu` and `/api/v1/users/me` requests returned HTTP 200.
- Validated `linux/amd64` images:
  - backend `sami-backend:test` —
    `sha256:95330a3d3dd573ac8e54c136f9918f2b37e2dcd1b2d31c2f113f01dfb852ce6a`
  - frontend `sami-frontend:test` —
    `sha256:c8c6fee83c7ada9b60296f8c584bb9319c5486c0cf4b4e402f0e23581fd45a98`
  Both OCI revision labels equal the application source SHA.
- Prepared ignored deployment artifacts:
  `deployment-artifacts/sami-backend-test.tar` (SHA-256
  `CAF091126AA17675D235EF3245BACE2282A4B14C9E016E7183D79DE00C835EA2`) and
  `deployment-artifacts/sami-frontend-test.tar` (SHA-256
  `66B9CC8549B8ED1B0279DF734A8710FF2766442BBA2573F05D3C1BBA1B90C50D`).
  Their TAR manifest configs match the validated image config IDs.
