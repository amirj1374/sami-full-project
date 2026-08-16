# Browser scenario catalog

Select the smallest set that completely covers the request. Run the baseline
for broad requests such as “test the whole UI”; add module journeys for changed
or explicitly requested areas.

## Baseline

| ID | Journey | Required proof |
|---|---|---|
| AUTH-01 | Administrator login and logout | Authenticated menu loads; logout invalidates access; protected route redirects |
| NAV-01 | Reachable-menu route smoke | Page settles without fatal console error, failed primary request, blank state, or unexpected redirect |
| USER-01 | Create an active standard user | Unique user appears in search with selected role/status |
| USER-02 | Login as the created user | Authentication succeeds; allowed menu is correct; forbidden route is denied |
| I18N-01 | Persian and English | Key content changes; Persian is RTL and English is LTR; no raw enum/i18n key appears |
| A11Y-01 | Keyboard-only critical path | Logical focus, visible indicator, activation, dialog containment, Escape, and route focus pass |
| RESP-01 | Desktop and mobile widths | No document/data-surface overflow; actions and validation remain operable |

## Core business data

| ID | Journey | Required proof |
|---|---|---|
| CRM-01 | Create and search customer | Saved record survives close/reload and is selectable from dependent forms |
| CRM-02 | Edit customer | Updated value persists and history/audit surface remains reachable when supported |
| PROD-01 | Create or select a valid product | Product is active/selectable and required identifier policy is understood |
| SUP-01 | Create or select a supplier | Supplier is active and selectable in supplier purchase |

Use existing approved prerequisite records when creation is outside the test
scope. Record their identifiers without changing them.

## Purchasing and inventory

| ID | Journey | Required proof |
|---|---|---|
| PUR-01 | Create supplier purchase | Supplier, warehouse, lines, totals, and saved lifecycle state persist |
| PUR-02 | Submit/approve purchase | Only available valid actions appear and status transition persists |
| PUR-03 | Receive goods | Quantity/serial validation works; receipt persists; inventory changes exactly once |
| CPUR-01 | Create purchase from customer | Customer seller and inspection/valuation fields persist; customer is not treated as supplier |
| CPUR-02 | Settle or explicitly waive | Settlement transition persists and audit/history evidence exists |
| CPUR-03 | Receive customer purchase | Receipt is allowed only after settlement gate; inventory and customer history update exactly once |
| PUR-NEG-01 | Duplicate/invalid receipt | UI prevents or backend rejects it without double inventory movement |

Follow the lifecycle actions exposed by the current UI. Do not assume a fixed
approval path: configuration may auto-approve purchases below a threshold.

## Sales

| ID | Journey | Required proof |
|---|---|---|
| SALE-01 | Create sale for customer/product | Totals use Toman formatting; record persists with correct customer and line |
| SALE-02 | Advance valid sale lifecycle | Status and inventory effects occur once; unavailable actions stay unavailable |
| SALE-NEG-01 | Invalid stock/input | Clear localized error appears; no partial sale or inventory mutation remains |

## Page-quality checks

For every page in scope, check initial loading, populated or empty state, API
failure presentation, retry if provided, form validation, cancel without
mutation, duplicate-submit protection, translated enum/status values, Toman
formatting and separators, Persian date display, accessible names, focus order,
mobile cards, details/action reachability, and console/network cleanliness.

## Expansion rule

When a change affects another module, add the shortest cross-module proof. For
example, customer purchase requires CRM history and inventory evidence; a new
role requires login and direct-route denial; a sale requires customer/product
selection and inventory evidence. Do not declare the originating workflow pass
until its required downstream boundary is verified.
