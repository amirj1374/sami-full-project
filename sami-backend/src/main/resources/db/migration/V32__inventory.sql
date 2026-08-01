-- Canonical tenant-scoped Inventory and Warehouse bounded context.
--
-- The purchasing warehouse lookup is promoted in place so existing purchase
-- references keep their identifiers. Product.stock_quantity remains a
-- compatibility projection; inventory_balances and inventory_movements are
-- authoritative after this migration.

ALTER TABLE pur_warehouses
    ADD COLUMN company_id BIGINT,
    ADD COLUMN branch_id BIGINT,
    ADD COLUMN description VARCHAR(500),
    ADD COLUMN warehouse_type VARCHAR(32) NOT NULL DEFAULT 'STANDARD',
    ADD COLUMN allows_negative_stock BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN is_default BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE pur_warehouses
    ADD CONSTRAINT fk_inventory_warehouse_company
        FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_inventory_warehouse_branch
        FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE RESTRICT,
    ADD CONSTRAINT ck_inventory_warehouse_type
        CHECK (warehouse_type IN ('STANDARD','RETAIL','TRANSIT','QUARANTINE','RETURNS'));

UPDATE pur_warehouses w
SET company_id = b.company_id,
    branch_id = b.id
FROM branches b
WHERE b.tenant_id = w.tenant_id
  AND b.is_default
  AND w.branch_id IS NULL;

WITH ranked AS (
    SELECT id, row_number() OVER (PARTITION BY tenant_id ORDER BY display_order, id) AS rn
    FROM pur_warehouses
)
UPDATE pur_warehouses w SET is_default = TRUE
FROM ranked r WHERE r.id = w.id AND r.rn = 1;

CREATE UNIQUE INDEX uq_inventory_warehouse_default
    ON pur_warehouses(tenant_id) WHERE is_default;
CREATE INDEX ix_inventory_warehouse_scope
    ON pur_warehouses(tenant_id, company_id, branch_id, active);

CREATE TABLE inventory_locations (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    warehouse_id BIGINT NOT NULL REFERENCES pur_warehouses(id) ON DELETE RESTRICT,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    location_type VARCHAR(32) NOT NULL DEFAULT 'STORAGE',
    description VARCHAR(500),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_inventory_location_type CHECK
        (location_type IN ('RECEIVING','STORAGE','PICKING','TRANSIT','QUARANTINE','RETURNS')),
    CONSTRAINT uq_inventory_location_code UNIQUE (tenant_id, warehouse_id, code)
);
CREATE UNIQUE INDEX uq_inventory_location_default
    ON inventory_locations(tenant_id, warehouse_id) WHERE is_default;
CREATE INDEX ix_inventory_locations_scope
    ON inventory_locations(tenant_id, warehouse_id, active);

INSERT INTO inventory_locations(tenant_id, warehouse_id, code, name, is_default)
SELECT tenant_id, id, 'DEFAULT', 'Default Location', TRUE
FROM pur_warehouses;

CREATE TABLE inventory_balances (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    warehouse_id BIGINT NOT NULL REFERENCES pur_warehouses(id) ON DELETE RESTRICT,
    location_id BIGINT NOT NULL REFERENCES inventory_locations(id) ON DELETE RESTRICT,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    on_hand NUMERIC(16,3) NOT NULL DEFAULT 0,
    reserved NUMERIC(16,3) NOT NULL DEFAULT 0,
    average_unit_cost NUMERIC(18,4) NOT NULL DEFAULT 0,
    reorder_point NUMERIC(16,3) NOT NULL DEFAULT 0,
    last_movement_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_inventory_balance UNIQUE (tenant_id, warehouse_id, location_id, product_id),
    CONSTRAINT ck_inventory_balance_on_hand CHECK (on_hand >= 0),
    CONSTRAINT ck_inventory_balance_reserved CHECK (reserved >= 0 AND reserved <= on_hand),
    CONSTRAINT ck_inventory_balance_cost CHECK (average_unit_cost >= 0),
    CONSTRAINT ck_inventory_balance_reorder CHECK (reorder_point >= 0)
);
CREATE INDEX ix_inventory_balance_product
    ON inventory_balances(tenant_id, product_id, warehouse_id);
