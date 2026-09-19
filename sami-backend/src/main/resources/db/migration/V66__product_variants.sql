ALTER TABLE products ADD CONSTRAINT uq_products_tenant_id UNIQUE (tenant_id, id);

CREATE TABLE product_variants (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL,
    variant_code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    sku VARCHAR(64),
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_variant_product_tenant FOREIGN KEY (tenant_id, product_id)
        REFERENCES products(tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT uq_variant_code UNIQUE (tenant_id, product_id, variant_code),
    CONSTRAINT uq_variant_sku UNIQUE (tenant_id, sku),
    CONSTRAINT ck_variant_status CHECK (status IN ('ACTIVE','INACTIVE'))
);
CREATE INDEX ix_product_variants_product ON product_variants(tenant_id, product_id, status);
