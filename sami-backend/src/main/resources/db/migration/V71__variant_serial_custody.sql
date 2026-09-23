ALTER TABLE product_variants
    ADD CONSTRAINT uq_product_variants_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE inventory_serial_units
    ADD COLUMN variant_id BIGINT;

ALTER TABLE inventory_serial_units
    ADD CONSTRAINT fk_serial_variant_tenant
    FOREIGN KEY (tenant_id, variant_id) REFERENCES product_variants(tenant_id, id) ON DELETE RESTRICT;

ALTER TABLE inventory_transfer_items
    ADD COLUMN variant_id BIGINT;

ALTER TABLE inventory_transfer_items
    ADD CONSTRAINT fk_transfer_item_variant_tenant
    FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE RESTRICT;

CREATE INDEX ix_inventory_serial_variant_lookup
    ON inventory_serial_units(tenant_id, product_id, variant_id, warehouse_id, location_id, status);

CREATE INDEX ix_inventory_serial_variant_identity
    ON inventory_serial_units(tenant_id, product_id, variant_id, serial_number)
    WHERE serial_number IS NOT NULL;

CREATE INDEX ix_inventory_serial_variant_imei
    ON inventory_serial_units(tenant_id, product_id, variant_id, imei)
    WHERE imei IS NOT NULL;
