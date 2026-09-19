CREATE TABLE units_of_measure (
 id BIGSERIAL PRIMARY KEY, tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
 code VARCHAR(32) NOT NULL, name VARCHAR(120) NOT NULL, status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uq_uom_tenant_code UNIQUE (tenant_id, code), CONSTRAINT ck_uom_status CHECK(status IN ('ACTIVE','INACTIVE')));
CREATE TABLE uom_conversions (
 id BIGSERIAL PRIMARY KEY, tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
 from_uom_id BIGINT NOT NULL REFERENCES units_of_measure(id) ON DELETE RESTRICT,
 to_uom_id BIGINT NOT NULL REFERENCES units_of_measure(id) ON DELETE RESTRICT,
 factor NUMERIC(24,12) NOT NULL, status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT ck_conversion_factor CHECK(factor > 0), CONSTRAINT ck_conversion_uom_distinct CHECK(from_uom_id <> to_uom_id),
 CONSTRAINT ck_conversion_status CHECK(status IN ('ACTIVE','INACTIVE')), CONSTRAINT uq_conversion_direction UNIQUE(tenant_id,from_uom_id,to_uom_id));
CREATE INDEX ix_uom_conversion_lookup ON uom_conversions(tenant_id,from_uom_id,to_uom_id,status);
ALTER TABLE products ADD COLUMN base_uom_id BIGINT REFERENCES units_of_measure(id) ON DELETE RESTRICT;
ALTER TABLE product_variants ADD COLUMN base_uom_id BIGINT REFERENCES units_of_measure(id) ON DELETE RESTRICT;
