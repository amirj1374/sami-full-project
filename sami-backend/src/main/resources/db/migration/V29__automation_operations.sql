-- Operational Automation permissions. V29 is intentionally reserved after the
-- Sales V28 migration on the release integration branch.
INSERT INTO permissions (module_id, action, code, name, is_system)
SELECT m.id, p.action, 'automation:' || p.action, p.name, TRUE
FROM modules m
CROSS JOIN (VALUES
    ('import', 'Import automation configuration'),
    ('export', 'Export automation configuration'),
    ('report', 'View and export automation reports')
) AS p(action, name)
WHERE m.code = 'automation'
  AND NOT EXISTS (
      SELECT 1 FROM permissions existing
      WHERE existing.code = 'automation:' || p.action
  );
