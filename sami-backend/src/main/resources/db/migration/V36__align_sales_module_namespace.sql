-- Align the Sales module registry key with its route and permission namespace.
--
-- V28 intentionally reused the original `orders` module row so its stable ID
-- and foreign-key relationships survived the Sales implementation. It also
-- replaced that row's permissions with `sales:*`, leaving the backend menu to
-- check `orders:view` for a role that actually holds `sales:view`. Preserve the
-- row and every role grant; only correct the immutable registry namespace.

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM modules WHERE code = 'sales')
       AND EXISTS (SELECT 1 FROM modules WHERE code = 'orders' AND path = '/sales') THEN
        RAISE EXCEPTION 'Cannot align Sales module namespace: both sales and transformed orders rows exist';
    END IF;

    UPDATE modules
    SET code = 'sales',
        name = 'Sales',
        icon = 'mdi-point-of-sale',
        path = '/sales'
    WHERE code = 'orders'
      AND path = '/sales';

    IF NOT EXISTS (SELECT 1 FROM modules WHERE code = 'sales' AND path = '/sales') THEN
        RAISE EXCEPTION 'Cannot align Sales module namespace: transformed /sales module row was not found';
    END IF;
END $$;

-- These legacy scaffold permissions intentionally remain ungrantable business
-- actions, but their diagnostic namespace should no longer imply a live Orders
-- module after the registry rename.
UPDATE permissions
SET code = 'sales-legacy:' || action,
    name = 'Legacy sales ' || action
WHERE module_id = (SELECT id FROM modules WHERE code = 'sales')
  AND action IN ('delete', 'import')
  AND code LIKE 'orders-legacy:%';
