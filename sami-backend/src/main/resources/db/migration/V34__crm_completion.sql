-- Complete CRM tenant enforcement and query support.

-- CRM now binds the trusted request tenant on every tenant-owned insert.
-- Remove the transitional V17 fallback only from tables owned by this module.
ALTER TABLE customers ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE customer_notes ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE customer_events ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE customer_relations ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE customer_blacklist_entries ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE crm_segments ALTER COLUMN tenant_id DROP DEFAULT;

-- Tenant-first indexes for all hot CRM read paths.
CREATE INDEX IF NOT EXISTS idx_customers_tenant_created
    ON customers (tenant_id, created_at DESC) WHERE merged_into_id IS NULL;
CREATE INDEX IF NOT EXISTS idx_customers_tenant_status
    ON customers (tenant_id, status_id) WHERE merged_into_id IS NULL;
CREATE INDEX IF NOT EXISTS idx_customers_tenant_type
    ON customers (tenant_id, type_id) WHERE merged_into_id IS NULL;
CREATE INDEX IF NOT EXISTS idx_customers_tenant_source
    ON customers (tenant_id, source_id) WHERE merged_into_id IS NULL;
CREATE INDEX IF NOT EXISTS idx_customer_events_tenant_customer
    ON customer_events (tenant_id, customer_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_customer_notes_tenant_customer
    ON customer_notes (tenant_id, customer_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_customer_relations_tenant_customer
    ON customer_relations (tenant_id, customer_id);
CREATE INDEX IF NOT EXISTS idx_customer_relations_tenant_related
    ON customer_relations (tenant_id, related_customer_id);
CREATE INDEX IF NOT EXISTS idx_customer_blacklist_tenant_customer
    ON customer_blacklist_entries (tenant_id, customer_id, created_at DESC);

-- Lifecycle axes were introduced by V25. CRM is already ACTIVE on both axes;
-- record completion without introducing a second lifecycle contract.
UPDATE modules
SET description = 'Customer profiles, lifecycle, segments, configuration and reports',
    progress_percentage = 100,
    is_available = TRUE,
    is_production_ready = TRUE,
    development_notes = 'Tenant-safe CRM lifecycle, import/export, segmentation and reports'
WHERE code = 'customers';
