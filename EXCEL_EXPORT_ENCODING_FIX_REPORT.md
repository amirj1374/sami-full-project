# Excel and CSV Encoding Fix Report

**Date:** 2026-07-31  
**Branch:** `codex/final-sami-release`

## Root cause

SAMI exports are CSV text, not XLSX workbooks. The CSV generators returned
valid Java Unicode strings, and frontend download handlers converted those
strings directly to browser `Blob` objects without binary corruption. However,
the generated files had no UTF-8 byte-order mark and most responses declared
only `text/csv` without an explicit charset. Microsoft Excel can therefore
interpret the bytes using a legacy Windows code page, making Persian headers
and values unreadable even though browsers and UTF-8-aware editors display the
same file correctly.

## Backend changes

- Every CSV generator now begins its output with Unicode `U+FEFF`. When encoded
  as UTF-8 by Spring or the browser Blob API, the first bytes are exactly
  `EF BB BF`, which enables Excel's UTF-8 detection.
- Empty report exports also contain the encoding marker.
- CSV response media types now explicitly declare `charset=UTF-8` for Users,
  Customers, Suppliers, KPIs, Files, Knowledge, and Licensing reports.
- Existing API paths, permission checks, row values, quoting, dates, numbers,
  and filenames remain unchanged.

## Frontend investigation

The Users, Customers, Suppliers, KPI, and Dashboard export clients request text
and create a Blob from the returned Unicode string. That preserves the BOM and
Persian characters. Binary download endpoints already use `responseType:
'blob'`. No random transcoding or character replacement was introduced.

## Tests

`CsvEncodingTest` covers Persian headers, Persian values, mixed Persian/English
text, a numeric value, and an empty export. It verifies the exact UTF-8 prefix
bytes and round-trip content. Backend compilation/test results are recorded in
the final release report.

## Before / after

- **Before:** UTF-8 CSV content without a detection marker; Excel could open it
  using a legacy code page and display Persian as mojibake.
- **After:** UTF-8 CSV with BOM plus an explicit UTF-8 response charset; Excel,
  LibreOffice, Google Sheets import, browsers, and UTF-8 editors can identify
  the character encoding without altering business data.

## Limitation

These endpoints intentionally remain CSV exports. Spreadsheet-native styling,
worksheet RTL flags, fonts, formulas, and column types would require a separate
approved XLSX export contract and library.
