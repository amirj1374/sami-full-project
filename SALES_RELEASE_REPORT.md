# Sales Release Report

## Release identity

- Release branch: `codex/final-sami-release`
- Sales feature commit: `b0ddb217a0f77c211fedc2d78070c2a9f491f2d2`
- Sales merge commit: `4e291fe134fcbbe2c9ef9258bb671fee1efd0430`
- Validated release branch SHA: `4e291fe134fcbbe2c9ef9258bb671fee1efd0430`

## Delivered scope

- Tenant-scoped draft, confirmation, completion, cancellation and partial/full return lifecycle with trusted company/branch validation.
- Item, service, mixed-payment, discount approval, low-margin approval, stock movement, accounting entry, commission, audit and domain-event flows.
- Atomic stock issue/restore checks, overpayment prevention, persisted approved discounts and tenant-safe return numbering.
- Filtered Sales listings and management reports covering revenue, gross profit, average ticket, discount/return rates, daily sales, payment methods, products and sellers.
- Excel-compatible UTF-8 BOM CSV export, printable/PDF-ready report and invoice views, and accounting history visibility.
- Persisted lost-sales capture and analysis with customer, product, reason and expected-value context.
- Responsive Vue/Vuetify Sales workspace with invoices, reports and lost-sales views, desktop tables, mobile cards/dialogs, permission-aware actions and English/Persian RTL localization.
- Additive Flyway V31 schema, reporting indexes, Sales permissions and production lifecycle metadata; all earlier migrations remain unchanged.

## Validation executed

### Backend

- Maven compile: passed.
- Complete `mvn test` suite: passed on the feature tree and again on the merged release tree.
- Result: 179 tests, 0 failures, 0 errors, 0 skipped.
- Focused Sales coverage verifies trusted tenant persistence and tenant-correct domain events; existing lifecycle and CSV encoding tests also pass.
- Fresh PostgreSQL 16 validation: Flyway V1-V31 applied successfully; `V31 sales completion` is recorded successful and both new tables exist.
- Hibernate schema validation and Spring application startup completed against the isolated database.
- Live production-nginx API smoke checks passed for authentication, Sales listing, reports and lost-sales listing.

### Frontend

- `npm run type-check`: passed on the feature and merged release trees.
- `npm run build`: passed on the feature and merged release trees; 983 modules transformed.
- The repository has no configured frontend unit-test script, so no unavailable test command was represented as passing.
- Localization parity: 0 English-only keys and 0 Persian-only keys.
- Diff, JSON parsing and unfinished/debug-marker checks passed.

### Browser and responsive verification

- The production frontend image was exercised through nginx against the production backend image and a fresh PostgreSQL database.
- Authenticated desktop viewport `1440x900`: Sales, Reports and Lost Sales views loaded using real APIs; empty states and permission-controlled actions rendered correctly.
- Mobile viewport `390x844`: no horizontal page overflow (`scrollWidth <= innerWidth`), mobile cards/navigation rendered cleanly and Sales actions remained reachable.
- Persian mobile verification: `lang=fa`, `dir=rtl`, localized Sales navigation, metrics, tabs and empty states; no horizontal page overflow.
- Desktop and mobile screenshots were visually inspected for clipping, overlap, broken text and layout defects.

### Docker

- Backend image: `sami-backend:sales-release`, `linux/amd64`, image ID `sha256:7dd8eedf154b1ca06b0b90ab164da1f4b5e3cf053dc61cf721ed23c985e4aca0`.
- Frontend image: `sami-frontend:sales-release`, `linux/amd64`, image ID `sha256:28e752eaf05fb67be274b0c4b5ccb168cea4cf0fa78b4220a4dfdff59e9e00e2`.
- Both repository production Dockerfiles completed successfully without modification.

## Known limitations

- Inventory and accounting are represented by the existing canonical Sales stock-movement and accounting-entry contracts because separate production Inventory and General Ledger module APIs do not yet exist in this repository.
- Installment, external payment-provider, Notification Center and AI model integrations remain behind the existing payment status, domain-event and recommendation-provider boundaries. No fake external response or speculative integration was added.
- Browser validation used an isolated bootstrap administrator and fresh QA database; external payment, credit-check, accounting-provider and AI-provider systems were not available in this environment.

## Release conclusion

Sales is implemented, customer-visible, merged and validated on `codex/final-sami-release`. No subsequent module was started.
