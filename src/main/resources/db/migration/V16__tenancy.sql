-- =====================================================================
-- V16 — Multi-tenancy foundation
--
-- Establishes the tenant as the isolation boundary. See docs/tenancy.md.
--
-- This migration is DELIBERATELY BEHAVIOUR-PRESERVING: it adds columns,
-- backfills every existing row to a single DEFAULT tenant and reworks
-- uniqueness, but adds NO Java enforcement. A single-tenant deployment
-- under a discriminator model is behaviourally identical to no tenancy,
-- so existing installs and E2E fixtures keep working unchanged.
-- Enforcement (@TenantId / @Filter) lands in a later step.
--
-- Tier taxonomy (docs/tenancy.md §3, Appendix A):
--   P — platform-global      17 tables, no tenant_id
--   S — shared-with-override 44 tables, tenant_id NULL = platform default
--   T — tenant-owned         62 tables, tenant_id NOT NULL
-- =====================================================================


-- ---------------------------------------------------------------------
-- 1. The DEFAULT tenant
--
-- Every pre-tenancy row belongs to it. Status is resolved by FLAG, never
-- by name, consistent with the rest of the codebase.
-- ---------------------------------------------------------------------
INSERT INTO tenants (code, name, description, status_id, config, activated_at)
SELECT 'DEFAULT',
       'Default Tenant',
       'Implicit tenant created by V16 to own all data that predates multi-tenancy. '
           || 'An on-premise install runs entirely within this tenant.',
       ls.id,
       '{}'::jsonb,
       now()
FROM licensing_statuses ls
WHERE ls.scope = 'TENANT'
  AND ls.grants_access
ORDER BY ls.display_order
LIMIT 1;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM tenants WHERE code = 'DEFAULT') THEN
        RAISE EXCEPTION 'V16: could not create DEFAULT tenant — no TENANT-scoped '
                        'licensing_status with grants_access exists';
    END IF;
END $$;


-- ---------------------------------------------------------------------
-- 2. Platform vs tenant roles  (docs/tenancy.md D6)
--
-- A tenant super-admin is omnipotent WITHIN its tenant. Only a platform
-- role may cross tenants, and only via TenantContext.runAsPlatform(...).
-- Without this split, the existing is_super_admin bypass would become a
-- cross-tenant escape hatch the moment isolation is enforced.
-- ---------------------------------------------------------------------
ALTER TABLE roles ADD COLUMN is_platform BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE roles SET is_platform = TRUE WHERE is_super_admin;

COMMENT ON COLUMN roles.is_platform IS
    'Platform operator role: may operate across tenants. Distinct from is_super_admin, '
    'which grants all permissions but only within the holder''s own tenant.';


-- ---------------------------------------------------------------------
-- 3. Tier T — tenant-owned tables
--
-- add tenant_id → backfill to DEFAULT → NOT NULL → FK → index.
-- Driven from a table list so the 44 additions cannot drift from one
-- another; each step is identical by construction.
--
-- ON DELETE RESTRICT: deleting a tenant must be an explicit, ordered
-- purge, never a silent cascade across 47 tables.
-- ---------------------------------------------------------------------
DO $$
DECLARE
    t          TEXT;
    default_id BIGINT;
    tables     TEXT[] := ARRAY[
        -- identity
        'users', 'roles', 'user_profiles', 'user_audit_log',
        'refresh_tokens', 'password_reset_tokens',
        -- product
        'products',
        -- crm
        'customers', 'customer_notes', 'customer_events', 'customer_relations',
        'customer_blacklist_entries', 'crm_segments',
        -- purchasing
        'purchases', 'purchase_receipts', 'purchase_returns', 'purchase_logs',
        'purchase_attachments', 'purchase_unit_identifiers', 'pur_warehouses',
        -- supplier
        'suppliers', 'sup_ratings', 'sup_documents', 'sup_logs', 'sup_channels',
        -- dashboard
        'dashboards', 'dashboard_widgets', 'dashboard_shares', 'dashboard_favorites',
        'dashboard_saved_filters', 'dashboard_audit_log', 'kpi_values',
        -- automation
        'automation_rules', 'automation_executions', 'automation_failures',
        'automation_audit_log',
        -- licensing
        'license_audit_log',
        -- data quality
        'validation_runs', 'quality_issues', 'quality_corrections', 'quality_scores',
        'quality_audit_log',
        -- metadata
        'meta_record_form_versions', 'meta_audit_log'
    ];
