# 0912 SIM Investment

## Purpose

This module imports 0912 SIM market snapshots, preserves their source evidence and calculates traceable investment signals. It is independent from canonical Inventory, Purchasing and Sales ownership: importing a file never creates or changes products, stock, purchases or sales.

## Import contract

- Accepted formats: UTF-8 CSV and XLSX, up to 20 MB and 100,000 source rows.
- Required first-row headers: `phone`, `price`, `status`, `seller_id`.
- Phone values are normalized to the 11-digit `0912xxxxxxx` form.
- Prices are non-negative Toman amounts. A zero price is retained as market evidence and excluded from valuation.
- The tenant and SHA-256 file hash provide idempotency. Importing the same file twice is rejected.
- Duplicate phones inside a snapshot are reported and only one analytical listing is retained.
- Full-snapshot imports mark listings absent from that source/date as removed. Older snapshots cannot supersede a newer full snapshot.
- Every accepted row retains its source row, batch, seller, condition and observation date. Warnings and errors remain available in import history.

## Analysis

The number analyzer detects repetitions, pairs, triples, four-or-more repeats, ABAB, AABB, ABBA, sequences, symmetry, special endings and low digit variety. It assigns an explainable score and one of `ORDINARY`, `SEMI_ROUND`, `ROUND`, `SPECIAL` or `VIP`.

Comparable samples use prefix, number class, score band and condition. IQR filtering removes valuation outliers. The result records mean, median, floor, ceiling, recommended buy/sell ranges, sample count, liquidity and confidence. Low-confidence results are explicitly labeled and the UI does not present them as conclusive prices.

Actual buy/sell figures are read from existing product, purchasing and sales records when a matching SIM SKU exists. These joins are read-only. Potential profit and opportunity ranking keep actual and calculated amounts separate.

## API and permissions

Base path: `/api/v1/sim-investment`.

- `sim-investment:view`: dashboard, list, detail and opportunities.
- `sim-investment:import`: CSV/XLSX market snapshot import.
- `sim-investment:recalculate`: rerun analysis from retained evidence.
- `sim-investment:view-history`: import messages and price history.

All database reads and writes are tenant-scoped. The frontend is bilingual, supports RTL/LTR and uses expandable mobile record cards instead of horizontally scrolling tables.

## Validation commands

```text
cd sami-backend
mvn test
mvn clean verify

cd ../sami-frontend
npm test
npm run type-check
npm run build
```

Fresh PostgreSQL validation must run Flyway V1 through V47 before any customer release. The supplied `0912_2026-08-21.csv` is a market source file and must be imported through this module, never loaded directly into canonical ERP tables.
