ALTER TABLE purchases
    ADD COLUMN company_id BIGINT,
    ADD COLUMN branch_id BIGINT,
    ADD COLUMN seller_type VARCHAR(16) NOT NULL DEFAULT 'SUPPLIER',
    ALTER COLUMN supplier_id DROP NOT NULL,
    ADD COLUMN seller_customer_id BIGINT,
    ADD COLUMN linked_sale_id BIGINT,
    ADD COLUMN item_condition VARCHAR(20) NOT NULL DEFAULT 'OTHER',
    ADD COLUMN inspection_notes VARCHAR(2000),
    ADD COLUMN ownership_declared BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN declaration_notes VARCHAR(1000),
    ADD COLUMN valuation_amount NUMERIC(14,2),
    ADD COLUMN settlement_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN settlement_method VARCHAR(40),
    ADD COLUMN settlement_reference VARCHAR(160),
    ADD COLUMN settled_amount NUMERIC(14,2),
    ADD COLUMN settled_at TIMESTAMPTZ;

UPDATE purchases p SET company_id =
    (SELECT id FROM companies WHERE tenant_id = p.tenant_id ORDER BY id LIMIT 1);
UPDATE purchases p SET branch_id =
    (SELECT id FROM branches WHERE tenant_id = p.tenant_id ORDER BY id LIMIT 1);

ALTER TABLE purchases
    ALTER COLUMN company_id SET NOT NULL,
    ALTER COLUMN branch_id SET NOT NULL,
    ADD CONSTRAINT fk_purchases_company FOREIGN KEY (company_id) REFERENCES companies(id),
    ADD CONSTRAINT fk_purchases_branch FOREIGN KEY (branch_id) REFERENCES branches(id),
    ADD CONSTRAINT fk_purchases_seller_customer FOREIGN KEY (seller_customer_id) REFERENCES customers(id),
    ADD CONSTRAINT fk_purchases_linked_sale FOREIGN KEY (linked_sale_id) REFERENCES sales(id),
    ADD CONSTRAINT ck_purchases_seller CHECK (
        (seller_type = 'SUPPLIER' AND supplier_id IS NOT NULL AND seller_customer_id IS NULL)
        OR (seller_type = 'CUSTOMER' AND supplier_id IS NULL AND seller_customer_id IS NOT NULL)
    ),
    ADD CONSTRAINT ck_purchases_condition CHECK (item_condition IN ('USED','NEW_SEALED','OTHER')),
    ADD CONSTRAINT ck_purchases_settlement_status CHECK (settlement_status IN ('PENDING','SETTLED','WAIVED')),
    ADD CONSTRAINT ck_purchases_valuation_nonnegative CHECK (valuation_amount IS NULL OR valuation_amount >= 0),
    ADD CONSTRAINT ck_purchases_settled_nonnegative CHECK (settled_amount IS NULL OR settled_amount >= 0);

CREATE UNIQUE INDEX uq_purchases_tenant_linked_sale ON purchases(tenant_id, linked_sale_id)
    WHERE linked_sale_id IS NOT NULL;
CREATE INDEX idx_purchases_tenant_customer ON purchases(tenant_id, seller_customer_id)
    WHERE seller_customer_id IS NOT NULL;