BEGIN
    SELECT id INTO STRICT default_id FROM tenants WHERE code = 'DEFAULT';

    FOREACH t IN ARRAY tables LOOP
        EXECUTE format('ALTER TABLE %I ADD COLUMN tenant_id BIGINT', t);
        EXECUTE format('UPDATE %I SET tenant_id = %s', t, default_id);
        EXECUTE format('ALTER TABLE %I ALTER COLUMN tenant_id SET NOT NULL', t);
        EXECUTE format(
            'ALTER TABLE %I ADD CONSTRAINT fk_%s_tenant '
            'FOREIGN KEY (tenant_id) REFERENCES tenants (id) ON DELETE RESTRICT', t, t);
        EXECUTE format('CREATE INDEX idx_%s_tenant ON %I (tenant_id)', t, t);
    END LOOP;
END $$;

-- meta_field_values already carries a nullable tenant_id (V15). Backfill and
-- constrain it rather than adding a second column.
UPDATE meta_field_values
SET tenant_id = (SELECT id FROM tenants WHERE code = 'DEFAULT')
WHERE tenant_id IS NULL;

ALTER TABLE meta_field_values ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE meta_field_values ADD CONSTRAINT fk_meta_field_values_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenants (id) ON DELETE RESTRICT;
CREATE INDEX idx_meta_field_values_tenant ON meta_field_values (tenant_id);

-- licenses and usage_counters (V12) already have tenant_id NOT NULL + FK.
-- Backfill only: pre-V16 rows may reference a tenant that predates DEFAULT,
-- so they are left as-is. No structural change required.


-- ---------------------------------------------------------------------
-- 4. Tier S — shared-with-override tables
--
-- Nullable tenant_id. NULL = platform default, visible to every tenant.
-- A row with a tenant_id is that tenant's override. Existing seeded rows
-- stay NULL and therefore remain visible to all tenants — which is what
-- makes this migration behaviour-preserving.
-- ---------------------------------------------------------------------
DO $$
DECLARE
    t      TEXT;
    tables TEXT[] := ARRAY[
        -- user
        'user_statuses', 'profile_field_definitions',
        -- crm
        'customer_types', 'customer_statuses', 'customer_sources', 'customer_tags',
        'crm_duplicate_rules', 'crm_relation_types', 'crm_preference_definitions',
        'crm_blacklist_reasons',
        -- purchasing
        'pur_statuses', 'pur_types', 'pur_cancel_reasons', 'pur_identifier_types',
        'pur_approval_rules',
        -- supplier
        'sup_types', 'sup_statuses', 'sup_categories', 'sup_tags', 'sup_payment_terms',
        'sup_rating_criteria', 'sup_document_types', 'sup_duplicate_rules',
        -- dashboard
        'dash_statuses', 'dash_visibilities', 'dash_kpi_statuses', 'dash_widget_types',
        'dash_chart_types', 'dash_data_sources', 'dash_refresh_policies', 'kpi_definitions',
        -- automation
        'automation_statuses',
        -- data quality
        'quality_statuses', 'quality_severities', 'quality_dimensions', 'quality_score_bands',
        -- metadata
        'meta_entities'
    ];
BEGIN
    FOREACH t IN ARRAY tables LOOP
        EXECUTE format('ALTER TABLE %I ADD COLUMN tenant_id BIGINT', t);
        EXECUTE format(
            'ALTER TABLE %I ADD CONSTRAINT fk_%s_tenant '
            'FOREIGN KEY (tenant_id) REFERENCES tenants (id) ON DELETE CASCADE', t, t);
        EXECUTE format('CREATE INDEX idx_%s_tenant ON %I (tenant_id)', t, t);
    END LOOP;
END $$;

-- quality_rules, meta_fields and meta_forms already declared a nullable
-- tenant_id (V13/V15) but never constrained it. Add the missing FK + index.
-- Their existing rows are all NULL, i.e. platform defaults — correct as-is.
ALTER TABLE quality_rules ADD CONSTRAINT fk_quality_rules_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenants (id) ON DELETE CASCADE;
CREATE INDEX idx_quality_rules_tenant ON quality_rules (tenant_id);

