-- Purchasing completion: tenant-safe writes, idempotent imports and reporting.

ALTER TABLE purchases ADD COLUMN import_key VARCHAR(160);

CREATE UNIQUE INDEX uq_purchases_tenant_import_key
    ON purchases(tenant_id, import_key)
    WHERE import_key IS NOT NULL;

-- These write paths now bind TenantContext explicitly. Removing their V17
-- defaults makes omissions fail closed instead of leaking into DEFAULT.
ALTER TABLE purchases ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE purchase_receipts ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE purchase_returns ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE purchase_logs ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE purchase_attachments ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE purchase_unit_identifiers ALTER COLUMN tenant_id DROP DEFAULT;

CREATE INDEX ix_purchases_tenant_created
    ON purchases(tenant_id, created_at DESC);
CREATE INDEX ix_purchases_tenant_status_created
    ON purchases(tenant_id, status_id, created_at DESC);
CREATE INDEX ix_purchases_tenant_supplier_created
    ON purchases(tenant_id, supplier_id, created_at DESC);
CREATE INDEX ix_purchase_receipts_tenant_purchase
    ON purchase_receipts(tenant_id, purchase_id, created_at DESC);
CREATE INDEX ix_purchase_returns_tenant_purchase
    ON purchase_returns(tenant_id, purchase_id, created_at DESC);
CREATE INDEX ix_purchase_logs_tenant_purchase
    ON purchase_logs(tenant_id, purchase_id, occurred_at DESC);
CREATE INDEX ix_purchase_attachments_tenant_purchase
    ON purchase_attachments(tenant_id, purchase_id, created_at DESC);

INSERT INTO permissions(module_id, action, code, name, is_system)
SELECT m.id, 'report', 'purchasing:report', 'View purchasing reports', TRUE
FROM modules m
WHERE m.code = 'purchasing'
ON CONFLICT(module_id, action) DO UPDATE
SET code = EXCLUDED.code, name = EXCLUDED.name, is_system = TRUE;

UPDATE modules
SET description = 'Purchase orders, approvals, receiving, supplier returns and reports',
    progress_percentage = 100,
    is_available = TRUE,
    is_production_ready = TRUE,
    development_notes = 'Tenant-safe purchase lifecycle, inventory posting, import/export and reports'
WHERE code = 'purchasing';
