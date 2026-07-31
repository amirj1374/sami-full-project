-- Enterprise Sales aggregate and its minimum canonical inventory/payment/accounting contracts.

CREATE TABLE sale_numbers (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    sequence_year INTEGER NOT NULL,
    next_value BIGINT NOT NULL DEFAULT 1 CHECK (next_value > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_sale_numbers UNIQUE (tenant_id, sequence_year)
);

CREATE TABLE sales (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE RESTRICT,
    branch_id BIGINT NOT NULL REFERENCES branches(id) ON DELETE RESTRICT,
    invoice_number VARCHAR(40) NOT NULL,
    idempotency_key VARCHAR(100),
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    seller_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    sale_type VARCHAR(32) NOT NULL,
    status VARCHAR(24) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'IRR',
    subtotal NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK (subtotal >= 0),
    discount_total NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK (discount_total >= 0),
    tax_total NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK (tax_total >= 0),
    cost_total NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK (cost_total >= 0),
    final_amount NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK (final_amount >= 0),
    profit NUMERIC(18,2) NOT NULL DEFAULT 0,
    commission_amount NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK (commission_amount >= 0),
    notes VARCHAR(2000),
    confirmed_at TIMESTAMPTZ, completed_at TIMESTAMPTZ, cancelled_at TIMESTAMPTZ,
    cancellation_reason VARCHAR(500), created_by BIGINT, created_by_email VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_sales_invoice UNIQUE (tenant_id, invoice_number),
    CONSTRAINT uq_sales_idempotency UNIQUE (tenant_id, idempotency_key),
    CONSTRAINT ck_sales_status CHECK (status IN ('DRAFT','CONFIRMED','COMPLETED','CANCELLED','PARTIALLY_RETURNED','RETURNED'))
);
CREATE INDEX ix_sales_scope_status ON sales(tenant_id, company_id, branch_id, status);
CREATE INDEX ix_sales_customer ON sales(tenant_id, customer_id, created_at DESC);

CREATE TABLE sale_items (
    id BIGSERIAL PRIMARY KEY, sale_id BIGINT NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    product_sku VARCHAR(100) NOT NULL, product_name VARCHAR(255) NOT NULL,
    serial_number VARCHAR(128), imei VARCHAR(32), quantity NUMERIC(14,3) NOT NULL CHECK (quantity > 0),
    returned_quantity NUMERIC(14,3) NOT NULL DEFAULT 0 CHECK (returned_quantity >= 0 AND returned_quantity <= quantity),
    unit_price NUMERIC(18,2) NOT NULL CHECK (unit_price >= 0), cost_price NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK (cost_price >= 0),
    discount NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK (discount >= 0), tax NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK (tax >= 0),
    line_total NUMERIC(18,2) NOT NULL CHECK (line_total >= 0), profit NUMERIC(18,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_sale_item_imei ON sale_items(tenant_id, imei) WHERE imei IS NOT NULL;
CREATE UNIQUE INDEX uq_sale_item_serial ON sale_items(tenant_id, serial_number) WHERE serial_number IS NOT NULL;
CREATE INDEX ix_sale_items_sale ON sale_items(sale_id);

CREATE TABLE sale_payments (
    id BIGSERIAL PRIMARY KEY, sale_id BIGINT NOT NULL REFERENCES sales(id) ON DELETE CASCADE, tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    method VARCHAR(24) NOT NULL, status VARCHAR(24) NOT NULL DEFAULT 'CAPTURED', amount NUMERIC(18,2) NOT NULL CHECK(amount > 0),
    reference_no VARCHAR(160), provider_reference VARCHAR(255), reversed_amount NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK(reversed_amount >= 0 AND reversed_amount <= amount),
    paid_at TIMESTAMPTZ NOT NULL DEFAULT now(), created_by BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_sale_payment_method CHECK(method IN ('CASH','CARD','TRANSFER','CHEQUE','WALLET','INSTALLMENT')),
    CONSTRAINT ck_sale_payment_status CHECK(status IN ('PENDING','CAPTURED','FAILED','REVERSED','PARTIALLY_REVERSED'))
);
CREATE INDEX ix_sale_payments_sale ON sale_payments(sale_id);

CREATE TABLE sale_services (
    id BIGSERIAL PRIMARY KEY, sale_id BIGINT NOT NULL REFERENCES sales(id) ON DELETE CASCADE, tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    service_type VARCHAR(48) NOT NULL, description VARCHAR(255), price NUMERIC(18,2) NOT NULL CHECK(price >= 0), cost NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK(cost >= 0), employee_id BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE sale_discounts (
    id BIGSERIAL PRIMARY KEY, sale_id BIGINT NOT NULL REFERENCES sales(id) ON DELETE CASCADE, tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    discount_type VARCHAR(24) NOT NULL, amount NUMERIC(18,2) NOT NULL CHECK(amount >= 0), reason VARCHAR(500) NOT NULL,
    status VARCHAR(24) NOT NULL, requested_by BIGINT, approved_by BIGINT, requested_at TIMESTAMPTZ NOT NULL DEFAULT now(), decided_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_sale_discount_status CHECK(status IN ('NOT_REQUIRED','PENDING','APPROVED','REJECTED'))
);

CREATE TABLE sale_returns (
    id BIGSERIAL PRIMARY KEY, sale_id BIGINT NOT NULL REFERENCES sales(id) ON DELETE RESTRICT, tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    return_number VARCHAR(48) NOT NULL, reason VARCHAR(500) NOT NULL, refund_method VARCHAR(24) NOT NULL,
    total_amount NUMERIC(18,2) NOT NULL CHECK(total_amount >= 0), created_by BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_sale_return_number UNIQUE(tenant_id, return_number)
);
CREATE TABLE sale_return_items (
    id BIGSERIAL PRIMARY KEY, return_id BIGINT NOT NULL REFERENCES sale_returns(id) ON DELETE CASCADE,
    sale_item_id BIGINT NOT NULL REFERENCES sale_items(id) ON DELETE RESTRICT, quantity NUMERIC(14,3) NOT NULL CHECK(quantity > 0), amount NUMERIC(18,2) NOT NULL CHECK(amount >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE sale_audit_history (
    id BIGSERIAL PRIMARY KEY, sale_id BIGINT NOT NULL REFERENCES sales(id) ON DELETE CASCADE, tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    action VARCHAR(48) NOT NULL, actor_id BIGINT, actor_email VARCHAR(255), old_value JSONB, new_value JSONB, ip_address VARCHAR(64), device VARCHAR(255), occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX ix_sale_audit_timeline ON sale_audit_history(tenant_id, sale_id, occurred_at DESC);

CREATE TABLE sales_inventory_units (
    id BIGSERIAL PRIMARY KEY, tenant_id BIGINT NOT NULL REFERENCES tenants(id), branch_id BIGINT NOT NULL REFERENCES branches(id), product_id BIGINT NOT NULL REFERENCES products(id),
    serial_number VARCHAR(128), imei VARCHAR(32), quantity NUMERIC(14,3) NOT NULL DEFAULT 0 CHECK(quantity >= 0), reserved_quantity NUMERIC(14,3) NOT NULL DEFAULT 0 CHECK(reserved_quantity >= 0 AND reserved_quantity <= quantity),
    status VARCHAR(24) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_inventory_imei ON sales_inventory_units(tenant_id, imei) WHERE imei IS NOT NULL;
CREATE UNIQUE INDEX uq_inventory_serial ON sales_inventory_units(tenant_id, serial_number) WHERE serial_number IS NOT NULL;
CREATE UNIQUE INDEX uq_inventory_bulk ON sales_inventory_units(tenant_id, branch_id, product_id) WHERE imei IS NULL AND serial_number IS NULL;
CREATE INDEX ix_inventory_available ON sales_inventory_units(tenant_id, branch_id, product_id, status);

CREATE TABLE sales_stock_movements (
    id BIGSERIAL PRIMARY KEY, tenant_id BIGINT NOT NULL REFERENCES tenants(id), sale_id BIGINT NOT NULL REFERENCES sales(id), sale_item_id BIGINT REFERENCES sale_items(id), inventory_unit_id BIGINT REFERENCES sales_inventory_units(id),
    movement_type VARCHAR(24) NOT NULL, quantity NUMERIC(14,3) NOT NULL CHECK(quantity > 0), occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(), actor_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE sale_accounting_entries (
    id BIGSERIAL PRIMARY KEY, tenant_id BIGINT NOT NULL REFERENCES tenants(id), sale_id BIGINT NOT NULL REFERENCES sales(id), entry_type VARCHAR(24) NOT NULL,
    account_code VARCHAR(48) NOT NULL,
    debit NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK(debit >= 0), credit NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK(credit >= 0), reference_no VARCHAR(80) NOT NULL,
    posted_at TIMESTAMPTZ NOT NULL DEFAULT now(), reversed_entry_id BIGINT REFERENCES sale_accounting_entries(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE sale_ai_recommendations (
    id BIGSERIAL PRIMARY KEY, tenant_id BIGINT NOT NULL REFERENCES tenants(id), sale_id BIGINT REFERENCES sales(id), recommendation_type VARCHAR(48) NOT NULL,
    request_data JSONB NOT NULL DEFAULT '{}'::jsonb, result_data JSONB NOT NULL DEFAULT '{}'::jsonb, accepted BOOLEAN, model_key VARCHAR(100), created_by BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0
);

UPDATE modules SET name='Sales', icon='mdi-point-of-sale', path='/sales', enabled=TRUE,
    backend_status_id=(SELECT id FROM module_statuses WHERE code='BACKEND_READY' AND tenant_id IS NULL),
    frontend_status_id=(SELECT id FROM module_statuses WHERE code='PLANNED' AND tenant_id IS NULL),
    overall_status_id=(SELECT id FROM module_statuses WHERE code='IN_DEVELOPMENT' AND tenant_id IS NULL),
    progress_percentage=60, is_production_ready=FALSE
WHERE code='orders';

INSERT INTO permissions(module_id, action, code, name, is_system)
SELECT m.id, p.action, 'sales:'||p.action, p.name, TRUE FROM modules m
CROSS JOIN (VALUES
 ('view','View sales'),('create','Create sales'),('edit','Edit draft sales'),('confirm','Confirm sales'),
 ('complete','Complete sales'),('cancel','Cancel sales'),('return','Return sales'),('payment','Manage sale payments'),
 ('approve-discount','Approve sale discounts'),('view-audit','View sale audit'),('report','View sale reports'),('export','Export sales')
) p(action,name) WHERE m.code='orders'
ON CONFLICT(module_id, action) DO UPDATE
SET code=EXCLUDED.code, name=EXCLUDED.name, is_system=TRUE;

-- Retire the two generic scaffold actions that Sales deliberately does not
-- expose. Keeping the permission rows preserves historical role references,
-- while disabling their codes prevents them from masquerading as real APIs.
UPDATE permissions SET code='orders-legacy:'||action, name='Legacy orders '||action
WHERE module_id=(SELECT id FROM modules WHERE code='orders')
  AND action IN ('delete','import');
