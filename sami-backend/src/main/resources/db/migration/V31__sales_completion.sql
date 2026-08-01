-- Complete the Sales operational surface without changing the V28 aggregate.

CREATE TABLE sale_return_numbers (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    next_value BIGINT NOT NULL DEFAULT 1 CHECK (next_value > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_sale_return_numbers_tenant UNIQUE (tenant_id)
);

CREATE TABLE lost_sales (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE RESTRICT,
    branch_id BIGINT NOT NULL REFERENCES branches(id) ON DELETE RESTRICT,
    customer_id BIGINT REFERENCES customers(id) ON DELETE RESTRICT,
    product_id BIGINT REFERENCES products(id) ON DELETE RESTRICT,
    seller_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    reason_code VARCHAR(40) NOT NULL,
    notes VARCHAR(1000),
    expected_amount NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK (expected_amount >= 0),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX ix_lost_sales_scope_date ON lost_sales(tenant_id, company_id, branch_id, occurred_at DESC);
CREATE INDEX ix_lost_sales_reason ON lost_sales(tenant_id, reason_code, occurred_at DESC);

CREATE INDEX ix_sales_reporting ON sales(tenant_id, created_at DESC, branch_id, seller_id, sale_type);
CREATE INDEX ix_sale_payments_reporting ON sale_payments(tenant_id, paid_at DESC, method, status);
CREATE INDEX ix_sale_returns_reporting ON sale_returns(tenant_id, created_at DESC);

INSERT INTO permissions(module_id, action, code, name, is_system)
SELECT m.id, p.action, 'sales:' || p.action, p.name, TRUE
FROM modules m
CROSS JOIN (VALUES
    ('manage-lost-sales', 'Manage lost sales'),
    ('view-accounting', 'View sale accounting entries')
) p(action, name)
WHERE m.code = 'orders'
ON CONFLICT(module_id, action) DO UPDATE
SET code = EXCLUDED.code, name = EXCLUDED.name, is_system = TRUE;

UPDATE modules
SET backend_status_id=(SELECT id FROM module_statuses WHERE code='ACTIVE' AND tenant_id IS NULL),
    frontend_status_id=(SELECT id FROM module_statuses WHERE code='ACTIVE' AND tenant_id IS NULL),
    overall_status_id=(SELECT id FROM module_statuses WHERE code='ACTIVE' AND tenant_id IS NULL),
    progress_percentage=100,
    is_production_ready=TRUE
WHERE code='orders';
