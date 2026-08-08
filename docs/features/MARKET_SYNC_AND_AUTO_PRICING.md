# Market Sync and Auto Pricing

Market Sync imports authorized structured SIM-market snapshots, normalizes Iranian mobile numbers, applies tenant pricing profiles, maintains one available inventory unit per active listing, and records publication intent and history.

## Supported source contract

The implemented adapter is opt-in and accepts HTTPS JSON shaped as `{ "items": [{ "number": "09123456789", "price": 1000000, "cost": 900000 }] }`. A number is normalized to ten digits beginning with `9`; an optional single leading zero is removed. Malformed rows, duplicate normalized numbers and cross-source ownership collisions are recorded without silently changing ownership.

The two Rond examples remain disabled contract placeholders. No scraping or undocumented HTML dependency is implemented. Enabling a source requires an authorized structured endpoint and the `STRUCTURED_JSON_V1` provider.

## Pricing and stock

Pricing is calculated only on the backend in this order: percentage adjustment, fixed adjustment, optional minimum profit, rounding, then publication range checks. If minimum profit is configured but acquisition cost is absent, pricing is marked `COST_UNVERIFIED` and publication fails closed.

Products use SKU `SIM-{normalizedCode}`. Inventory remains the stock owner: a remotely available unsold item is reconciled to quantity one, a missing item to zero, and a completed Sale locks it as `SOLD_LOCKED`. A later source snapshot cannot automatically reactivate a sold item; an authorized explicit reactivation is required.

## Publication

Blacklist rules take precedence. If any applicable whitelist exists, unmatched items are excluded. Source, availability, conflict, price validity and min/max checks must all pass. Publication is idempotently tracked per product/channel.

The website boundary is intentionally fail-safe: no concrete website connector exists in this repository, so publish attempts are recorded as `FAILED` with `PUBLICATION_FAILED`. Unpublishing a never-published item is safe locally. Configure and validate a real connector before production publication.

## Operations

- API root: `/api/v1/market-sync`
- Scheduler handler: `market-sync.source`
- Migration: `V42__market_sync_auto_pricing.sql`
- Permissions: `market-sync:view`, `manage-sources`, `manage-pricing`, `manage-publication`, `execute`, `view-history`, `view-errors`
- UI route: `/market-sync`

Every query and mutation is tenant-scoped. Source management, pricing, publication rules, manual execution, history/errors, conflict resolution, sold-item reactivation and Sale-driven stock changes are audited or retained as operational evidence.

## Remaining infrastructure validation

Fresh PostgreSQL/Flyway execution, live source contracts, a concrete website connector, Docker images, production Compose/nginx and live authenticated browser smoke remain required before production release.
