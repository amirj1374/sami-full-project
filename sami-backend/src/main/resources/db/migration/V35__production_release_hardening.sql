-- Production release hardening: make module lifecycle data truthful and finish
-- trusted tenant enforcement for the customer-visible scheduler.

-- Scheduler request writes now always bind the authenticated tenant and
-- background executions copy the persisted job tenant. Remove the transitional
-- fallback so a missing scope fails at the application boundary instead of
-- silently writing into the platform default tenant.
ALTER TABLE scheduled_jobs ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE job_executions ALTER COLUMN tenant_id DROP DEFAULT;

CREATE INDEX IF NOT EXISTS idx_scheduled_jobs_tenant_code
    ON scheduled_jobs (tenant_id, code);
CREATE INDEX IF NOT EXISTS idx_job_executions_tenant_started
    ON job_executions (tenant_id, started_at DESC);

-- These modules have executable backend and frontend workflows in this release.
-- V25 captured an older snapshot before their UI and operational slices landed.
UPDATE modules
SET backend_status_id = (SELECT id FROM module_statuses WHERE code = 'ACTIVE' AND tenant_id IS NULL),
    frontend_status_id = (SELECT id FROM module_statuses WHERE code = 'ACTIVE' AND tenant_id IS NULL),
    overall_status_id = NULL,
    progress_percentage = 100,
    is_available = TRUE,
    is_production_ready = TRUE,
    enabled = TRUE,
    development_notes = 'Validated backend and responsive frontend included in the final release'
WHERE code IN ('automation', 'licensing', 'scheduler');

-- Keep unfinished capabilities in the repository for future approved work, but
-- do not expose their partial or placeholder workspaces in the production menu.
-- This is reversible lifecycle data, not deletion of application or customer data.
UPDATE modules
SET enabled = FALSE,
    is_available = FALSE,
    is_production_ready = FALSE,
    development_notes = 'Excluded from the production navigation until its end-to-end workflow is complete'
WHERE code IN ('organization', 'data-quality', 'files', 'metadata', 'knowledge',
               'calendar', 'portal', 'appointments', 'communication');
