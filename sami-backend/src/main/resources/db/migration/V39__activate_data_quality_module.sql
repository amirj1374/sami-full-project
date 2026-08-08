-- Data Quality now has a shipped, permission-gated Vue workflow in addition
-- to its existing tenant-aware schema, services and REST API.
UPDATE modules SET
    backend_status_id = (SELECT id FROM module_statuses WHERE code = 'ACTIVE' AND tenant_id IS NULL),
    frontend_status_id = (SELECT id FROM module_statuses WHERE code = 'ACTIVE' AND tenant_id IS NULL),
    progress_percentage = 100,
    is_production_ready = TRUE,
    enabled = TRUE
WHERE code = 'data-quality';