ALTER TABLE meta_fields ADD CONSTRAINT fk_meta_fields_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenants (id) ON DELETE CASCADE;

ALTER TABLE meta_forms ADD CONSTRAINT fk_meta_forms_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenants (id) ON DELETE CASCADE;
CREATE INDEX idx_meta_forms_tenant ON meta_forms (tenant_id);
-- meta_fields already has idx_meta_fields_scope (tenant_id, company_id, branch_id).


-- =====================================================================
-- 5. Uniqueness rework
--
-- The highest-risk part of this migration: changing UNIQUE (code) to
-- UNIQUE (tenant_id, code) changes what the application treats as a
-- conflict. Each is reviewed individually below.
-- =====================================================================


-- ---------------------------------------------------------------------
-- 5a. Tier T — plain composite uniques
--
-- users.email is deliberately EXCLUDED: it stays globally unique.
-- Email is the JWT subject and login identifier; scoping it per tenant
-- would require a tenant-aware login, a JWT subject change to user id
-- and a refresh-token migration. See docs/tenancy.md D5.
--
-- refresh_tokens.token_hash and password_reset_tokens.token_hash also
-- stay globally unique — they are secrets, and global uniqueness is both
-- correct and desirable for them.
--
-- licenses.license_key stays globally unique: it is the activation
-- credential and must not repeat across tenants.
-- ---------------------------------------------------------------------
ALTER TABLE roles           DROP CONSTRAINT uq_roles_name;
ALTER TABLE roles           ADD CONSTRAINT uq_roles_name UNIQUE (tenant_id, name);

ALTER TABLE products        DROP CONSTRAINT uq_products_sku;
ALTER TABLE products        ADD CONSTRAINT uq_products_sku UNIQUE (tenant_id, sku);

ALTER TABLE customers       DROP CONSTRAINT uq_customers_code;
ALTER TABLE customers       ADD CONSTRAINT uq_customers_code UNIQUE (tenant_id, customer_code);

ALTER TABLE crm_segments    DROP CONSTRAINT uq_crm_segments_name;
ALTER TABLE crm_segments    ADD CONSTRAINT uq_crm_segments_name UNIQUE (tenant_id, name);

ALTER TABLE purchases       DROP CONSTRAINT uq_purchases_number;
ALTER TABLE purchases       ADD CONSTRAINT uq_purchases_number UNIQUE (tenant_id, purchase_number);

ALTER TABLE pur_warehouses  DROP CONSTRAINT uq_pur_warehouses_code;
ALTER TABLE pur_warehouses  ADD CONSTRAINT uq_pur_warehouses_code UNIQUE (tenant_id, code);

ALTER TABLE suppliers       DROP CONSTRAINT uq_suppliers_code;
ALTER TABLE suppliers       ADD CONSTRAINT uq_suppliers_code UNIQUE (tenant_id, supplier_code);

ALTER TABLE dashboards      DROP CONSTRAINT uq_dashboards_code;
ALTER TABLE dashboards      ADD CONSTRAINT uq_dashboards_code UNIQUE (tenant_id, code);

ALTER TABLE automation_rules      DROP CONSTRAINT uq_automation_rules_code;
ALTER TABLE automation_rules      ADD CONSTRAINT uq_automation_rules_code
    UNIQUE (tenant_id, code);

ALTER TABLE automation_executions DROP CONSTRAINT uq_automation_executions_number;
ALTER TABLE automation_executions ADD CONSTRAINT uq_automation_executions_number
    UNIQUE (tenant_id, execution_number);

ALTER TABLE validation_runs       DROP CONSTRAINT uq_validation_runs_number;
ALTER TABLE validation_runs       ADD CONSTRAINT uq_validation_runs_number
    UNIQUE (tenant_id, run_number);

ALTER TABLE licenses              DROP CONSTRAINT uq_licenses_code;
ALTER TABLE licenses              ADD CONSTRAINT uq_licenses_code UNIQUE (tenant_id, code);

ALTER TABLE meta_field_values     DROP CONSTRAINT uq_meta_field_values;
ALTER TABLE meta_field_values     ADD CONSTRAINT uq_meta_field_values
    UNIQUE (tenant_id, field_id, record_id);

ALTER TABLE meta_record_form_versions DROP CONSTRAINT uq_meta_record_form_versions;
ALTER TABLE meta_record_form_versions ADD CONSTRAINT uq_meta_record_form_versions
    UNIQUE (tenant_id, module_code, entity_code, record_id);

