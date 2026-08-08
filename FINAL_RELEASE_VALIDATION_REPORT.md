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
