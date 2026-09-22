ALTER TABLE inventory_reservations
    ADD COLUMN variant_id BIGINT,
    ADD COLUMN requested_quantity NUMERIC(16,3),
    ADD COLUMN reserved_quantity NUMERIC(16,3),
    ADD COLUMN backordered_quantity NUMERIC(16,3) NOT NULL DEFAULT 0,
    ADD COLUMN entered_quantity NUMERIC(18,6),
    ADD COLUMN entered_uom_id BIGINT,
    ADD COLUMN conversion_factor NUMERIC(24,12),
    ADD COLUMN base_quantity NUMERIC(18,6),
    ADD COLUMN base_uom_id BIGINT,
    ADD COLUMN backorder_status VARCHAR(24) NOT NULL DEFAULT 'FULFILLED';
UPDATE inventory_reservations SET requested_quantity=quantity, reserved_quantity=quantity, base_quantity=quantity,
    backorder_status=CASE WHEN backordered_quantity > 0 THEN 'OPEN' ELSE 'FULFILLED' END
    WHERE requested_quantity IS NULL;
ALTER TABLE inventory_reservations ALTER COLUMN requested_quantity SET NOT NULL;
ALTER TABLE inventory_reservations ALTER COLUMN reserved_quantity SET NOT NULL;
ALTER TABLE inventory_reservations ALTER COLUMN base_quantity SET NOT NULL;
ALTER TABLE inventory_reservations ADD CONSTRAINT fk_reservation_variant_tenant
    FOREIGN KEY (tenant_id, variant_id) REFERENCES product_variants(tenant_id, id) ON DELETE RESTRICT;
ALTER TABLE inventory_reservations ADD CONSTRAINT fk_reservation_entered_uom
    FOREIGN KEY (entered_uom_id) REFERENCES units_of_measure(id) ON DELETE RESTRICT;
ALTER TABLE inventory_reservations ADD CONSTRAINT fk_reservation_base_uom
    FOREIGN KEY (base_uom_id) REFERENCES units_of_measure(id) ON DELETE RESTRICT;
ALTER TABLE inventory_reservations ADD CONSTRAINT ck_reservation_backorder_status
    CHECK (backorder_status IN ('OPEN','PARTIALLY_FULFILLED','FULFILLED','CANCELLED'));
ALTER TABLE inventory_reservations ADD CONSTRAINT ck_reservation_backorder_quantity
    CHECK (backordered_quantity >= 0 AND backordered_quantity <= requested_quantity);
ALTER TABLE inventory_reservations ADD CONSTRAINT ck_reservation_allocation
    CHECK (reserved_quantity >= 0 AND reserved_quantity + backordered_quantity = requested_quantity);
DROP INDEX uq_inventory_reservation_active;
CREATE UNIQUE INDEX uq_inventory_reservation_active_product ON inventory_reservations
    (tenant_id, source_type, source_id, source_line_id, product_id)
    WHERE status='ACTIVE' AND variant_id IS NULL;
CREATE UNIQUE INDEX uq_inventory_reservation_active_variant ON inventory_reservations
    (tenant_id, source_type, source_id, source_line_id, product_id, variant_id)
    WHERE status='ACTIVE' AND variant_id IS NOT NULL;
CREATE INDEX ix_inventory_reservation_identity ON inventory_reservations
    (tenant_id, warehouse_id, location_id, product_id, variant_id, status);