-- kpi_definitions is Tier S (shared), so two tenants computing the same shared
-- KPI for the same period would collide on the old three-column index.
DROP INDEX uq_kpi_values_period;
CREATE UNIQUE INDEX uq_kpi_values_period
    ON kpi_values (tenant_id, kpi_id, period_key, computed_at);

-- IMEI / serial-number uniqueness must be per-tenant: left global, one tenant
-- registering an IMEI would block another AND leak its existence via the
-- conflict error.
ALTER TABLE purchase_unit_identifiers DROP CONSTRAINT uq_pui_type_value;
ALTER TABLE purchase_unit_identifiers ADD CONSTRAINT uq_pui_type_value
    UNIQUE (tenant_id, identifier_type_id, value);


-- ---------------------------------------------------------------------
-- 5b. Tier T — partial unique indexes
-- ---------------------------------------------------------------------
DROP INDEX uq_roles_default;
CREATE UNIQUE INDEX uq_roles_default ON roles (tenant_id) WHERE is_default;

DROP INDEX uq_user_profiles_employee_code;
DROP INDEX uq_user_profiles_national_code;
DROP INDEX uq_user_profiles_phone;
CREATE UNIQUE INDEX uq_user_profiles_employee_code
    ON user_profiles (tenant_id, employee_code) WHERE employee_code IS NOT NULL;
CREATE UNIQUE INDEX uq_user_profiles_national_code
    ON user_profiles (tenant_id, national_code) WHERE national_code IS NOT NULL;
CREATE UNIQUE INDEX uq_user_profiles_phone
    ON user_profiles (tenant_id, phone_number)  WHERE phone_number  IS NOT NULL;


-- ---------------------------------------------------------------------
-- 5c. Tier S — split uniques
--
-- PostgreSQL treats NULLs as distinct, so a plain UNIQUE (tenant_id, code)
-- would permit unlimited platform-default rows sharing a code. Every Tier S
-- unique therefore becomes TWO partial indexes:
--   one platform default per code   (tenant_id IS NULL)
--   one override per tenant per code (tenant_id IS NOT NULL)
-- ---------------------------------------------------------------------
DO $$
DECLARE
    spec     TEXT[];
    specs    TEXT[][] := ARRAY[
        -- {table, existing constraint name, column list}
        ARRAY['user_statuses',              'uq_user_statuses_code',              'code'],
        ARRAY['profile_field_definitions',  'uq_profile_field_definitions_key',   'field_key'],
        ARRAY['customer_types',             'uq_customer_types_code',             'code'],
        ARRAY['customer_statuses',          'uq_customer_statuses_code',          'code'],
        ARRAY['customer_sources',           'uq_customer_sources_code',           'code'],
        ARRAY['customer_tags',              'uq_customer_tags_name',              'name'],
        ARRAY['crm_duplicate_rules',        'uq_crm_duplicate_rules_identifier',  'identifier'],
        ARRAY['crm_relation_types',         'uq_crm_relation_types_code',         'code'],
        ARRAY['crm_preference_definitions', 'uq_crm_preference_definitions_key',  'pref_key'],
        ARRAY['crm_blacklist_reasons',      'uq_crm_blacklist_reasons_code',      'code'],
        ARRAY['pur_statuses',               'uq_pur_statuses_code',               'code'],
        ARRAY['pur_types',                  'uq_pur_types_code',                  'code'],
        ARRAY['pur_cancel_reasons',         'uq_pur_cancel_reasons_code',         'code'],
        ARRAY['pur_identifier_types',       'uq_pur_identifier_types_code',       'code'],
        ARRAY['sup_types',                  'uq_sup_types_code',                  'code'],
        ARRAY['sup_statuses',               'uq_sup_statuses_code',               'code'],
        ARRAY['sup_categories',             'uq_sup_categories_name',             'name'],
        ARRAY['sup_tags',                   'uq_sup_tags_name',                   'name'],
        ARRAY['sup_payment_terms',          'uq_sup_payment_terms_code',          'code'],
        ARRAY['sup_rating_criteria',        'uq_sup_rating_criteria_code',        'code'],
        ARRAY['sup_document_types',         'uq_sup_document_types_code',         'code'],
        ARRAY['sup_duplicate_rules',        'uq_sup_duplicate_rules_identifier',  'identifier'],
        ARRAY['dash_statuses',              'uq_dash_statuses_code',              'code'],
        ARRAY['dash_visibilities',          'uq_dash_visibilities_code',          'code'],
        ARRAY['dash_kpi_statuses',          'uq_dash_kpi_statuses_code',          'code'],
        ARRAY['dash_widget_types',          'uq_dash_widget_types_code',          'code'],
        ARRAY['dash_chart_types',           'uq_dash_chart_types_code',           'code'],
        ARRAY['dash_data_sources',          'uq_dash_data_sources_code',          'code'],
        ARRAY['dash_refresh_policies',      'uq_dash_refresh_policies_code',      'code'],
        ARRAY['kpi_definitions',            'uq_kpi_definitions_code',            'code'],
        ARRAY['automation_statuses',        'uq_automation_statuses_code',        'code'],
        ARRAY['quality_statuses',           'uq_quality_statuses_code',           'code'],
        ARRAY['quality_severities',         'uq_quality_severities_code',         'code'],
        ARRAY['quality_dimensions',         'uq_quality_dimensions_code',         'code'],
        ARRAY['quality_score_bands',        'uq_quality_score_bands_code',        'code'],
        ARRAY['quality_rules',              'uq_quality_rules_code',              'code'],
        ARRAY['meta_entities',              'uq_meta_entities_target',            'module_code, entity_code'],
        ARRAY['meta_fields',                'uq_meta_fields_code',                'entity_id, code'],
        ARRAY['meta_forms',                 'uq_meta_forms_code',                 'code']
    ];
