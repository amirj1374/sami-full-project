# Legacy Asan import and comparison

## Purpose and safety boundary

The Legacy Data Import Center accepts an Asan RAR archive or a manifest-driven ZIP package containing XLSX/XLS source exports, records its provenance, inventories its contents, and imports only records whose storage format can be verified. Imported data is written exclusively to `legacy_*` staging tables. It does not create or update customers, products, invoices, inventory, accounting entries, or any other canonical SAMI record.

Access is tenant-scoped and permission-gated. New permissions are granted only to administrator and super-administrator roles by default. Archive access and lifecycle actions are summarized in `legacy_import_audit_logs`; source records are not copied into general-purpose audit messages.

## Archive controls

- RAR and ZIP signatures are checked; the Excel package must include `SAMI_ERP_Migration_Manifest.xlsx`.
- Upload, file-count, expanded-total, and per-entry limits are configurable under `app.legacy-import`.
- Absolute and parent-traversal entry paths are rejected before extraction.
- Files are extracted under generated names in an isolated operating-system temporary directory, which is deleted after processing.
- RAR4 uses Junrar. RAR5 uses the configured `app.legacy-import.unrar-executable` and argument-safe `ProcessBuilder` invocation. Production hosts processing RAR5 must install UnRAR and set this property when it is not on `PATH`.
- Password-protected, corrupt, unsupported, or over-limit archives fail closed.
- ZIP entry names are traversal-checked and case-insensitive duplicate paths are rejected. XLSX/XLS workbooks are parsed through Apache POI without executing macros or following external links.

## Manifest-driven Excel package

The package supplied on 2026-08-19 has SHA-256
`5649E744CF7A2E36888B7FF1CEFEDB5CCEC156190FB757113E2992B5AB2C1E13`.
It contains 37 files: the manifest, README, 35 source workbooks (34 XLSX and
one XLS). A production-adapter run parsed 35 datasets and 77,475 staging
records in about 12.5 seconds with zero parser errors. No source file is stored
in Git.

| Semantic staging type | Source rows |
| --- | ---: |
| Accounting journal | 26,016 |
| Daily operation | 22,372 |
| Bank transaction | 12,682 |
| Product movement | 8,430 |
| Account balance | 3,646 |
| Party | 2,963 |
| Inventory balance | 880 |
| Cash transaction | 454 |
| Trial balance | 32 |

The adapter preserves the original archive hash, per-file hash/path, worksheet,
source row identifier, approved legacy code, raw cell values, normalized values,
manifest target, field dictionary and control-column totals. Duplicate or blank
legacy keys are warnings for entity/balance datasets and remain reviewable; no
fuzzy match or destructive deduplication is performed.

The source trial balance has equal period debit and credit turnover, but the
standalone journal and daily-operation exports do not balance by themselves.
They are therefore evidence inputs, not independently promotable accounting
journals. Canonical accounting, bank and cash owners do not yet exist in SAMI,
so final promotion remains blocked until those contracts and a reviewed
reconciliation report are approved.

## Evidence from `ji14050511.rar`

The supplied archive was inspected read-only outside the repository. It contains 190 `.DAT` files and expands to 13,661,600 bytes. A complete parser run produced 190 file inventory rows, 57 datasets, and 184 informational/warning messages.

Six files have a verified Microsoft Jet database signature and are read with Jackcess in read-only mode. Their encoding is reported by the format as UTF-16LE:

| File | Tables | Verified rows in supplied archive | Semantic classification |
| --- | --- | ---: | --- |
| `Det_rowf.dat` | `Det_Row`, `Row_Fac` | 0 | Serial detail / invoice-line reference |
| `matn_fac.dat` | `sh_cus_fac` | 1 | Unknown invoice text/template |
| `not_shsa.dat` | `Shs_not` | 0 | Note |
| `sh_tm_n.dat` | `EdareKol`, `Foroush_Detail`, `My_Zone` | 40 / 0 / 7,401 | Tax office / customer / geography reference |
| `sms_list.dat` | `Sms_Table` | 0 | Message |
| `Zemant_db.dat` | `Foroshande_Nam`, `Model_nam`, `Serial_Tbl`, `Tarh_zemanat` | 0 / 0 / 0 / 2 | Warranty seller / model / serial / plan |

FastReport definitions expose report field names but contain no source records. They are classified `PARTIAL`, with schema-only dictionaries and medium confidence. Other proprietary binary files are inventory-only (`UNSUPPORTED`) because neither their record offsets nor Persian character codec can be established from the archive. Guessing either would risk silent data corruption.

The UI and API never expose sample source values in field dictionaries; dictionary samples are redacted. Staged raw values remain available only behind `legacy-import:view`.

## Normalization and comparison

Normalization applies NFKC, Arabic-to-Persian Yeh/Kaf conversion, whitespace/ZWNJ cleanup, Persian and Arabic digit conversion, code canonicalization, and Iranian phone-prefix normalization. Raw values remain unchanged beside normalized values.

Automatic comparison is deliberately conservative. RAR customer records use exact tenant-scoped customer codes. Manifest-driven package records may also use exact tenant-scoped supplier codes and product SKUs. A party code that resolves to both a customer and supplier is ambiguous. Unsupported datasets, absent identifiers, accounting rows and all weaker similarities remain `unmapped`; no fuzzy name match is silently accepted.

## Lifecycle and APIs

Lifecycle: `UPLOADED → ANALYZING → READY → IMPORTING → COMPLETED` (or `COMPLETED_WITH_WARNINGS` / `FAILED`). Re-analysis replaces prior staging evidence atomically; import is allowed only from `READY`.

API root: `/api/v1/legacy-imports`. It supports list, upload, detail, analyze, staging import, file/dataset/record/message inspection, comparison, and deletion. The Vue route is `/legacy-imports`.

## Operational notes

- The original archive is stored through SAMI's `FileStorage` abstraction using an opaque key.
- Duplicate archive hashes are rejected per tenant and source system.
- Batch deletion removes database staging rows and the stored archive.
- Retention scheduling is not automatic in this revision; administrators delete batches explicitly.
- No archive or extracted legacy data belongs in Git.
