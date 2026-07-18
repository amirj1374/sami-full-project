-- RBAC seed data: modules, their standard permissions and baseline grants.
-- Roles were created in V3 (users depend on them); everything else lives here.

INSERT INTO modules (code, name, icon, path, display_order, enabled, is_system) VALUES
    ('dashboard',   'Dashboard',   'mdi-view-dashboard',         '/',            10,  TRUE,  TRUE),
    ('users',       'Users',       'mdi-account-group',          '/users',       20,  TRUE,  TRUE),
    ('roles',       'Roles',       'mdi-shield-account',         '/roles',       30,  TRUE,  TRUE),
    ('permissions', 'Permissions', 'mdi-key-chain',              '/permissions', 40,  TRUE,  TRUE),
    ('modules',     'Modules',     'mdi-view-module',            '/modules',     50,  TRUE,  TRUE),
    ('products',    'Products',    'mdi-package-variant-closed', '/products',    60,  TRUE,  FALSE),
    ('categories',  'Categories',  'mdi-shape',                  '/categories',  70,  FALSE, FALSE),
    ('orders',      'Orders',      'mdi-cart',                   '/orders',      80,  FALSE, FALSE),
    ('reports',     'Reports',     'mdi-chart-bar',              '/reports',     90,  FALSE, FALSE),
    ('settings',    'Settings',    'mdi-cog',                    '/settings',    100, FALSE, FALSE);

-- Dashboard is view-only; every other module gets the six standard actions.
INSERT INTO permissions (module_id, action, code, name, is_system)
SELECT m.id,
       a.action,
       m.code || ':' || a.action,
       m.name || ' — ' || a.label,
       TRUE
FROM modules m
CROSS JOIN (VALUES
    ('view',   'View'),
    ('create', 'Create'),
    ('edit',   'Edit'),
    ('delete', 'Delete'),
    ('export', 'Export'),
    ('import', 'Import')
) AS a(action, label)
WHERE m.code <> 'dashboard' OR a.action = 'view';

-- Baseline grants for the default role. The super-admin role gets no rows: its
-- is_super_admin flag bypasses permission checks entirely.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('dashboard:view', 'products:view')
WHERE r.is_default;