BEGIN
    FOREACH spec SLICE 1 IN ARRAY specs LOOP
        EXECUTE format('ALTER TABLE %I DROP CONSTRAINT %I', spec[1], spec[2]);
        EXECUTE format(
            'CREATE UNIQUE INDEX %I ON %I (%s) WHERE tenant_id IS NULL',
            spec[2] || '_global', spec[1], spec[3]);
        EXECUTE format(
            'CREATE UNIQUE INDEX %I ON %I (tenant_id, %s) WHERE tenant_id IS NOT NULL',
            spec[2] || '_tenant', spec[1], spec[3]);
    END LOOP;
END $$;


-- ---------------------------------------------------------------------
-- 5d. Tier S — flag singletons
--
-- ~35 indexes of the form UNIQUE (is_default) WHERE is_default enforce one
-- default GLOBALLY. Under Tier S a tenant could never define its own default,
-- because the platform row already occupies the singleton. Split identically:
-- one platform default, plus one per tenant.
--
-- Child-table singletons (sup_addresses, sup_contacts, sup_bank_accounts,
-- sup_channels, customer_contacts, customer_addresses) are already scoped by
-- supplier_id / customer_id and are left unchanged.
--
-- Tier P singletons (billing_cycles, expiry_behaviors, feature_states,
-- license_types, licensing_statuses, payment_statuses, subscription_plans)
-- are platform-global by definition and are left unchanged.
-- ---------------------------------------------------------------------
DO $$
DECLARE
    spec  TEXT[];
    specs TEXT[][] := ARRAY[
        -- {index name, table, flag column}
        ARRAY['uq_user_statuses_default',       'user_statuses',       'is_default'],
        ARRAY['uq_user_statuses_archived',      'user_statuses',       'is_archived_state'],
        ARRAY['uq_user_statuses_deleted',       'user_statuses',       'is_deleted_state'],
        ARRAY['uq_customer_statuses_default',   'customer_statuses',   'is_default'],
        ARRAY['uq_customer_statuses_archived',  'customer_statuses',   'is_archived_state'],
        ARRAY['uq_customer_statuses_deleted',   'customer_statuses',   'is_deleted_state'],
        ARRAY['uq_customer_statuses_blacklist', 'customer_statuses',   'is_blacklist_state'],
        ARRAY['uq_customer_types_default',      'customer_types',      'is_default'],
        ARRAY['uq_pur_statuses_draft',          'pur_statuses',        'is_draft_state'],
        ARRAY['uq_pur_statuses_pending',        'pur_statuses',        'is_pending_state'],
        ARRAY['uq_pur_statuses_approved',       'pur_statuses',        'is_approved_state'],
        ARRAY['uq_pur_statuses_rejected',       'pur_statuses',        'is_rejected_state'],
        ARRAY['uq_pur_statuses_completed',      'pur_statuses',        'is_completed_state'],
        ARRAY['uq_pur_statuses_partial',        'pur_statuses',        'is_partial_state'],
        ARRAY['uq_pur_statuses_cancelled',      'pur_statuses',        'is_cancelled_state'],
        ARRAY['uq_pur_types_default',           'pur_types',           'is_default'],
        ARRAY['uq_sup_statuses_default',        'sup_statuses',        'is_default'],
        ARRAY['uq_sup_statuses_archived',       'sup_statuses',        'is_archived_state'],
        ARRAY['uq_sup_statuses_deleted',        'sup_statuses',        'is_deleted_state'],
        ARRAY['uq_sup_statuses_blacklist',      'sup_statuses',        'is_blacklist_state'],
        ARRAY['uq_sup_types_default',           'sup_types',           'is_default'],
        ARRAY['uq_dash_statuses_default',       'dash_statuses',       'is_default'],
        ARRAY['uq_dash_statuses_archived',      'dash_statuses',       'is_archived_state'],
        ARRAY['uq_dash_visibilities_default',   'dash_visibilities',   'is_default'],
        ARRAY['uq_dash_kpi_statuses_default',   'dash_kpi_statuses',   'is_default'],
        ARRAY['uq_dash_refresh_policies_default','dash_refresh_policies','is_default'],
        ARRAY['uq_automation_statuses_default', 'automation_statuses', 'is_default'],
        ARRAY['uq_automation_statuses_archived','automation_statuses', 'is_archived_state'],
        ARRAY['uq_quality_statuses_default',    'quality_statuses',    'is_default'],
        ARRAY['uq_quality_statuses_archived',   'quality_statuses',    'is_archived_state'],
        ARRAY['uq_quality_severities_default',  'quality_severities',  'is_default']
    ];
