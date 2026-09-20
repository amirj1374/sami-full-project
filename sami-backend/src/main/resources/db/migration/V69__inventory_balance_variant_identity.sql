-- Separate product-only and variant-specific balance identities.
-- Existing rows are preserved; no quantities or variants are backfilled.
ALTER TABLE inventory_balances DROP CONSTRAINT uq_inventory_balance;
DROP INDEX IF EXISTS uq_inventory_balance_variant;

CREATE UNIQUE INDEX uq_inventory_balance_product_only
    ON inventory_balances(tenant_id, warehouse_id, location_id, product_id)
    WHERE variant_id IS NULL;

CREATE UNIQUE INDEX uq_inventory_balance_variant
    ON inventory_balances(tenant_id, warehouse_id, location_id, product_id, variant_id)
    WHERE variant_id IS NOT NULL;
