# Legacy Asan import and comparison

## Purpose and safety boundary

The Legacy Data Import Center accepts an Asan RAR archive, records its provenance, inventories its contents, and imports only records whose storage format can be verified. Imported data is written exclusively to `legacy_*` staging tables. It does not create or update customers, products, invoices, inventory, accounting entries, or any other canonical SAMI record.

Access is tenant-scoped and permission-gated. New permissions are granted only to administrator and super-administrator roles by default. Archive access and lifecycle actions are summarized in `legacy_import_audit_logs`; source records are not copied into general-purpose audit messages.

## Archive controls

- File extension and RAR signature are both checked.
- Upload, file-count, expanded-total, and per-entry limits are configurable under `app.legacy-import`.
- Absolute and parent-traversal entry paths are rejected before extraction.
- Files are extracted under generated names in an isolated operating-system temporary directory, which is deleted after processing.
- RAR4 uses Junrar. RAR5 uses the configured `app.legacy-import.unrar-executable` and argument-safe `ProcessBuilder` invocation. Production hosts processing RAR5 must install UnRAR and set this property when it is not on `PATH`.
- Password-protected, corrupt, unsupported, or over-limit archives fail closed.

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

Automatic comparison is deliberately conservative. Only supported records classified as `CUSTOMER` are eligible, and only an exact `legacy_code = customers.customer_code` match inside the current tenant is classified automatically. Unsupported datasets, absent identifiers, and all weaker similarities remain `unmapped`; no fuzzy name match is silently accepted. The supplied archive has zero verified `Foroush_Detail` rows, so it truthfully yields no customer matches.

## Lifecycle and APIs

Lifecycle: `UPLOADED → ANALYZING → READY → IMPORTING → COMPLETED` (or `COMPLETED_WITH_WARNINGS` / `FAILED`). Re-analysis replaces prior staging evidence atomically; import is allowed only from `READY`.

API root: `/api/v1/legacy-imports`. It supports list, upload, detail, analyze, staging import, file/dataset/record/message inspection, comparison, and deletion. The Vue route is `/legacy-imports`.

## Operational notes

- The original archive is stored through SAMI's `FileStorage` abstraction using an opaque key.
- Duplicate archive hashes are rejected per tenant and source system.
- Batch deletion removes database staging rows and the stored archive.
- Retention scheduling is not automatic in this revision; administrators delete batches explicitly.
- No archive or extracted legacy data belongs in Git.