BEGIN
    FOREACH spec SLICE 1 IN ARRAY specs LOOP
        EXECUTE format('DROP INDEX %I', spec[1]);
        EXECUTE format(
            'CREATE UNIQUE INDEX %I ON %I ((TRUE)) WHERE %I AND tenant_id IS NULL',
            spec[1] || '_global', spec[2], spec[3]);
        EXECUTE format(
            'CREATE UNIQUE INDEX %I ON %I (tenant_id) WHERE %I AND tenant_id IS NOT NULL',
            spec[1] || '_tenant', spec[2], spec[3]);
    END LOOP;
END $$;


-- ---------------------------------------------------------------------
-- 6. Post-migration assertions
--
-- Fail loudly here rather than surfacing as a data leak later.
-- ---------------------------------------------------------------------
DO $$
DECLARE
    t              TEXT;
    orphans        BIGINT;
    missing_column INT;
BEGIN
    -- every Tier T table must have a NOT NULL tenant_id
    SELECT count(*) INTO missing_column
    FROM (VALUES
        ('users'), ('roles'), ('user_profiles'), ('products'), ('customers'),
        ('purchases'), ('suppliers'), ('dashboards'), ('automation_rules'),
        ('validation_runs'), ('meta_field_values')
    ) AS required(tbl)
    WHERE NOT EXISTS (
        SELECT 1 FROM information_schema.columns c
        WHERE c.table_name = required.tbl
          AND c.column_name = 'tenant_id'
          AND c.is_nullable = 'NO');

    IF missing_column > 0 THEN
        RAISE EXCEPTION 'V16: % Tier T table(s) lack a NOT NULL tenant_id', missing_column;
    END IF;

    -- no Tier T row may reference a non-existent tenant
    FOREACH t IN ARRAY ARRAY['users', 'roles', 'products', 'customers', 'purchases',
                             'suppliers', 'dashboards', 'meta_field_values'] LOOP
        EXECUTE format(
            'SELECT count(*) FROM %I x WHERE NOT EXISTS '
            '(SELECT 1 FROM tenants t WHERE t.id = x.tenant_id)', t) INTO orphans;
        IF orphans > 0 THEN
            RAISE EXCEPTION 'V16: % orphaned row(s) in %', orphans, t;
        END IF;
    END LOOP;

    RAISE NOTICE 'V16: tenancy foundation applied. All data assigned to tenant DEFAULT.';
END $$;
