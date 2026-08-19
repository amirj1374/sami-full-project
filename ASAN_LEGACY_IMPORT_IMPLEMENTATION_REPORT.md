# Asan legacy import implementation report

## Delivered

- Secure RAR4/RAR5 ingestion with signature, traversal, count, size, expansion, encryption/corruption, and temporary-workspace controls.
- Read-only Microsoft Jet parser and schema-only FastReport discovery.
- Provenance-preserving, tenant-scoped staging schema with batch/file/dataset/record/message/comparison/review/audit ownership.
- Explicit unsupported-file inventory with no guessed proprietary record layout.
- Upload, analysis, import, evidence browsing, comparison, and deletion APIs protected by seven dedicated permissions.
- Exact-code-only customer comparison that cannot cross tenant boundaries.
- Persian/English responsive Vue import center with RTL/LTR support and field-dictionary inspection.
- Unit coverage for normalization, signature validation, and path traversal defenses.
- Manifest-driven ZIP/XLSX/XLS staging with per-file provenance, source-row IDs, legacy codes, field dictionaries, mapping metadata and reconciliation totals.
- Conservative exact-code comparison for customers, suppliers and product SKUs; canonical writes remain zero.

## Supplied archive validation

The production parser was run directly against `ji14050511.rar`: 190 files, 57 datasets, and four non-empty verified tables (`sh_cus_fac` 1, `EdareKol` 40, `My_Zone` 7,401, `Tarh_zemanat` 2). No source file was added to the repository. Because the verified customer table is empty, this archive provides no customer comparison rows; the system reports zero rather than fabricating matches.

## Intentional limits

- Proprietary fixed-record files remain unsupported until an authoritative layout and codec are supplied.
- FastReport definitions contribute field dictionaries only, not data rows.
- Import remains staging-only. Promotion into canonical SAMI modules requires a separately approved mapping/reconciliation workflow.
- The supplied accounting package parses to 77,475 staging records, but canonical accounting/bank/cash owners are not implemented. Final accounting promotion is therefore blocked rather than guessed.
- RAR5 processing requires the official UnRAR executable on the backend host.
- An infrastructure-backed Flyway migration run requires PostgreSQL/Docker and is part of environment deployment validation, not this repository-only implementation.

## Rollback

Before release, revert the feature commit. After applying migration V40, application rollback can disable module `legacy-import`; database rollback must archive required audit evidence, delete `legacy_*` tables in foreign-key order, remove `legacy-import:*` permissions, then remove the module row. Flyway history must be reconciled through the normal controlled database procedure; migration history must not be edited ad hoc.