CREATE INDEX ix_inventory_balance_low_stock
    ON inventory_balances(tenant_id, warehouse_id, product_id)
    WHERE on_hand - reserved <= reorder_point;

INSERT INTO inventory_balances(
    tenant_id, warehouse_id, location_id, product_id, on_hand, last_movement_at)
SELECT p.tenant_id, w.id, l.id, p.id, p.stock_quantity, now()
FROM products p
JOIN pur_warehouses w ON w.tenant_id = p.tenant_id AND w.is_default
JOIN inventory_locations l ON l.tenant_id = p.tenant_id
    AND l.warehouse_id = w.id AND l.is_default
WHERE p.stock_quantity > 0;

CREATE TABLE inventory_serial_units (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    warehouse_id BIGINT NOT NULL REFERENCES pur_warehouses(id) ON DELETE RESTRICT,
    location_id BIGINT NOT NULL REFERENCES inventory_locations(id) ON DELETE RESTRICT,
    serial_number VARCHAR(128),
    imei VARCHAR(32),
    status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
    source_type VARCHAR(32),
    source_id BIGINT,
    source_line_id BIGINT,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    issued_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_inventory_serial_identity CHECK (serial_number IS NOT NULL OR imei IS NOT NULL),
    CONSTRAINT ck_inventory_serial_status CHECK
        (status IN ('AVAILABLE','RESERVED','IN_TRANSIT','ISSUED','QUARANTINED','RETURNED_TO_SUPPLIER'))
);
CREATE UNIQUE INDEX uq_inventory_serial_number
    ON inventory_serial_units(tenant_id, serial_number) WHERE serial_number IS NOT NULL;
CREATE UNIQUE INDEX uq_inventory_serial_unit_imei
    ON inventory_serial_units(tenant_id, imei) WHERE imei IS NOT NULL;
CREATE INDEX ix_inventory_serial_lookup
    ON inventory_serial_units(tenant_id, product_id, warehouse_id, status);

CREATE TABLE inventory_movements (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    from_warehouse_id BIGINT REFERENCES pur_warehouses(id) ON DELETE RESTRICT,
    from_location_id BIGINT REFERENCES inventory_locations(id) ON DELETE RESTRICT,
    to_warehouse_id BIGINT REFERENCES pur_warehouses(id) ON DELETE RESTRICT,
    to_location_id BIGINT REFERENCES inventory_locations(id) ON DELETE RESTRICT,
    movement_type VARCHAR(32) NOT NULL,
    quantity NUMERIC(16,3) NOT NULL CHECK (quantity > 0),
    unit_cost NUMERIC(18,4) NOT NULL DEFAULT 0 CHECK (unit_cost >= 0),
    source_type VARCHAR(32) NOT NULL,
    source_id BIGINT,
    source_line_id BIGINT,
    operation_key VARCHAR(160),
    reason VARCHAR(500),
    actor_id BIGINT,
    actor_email VARCHAR(255),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_inventory_movement_type CHECK (movement_type IN (
        'OPENING','RECEIPT','SUPPLIER_RETURN','ADJUSTMENT_IN','ADJUSTMENT_OUT',
        'TRANSFER_OUT','TRANSFER_IN','RESERVE','RELEASE','ISSUE','CUSTOMER_RETURN',
        'COUNT_GAIN','COUNT_LOSS'))
);
CREATE UNIQUE INDEX uq_inventory_movement_operation
    ON inventory_movements(tenant_id, operation_key) WHERE operation_key IS NOT NULL;
CREATE INDEX ix_inventory_movement_timeline
    ON inventory_movements(tenant_id, occurred_at DESC, product_id);
CREATE INDEX ix_inventory_movement_source
    ON inventory_movements(tenant_id, source_type, source_id);

INSERT INTO inventory_movements(
    tenant_id, product_id, to_warehouse_id, to_location_id, movement_type,
    quantity, source_type, operation_key, reason, occurred_at)
SELECT p.tenant_id, p.id, w.id, l.id, 'OPENING', p.stock_quantity,
       'MIGRATION', 'inventory-v32-opening-' || p.id, 'Opening balance migrated from product stock', now()
