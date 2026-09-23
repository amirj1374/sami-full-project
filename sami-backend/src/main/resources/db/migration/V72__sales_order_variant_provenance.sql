ALTER TABLE sales_order_lines ADD COLUMN variant_id BIGINT;
ALTER TABLE sales_order_lines ADD CONSTRAINT fk_sales_order_line_variant_tenant
    FOREIGN KEY (tenant_id, variant_id) REFERENCES product_variants(tenant_id, id)
    ON DELETE RESTRICT;
CREATE INDEX ix_sales_order_lines_variant ON sales_order_lines(tenant_id, product_id, variant_id);
