# SAMI ERP — Asan Migration Handoff

Date: 2026-08-19

Branch: `development`

Validation base before the handoff commit: `daa163c0c6e99f58276b874486637ffc93d88f18`

## Handoff summary

The supplied Asan accounting migration package is supported through a new
manifest-driven ZIP/XLSX/XLS adapter. The implementation is deliberately
staging-only: it records source provenance, dataset mappings, source rows,
legacy identifiers, field dictionaries and reconciliation metadata without
creating, updating or deleting canonical SAMI business records.

The package itself is not tracked in Git. Its verified SHA-256 is:

`5649E744CF7A2E36888B7FF1CEFEDB5CCEC156190FB757113E2992B5AB2C1E13`

Production-adapter inspection produced 35 datasets and 77,475 staging records
with zero parser errors. Migration V45 adds the dataset metadata required to
preserve manifest and reconciliation evidence; no historical Flyway migration
was changed.

## Reconciliation checkpoint

- Trial balance: period debit and credit both equal `6,201,456,640,469`; ending
  debit and credit both equal `1,551,656,422,473`.
- Account balances: movement net, balance-column net and side-balance net all
  equal `15,414,910,000`; 782 repeated-code rows are exact duplicates rather
  than conflicting values.
- Parties: 2,963 unique parties; 2,863 codes match the account-balance export
  with exact amount agreement. The other 100 require a business definition of
  whether absence means zero/no activity.
- Cash: after excluding `301,042` of numeric row/invoice identifiers stored in
  an amount column, the source reconstructs the trial-balance cash ending value
  of `149,500,000`.
- Banks: detailed ledgers are internally consistent, but their aggregate net is
  `699,837,000` higher than the trial-balance bank row. This is a conditional
  discrepancy, not a declared accounting error, because the package does not
  state a common extraction cutoff or the exact bank-account inclusion map.
- Inventory: 858 unique product codes and five cross-warehouse repeats were
  identified. Seventeen blank-code summary rows must not become products, and
  the zero-stock warehouse must still be preserved.
- Currency unit is not authoritative in the package. No rial/toman conversion
  or divide-by-ten operation has been applied.

The employer-facing Persian reconciliation document is committed as
`SAMI_ERP_ASAN_MIGRATION_RECONCILIATION_REPORT_FA.docx`.

## Validation executed on 2026-08-19

- `mvn clean verify`: PASS — 244 tests, 0 failures, 0 errors, 0 skipped; Java 21
  compiler target and Maven 3.9.16.
- `npm test`: PASS — 25 tests.
- `npm run type-check`: PASS.
- `npm run build`: PASS — Vite production bundle generated.
- `node scripts/validate-documentation.mjs`: PASS.
- `node --test scripts/validate-documentation.test.mjs`: PASS — 5 tests.
- Persian Word report: rendered to five pages and visually inspected; privacy
  metadata scrub and accessibility audit both passed with zero findings.

## Environment-blocked validation

Docker CLI 29.7.2, Compose 5.3.1 and Buildx 0.36 are installed, but the Docker
Desktop Linux engine is not running on this workstation. OpenSSH is not on the
current PATH. Therefore none of the following is reported as passed:

- fresh PostgreSQL 16 Flyway V1 through V45;
- database-backed staging and tenant-isolation validation;
- backend and frontend `linux/amd64` image builds;
- production Compose, nginx proxy and container health checks;
- live browser upload/analyze/stage/compare flow against the real database.

No Docker containers, networks or volumes were created by this validation, and
no database was modified.

## Continue from home

1. Pull `development` and verify the reported package SHA-256 before upload.
2. Start a unique disposable PostgreSQL 16/production-like Compose project.
3. Verify fresh Flyway V1 through V45 and database-backed staging isolation.
4. Build both images for `linux/amd64` and verify OCI revision provenance.
5. Run the authenticated Legacy Import flow: Analyze, Stage, Compare and export
   discrepancy evidence. Do not add canonical promotion before its accounting,
   bank, cash, warehouse and approval contracts are explicitly approved.
6. Resolve the bank cutoff/account-inclusion question and the currency unit with
   the employer before accepting final migration totals.
7. Tear down only the disposable Compose project and its explicitly temporary
   volumes; preserve unrelated and persistent data.

## Delivery status

Repository-only implementation and tests are ready for continued development.
The feature is not yet approved for final canonical accounting import because
the infrastructure gates and the employer decisions listed above remain open.