FROM products p
JOIN pur_warehouses w ON w.tenant_id = p.tenant_id AND w.is_default
JOIN inventory_locations l ON l.tenant_id = p.tenant_id
    AND l.warehouse_id = w.id AND l.is_default
WHERE p.stock_quantity > 0;

CREATE TABLE inventory_reservations (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    warehouse_id BIGINT NOT NULL REFERENCES pur_warehouses(id) ON DELETE RESTRICT,
    location_id BIGINT NOT NULL REFERENCES inventory_locations(id) ON DELETE RESTRICT,
    source_type VARCHAR(32) NOT NULL,
    source_id BIGINT NOT NULL,
    source_line_id BIGINT,
    quantity NUMERIC(16,3) NOT NULL CHECK (quantity > 0),
    fulfilled_quantity NUMERIC(16,3) NOT NULL DEFAULT 0,
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    serial_unit_id BIGINT REFERENCES inventory_serial_units(id) ON DELETE RESTRICT,
    expires_at TIMESTAMPTZ,
    created_by BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_inventory_reservation_status CHECK
        (status IN ('ACTIVE','RELEASED','FULFILLED','EXPIRED')),
    CONSTRAINT ck_inventory_reservation_fulfilled CHECK
        (fulfilled_quantity >= 0 AND fulfilled_quantity <= quantity)
);
CREATE UNIQUE INDEX uq_inventory_reservation_active
    ON inventory_reservations(tenant_id, source_type, source_id, source_line_id, product_id)
    WHERE status = 'ACTIVE';
CREATE INDEX ix_inventory_reservation_source
    ON inventory_reservations(tenant_id, source_type, source_id, status);

