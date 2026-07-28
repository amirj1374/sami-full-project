-- =====================================================================
-- V17 — Tenancy write scaffold  (TEMPORARY — see removal note below)
--
-- WHY THIS EXISTS
--
-- V16 added `tenant_id NOT NULL` to 47 Tier T tables. The Java layer does
-- not map that column yet (TenantContext / @TenantId land in a later
-- step), so every INSERT omits it and fails:
--
--   ERROR: null value in column "tenant_id" of relation "refresh_tokens"
--          violates not-null constraint
--
-- This was caught by smoke-testing login immediately after V16: issuing a
-- refresh token is the first write on the auth path. It disproves the
-- claim in docs/tenancy.md §6 that step 6.1 could be "schema only, no Java
-- enforcement" — a NOT NULL column with no writer is not behaviour-
-- preserving, it is a hard outage on every Tier T write path.
--
-- WHAT THIS DOES
--
-- Gives each Tier T `tenant_id` a column DEFAULT resolving to the DEFAULT
-- tenant, so pre-tenancy code keeps working exactly as before while the
-- Java layer catches up.
--
-- THIS IS A SCAFFOLD, NOT THE DESIGN.
--
-- While it is in place the system is FAIL-OPEN: a write path that forgets
-- to bind a tenant silently lands in the DEFAULT tenant instead of
-- erroring. That directly contradicts the fail-closed principle in
-- docs/tenancy.md §5, and is acceptable ONLY because there is exactly one
-- tenant until enforcement ships.
--
-- REMOVAL IS MANDATORY and is part of the migration that introduces
-- @TenantId enforcement: drop every default, then drop the function. The
-- function is intentionally depended upon by all 47 defaults, so
-- DROP FUNCTION fails loudly until every default is gone — the scaffold
-- cannot be half-removed.
-- =====================================================================

CREATE FUNCTION tenancy_default_tenant_id() RETURNS BIGINT
    LANGUAGE sql STABLE
    AS $$ SELECT id FROM tenants WHERE code = 'DEFAULT' $$;

COMMENT ON FUNCTION tenancy_default_tenant_id() IS
    'TEMPORARY rollout scaffold (V17). Supplies tenant_id for pre-tenancy code that '
    'does not yet bind a TenantContext. Dropped by the migration that enables '
    '@TenantId enforcement. See docs/tenancy.md.';

DO $$
DECLARE
    t     TEXT;
    n     INT := 0;
BEGIN
    -- Every Tier T column, discovered rather than re-listed, so this cannot
    -- drift from V16's table list.
    FOR t IN
        SELECT table_name
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND column_name  = 'tenant_id'
          AND is_nullable  = 'NO'
        ORDER BY table_name
    LOOP
        EXECUTE format(
            'ALTER TABLE %I ALTER COLUMN tenant_id SET DEFAULT tenancy_default_tenant_id()', t);
        n := n + 1;
    END LOOP;

    RAISE NOTICE 'V17: write scaffold applied to % Tier T table(s).', n;

    IF n < 40 THEN
        RAISE EXCEPTION 'V17: expected ~47 Tier T tables, found % — V16 may not have applied', n;
    END IF;
END $$;
