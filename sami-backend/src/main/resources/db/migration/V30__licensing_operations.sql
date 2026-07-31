-- Licensing reporting is independently permissioned from ordinary inventory
-- reads. The audit index supports tenant-scoped lifecycle/history queries.
INSERT INTO permissions (module_id, action, code, name, is_system)
SELECT m.id, 'report', 'licensing:report', 'View licensing reports', TRUE
FROM modules m
WHERE m.code = 'licensing'
  AND NOT EXISTS (
      SELECT 1 FROM permissions existing WHERE existing.code = 'licensing:report'
  );

CREATE INDEX idx_license_audit_tenant_entity_created
    ON license_audit_log (tenant_id, entity_type, entity_id, created_at DESC);