CREATE TABLE inventory_document_numbers (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    document_type VARCHAR(24) NOT NULL,
    sequence_year INTEGER NOT NULL,
    next_value BIGINT NOT NULL DEFAULT 1 CHECK (next_value > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_inventory_document_number UNIQUE (tenant_id, document_type, sequence_year)
);

CREATE TABLE inventory_transfers (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    transfer_number VARCHAR(40) NOT NULL,
    from_warehouse_id BIGINT NOT NULL REFERENCES pur_warehouses(id) ON DELETE RESTRICT,
    to_warehouse_id BIGINT NOT NULL REFERENCES pur_warehouses(id) ON DELETE RESTRICT,
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    notes VARCHAR(1000),
    created_by BIGINT,
    shipped_by BIGINT,
    received_by BIGINT,
    shipped_at TIMESTAMPTZ,
    received_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_inventory_transfer_number UNIQUE (tenant_id, transfer_number),
    CONSTRAINT ck_inventory_transfer_status CHECK
        (status IN ('DRAFT','SHIPPED','RECEIVED','CANCELLED')),
    CONSTRAINT ck_inventory_transfer_distinct CHECK (from_warehouse_id <> to_warehouse_id)
);
CREATE INDEX ix_inventory_transfer_status
    ON inventory_transfers(tenant_id, status, created_at DESC);

CREATE TABLE inventory_transfer_items (
    id BIGSERIAL PRIMARY KEY,
    transfer_id BIGINT NOT NULL REFERENCES inventory_transfers(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    quantity NUMERIC(16,3) NOT NULL CHECK (quantity > 0),
    serial_numbers JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_inventory_transfer_product UNIQUE (transfer_id, product_id)
);

CREATE TABLE inventory_counts (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    count_number VARCHAR(40) NOT NULL,
    warehouse_id BIGINT NOT NULL REFERENCES pur_warehouses(id) ON DELETE RESTRICT,
    location_id BIGINT REFERENCES inventory_locations(id) ON DELETE RESTRICT,
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    notes VARCHAR(1000),
    created_by BIGINT,
    posted_by BIGINT,
    posted_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_inventory_count_number UNIQUE (tenant_id, count_number),
    CONSTRAINT ck_inventory_count_status CHECK (status IN ('DRAFT','COUNTED','POSTED','CANCELLED'))
);
CREATE INDEX ix_inventory_count_status
    ON inventory_counts(tenant_id, status, created_at DESC);

CREATE TABLE inventory_count_items (
    id BIGSERIAL PRIMARY KEY,
    count_id BIGINT NOT NULL REFERENCES inventory_counts(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    expected_quantity NUMERIC(16,3) NOT NULL DEFAULT 0,
    counted_quantity NUMERIC(16,3),
    variance NUMERIC(16,3),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_inventory_count_product UNIQUE (count_id, product_id),
    CONSTRAINT ck_inventory_count_expected CHECK (expected_quantity >= 0),
    CONSTRAINT ck_inventory_count_counted CHECK (counted_quantity IS NULL OR counted_quantity >= 0)
);

CREATE TABLE inventory_audit_log (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    entity_type VARCHAR(32) NOT NULL,
    entity_id BIGINT,
    action VARCHAR(64) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    actor_id BIGINT,
    actor_email VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_inventory_audit_timeline
    ON inventory_audit_log(tenant_id, entity_type, entity_id, created_at DESC);

-- Preserve historical Sales movement evidence in the canonical timeline.
INSERT INTO inventory_movements(
    tenant_id, product_id, from_warehouse_id, from_location_id,
    movement_type, quantity, source_type, source_id, source_line_id,
    operation_key, actor_id, occurred_at)
SELECT sm.tenant_id, si.product_id, w.id, l.id,
       CASE sm.movement_type
           WHEN 'RESERVE' THEN 'RESERVE'
           WHEN 'RELEASE' THEN 'RELEASE'
           WHEN 'ISSUE' THEN 'ISSUE'
           WHEN 'RETURN' THEN 'CUSTOMER_RETURN'
       END,
       sm.quantity, 'SALE', sm.sale_id, sm.sale_item_id,
       'legacy-sales-movement-' || sm.id, sm.actor_id, sm.occurred_at
FROM sales_stock_movements sm
JOIN sale_items si ON si.id = sm.sale_item_id
JOIN pur_warehouses w ON w.tenant_id = sm.tenant_id AND w.is_default
JOIN inventory_locations l ON l.tenant_id = sm.tenant_id
    AND l.warehouse_id = w.id AND l.is_default
WHERE sm.movement_type IN ('RESERVE','RELEASE','ISSUE','RETURN');

-- Inventory becomes a real menu/RBAC owner rather than a dashboard placeholder.
INSERT INTO modules(
    code, name, description, icon, path, display_order, enabled, is_system,
    backend_status_id, frontend_status_id, overall_status_id, release_version,
    progress_percentage, development_notes, is_available, is_production_ready)
SELECT 'inventory', 'Inventory', 'Warehouse, stock, serial and movement management',
       'mdi-warehouse', '/inventory', 67, TRUE, FALSE,
       s.id, s.id, s.id, '1.0', 100,
       'Canonical warehouse, ledger, reservation, transfer and count workflows', TRUE, TRUE
FROM module_statuses s
WHERE s.code = 'ACTIVE' AND s.tenant_id IS NULL
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    icon = EXCLUDED.icon,
    path = EXCLUDED.path,
    display_order = EXCLUDED.display_order,
    enabled = TRUE,
    backend_status_id = EXCLUDED.backend_status_id,
    frontend_status_id = EXCLUDED.frontend_status_id,
    overall_status_id = EXCLUDED.overall_status_id,
    progress_percentage = 100,
    is_available = TRUE,
    is_production_ready = TRUE;

INSERT INTO permissions(module_id, action, code, name, is_system)
SELECT m.id, p.action, 'inventory:' || p.action, p.name, TRUE
FROM modules m
CROSS JOIN (VALUES
    ('view', 'View inventory'),
    ('manage-warehouses', 'Manage warehouses and locations'),
    ('adjust', 'Post stock adjustments'),
    ('transfer', 'Manage stock transfers'),
    ('count', 'Manage stock counts'),
    ('reserve', 'Manage reservations'),
    ('issue', 'Issue and return stock'),
    ('view-audit', 'View inventory audit'),
    ('report', 'View inventory reports'),
    ('import', 'Import inventory adjustments'),
    ('export', 'Export inventory data')
) p(action, name)
WHERE m.code = 'inventory'
ON CONFLICT(module_id, action) DO UPDATE
SET code = EXCLUDED.code, name = EXCLUDED.name, is_system = TRUE;
