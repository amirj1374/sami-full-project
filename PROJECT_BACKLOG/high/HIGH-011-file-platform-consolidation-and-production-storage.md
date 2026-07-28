---
id: HIGH-011
title: Consolidate file ownership and add production storage
status: needs-decision
priority: high
type: architecture
area: files
owners: []
depends_on: [CRIT-001, HIGH-001]
blocks: [MED-003]
estimated_size: L
risk: high
source_refs: [sami-backend/src/main/java/com/sami/app/common/storage, sami-backend/src/main/java/com/sami/app/files, sami-backend/src/main/resources/application.yml, sami-backend/docker-compose.prod.yml]
---

# Consolidate file ownership and add production storage

## Summary
Choose the canonical file platform, migrate legacy consumers and provide a
durable production object-storage provider and volume contract.

## Why this is needed
`common.storage` and `files` overlap, while production persistence explicitly
covers only the legacy upload path.

## Current evidence
Avatars/supplier/purchase attachments use legacy storage; managed Files has its
own metadata, staging and provider model.

## Existing implementation
Local providers, managed file versions/scans/retention/quota and legacy consumers.

## Missing work and scope
Ownership ADR, consumer migration, production provider, durability,
authorization, reconciliation and disaster recovery.

## Dependencies
Tenant isolation and PostgreSQL tests.

## Business decisions required
Object-storage vendor, retention/legal hold, encryption, antivirus and maximum sizes.

## Proposed implementation approach
Stabilize managed Files contract; add provider; dual-read/migrate/checksum;
retire legacy only after verification.

## Impacts
Backend, deployment/config/schema, file APIs and attachment UIs.

## Security and tenant-isolation considerations
Per-object authorization, scoped keys, malware controls and no public buckets.

## Testing requirements
Provider contract, checksum, interruption, migration, authorization and capacity tests.

## Documentation requirements
Storage architecture, migration and recovery runbooks.

## Acceptance criteria
- [ ] Canonical owner/provider/retention are approved.
- [ ] Production content and staging are durable.
- [ ] Existing attachments migrate without loss.
- [ ] Authorization and tenant isolation pass.
- [ ] Legacy retirement has a measured rollback path.

## Validation commands
Provider integration suite, reconciliation report and deployment health checks.

## Risks and rollback considerations
Keep source objects until checksum/count reconciliation succeeds.

## Relevant files
`common/storage`, `files`, avatar/supplier/purchasing attachment services and Compose.

## Notes for the next developer or AI agent
Never delete legacy files during initial migration.
