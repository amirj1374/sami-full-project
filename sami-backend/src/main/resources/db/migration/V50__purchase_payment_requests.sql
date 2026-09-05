CREATE TABLE purchase_payment_settings (
    tenant_id BIGINT PRIMARY KEY REFERENCES tenants(id) ON DELETE CASCADE,
    manager_approval_required BOOLEAN NOT NULL DEFAULT TRUE,
    payment_deadline_minutes INTEGER NOT NULL DEFAULT 20 CHECK(payment_deadline_minutes BETWEEN 1 AND 1440),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE purchase_payment_requests (
    id BIGSERIAL PRIMARY KEY, tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    request_number VARCHAR(64) NOT NULL, requester_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    supplier_name VARCHAR(200), purpose VARCHAR(1000) NOT NULL, requested_amount NUMERIC(18,2) NOT NULL CHECK(requested_amount > 0),
    paid_amount NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK(paid_amount >= 0), currency_code VARCHAR(3) NOT NULL DEFAULT 'IRR',
    document_reference VARCHAR(200), attachment_file_id BIGINT, status VARCHAR(40) NOT NULL,
    manager_id BIGINT REFERENCES users(id) ON DELETE SET NULL, manager_decided_at TIMESTAMPTZ, rejection_reason VARCHAR(1000),
    assigned_accountant_id BIGINT REFERENCES users(id) ON DELETE SET NULL, assigned_at TIMESTAMPTZ, payment_deadline_at TIMESTAMPTZ,
    delay_reason VARCHAR(1000), completed_at TIMESTAMPTZ, cancelled_at TIMESTAMPTZ,
    requester_viewed_at TIMESTAMPTZ, accountant_viewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_purchase_payment_request_number UNIQUE(tenant_id,request_number),
    CONSTRAINT ck_purchase_payment_amount CHECK(paid_amount <= requested_amount),
    CONSTRAINT ck_purchase_payment_status CHECK(status IN ('DRAFT','WAITING_MANAGER','APPROVED','WAITING_PAYMENT','PARTIALLY_PAID','PAID','REJECTED','OVERDUE','CANCELLED'))
);
CREATE INDEX ix_purchase_payment_requests_scope_status ON purchase_payment_requests(tenant_id,status,created_at DESC);
CREATE INDEX ix_purchase_payment_requests_requester ON purchase_payment_requests(tenant_id,requester_id,created_at DESC);

CREATE TABLE purchase_payment_daily_limits (
    id BIGSERIAL PRIMARY KEY, tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    treasury_account_id BIGINT NOT NULL REFERENCES treasury_accounts(id) ON DELETE CASCADE,
    payment_date DATE NOT NULL, method VARCHAR(32) NOT NULL, limit_amount NUMERIC(18,2) NOT NULL CHECK(limit_amount >= 0),
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL, updated_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_purchase_payment_limit UNIQUE(tenant_id,treasury_account_id,payment_date,method),
    CONSTRAINT ck_purchase_payment_method CHECK(method IN ('ACCOUNT_TRANSFER','CARD_TRANSFER','IBAN','FROM_BRANCH'))
);

CREATE TABLE purchase_payment_receipts (
    id BIGSERIAL PRIMARY KEY, tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    request_id BIGINT NOT NULL REFERENCES purchase_payment_requests(id) ON DELETE RESTRICT,
    treasury_account_id BIGINT NOT NULL REFERENCES treasury_accounts(id) ON DELETE RESTRICT,
    treasury_transaction_id BIGINT REFERENCES treasury_transactions(id) ON DELETE RESTRICT,
    method VARCHAR(32) NOT NULL, amount NUMERIC(18,2) NOT NULL CHECK(amount > 0), paid_at TIMESTAMPTZ NOT NULL,
    reference_number VARCHAR(200) NOT NULL, receipt_file_id BIGINT, note VARCHAR(1000), created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_purchase_payment_reference UNIQUE(tenant_id,treasury_account_id,reference_number),
    CONSTRAINT ck_purchase_receipt_method CHECK(method IN ('ACCOUNT_TRANSFER','CARD_TRANSFER','IBAN','FROM_BRANCH'))
);
CREATE INDEX ix_purchase_payment_receipts_request ON purchase_payment_receipts(tenant_id,request_id,paid_at);

CREATE TABLE purchase_payment_audit_log (
    id BIGSERIAL PRIMARY KEY, tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    request_id BIGINT REFERENCES purchase_payment_requests(id) ON DELETE CASCADE, action VARCHAR(64) NOT NULL,
    actor_id BIGINT REFERENCES users(id) ON DELETE SET NULL, details JSONB, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO modules(code,name,description,icon,path,display_order,enabled,is_system,backend_status_id,frontend_status_id,overall_status_id,is_available,is_production_ready,progress_percentage)
VALUES('purchase-payments','Purchase Payments','Approval and controlled treasury payment of purchase requests','mdi-cash-clock','/purchase-payments',146,TRUE,FALSE,
 (SELECT id FROM module_statuses WHERE code='ACTIVE' AND tenant_id IS NULL),(SELECT id FROM module_statuses WHERE code='ACTIVE' AND tenant_id IS NULL),
 (SELECT id FROM module_statuses WHERE code='ACTIVE' AND tenant_id IS NULL),TRUE,TRUE,100)
ON CONFLICT(code) DO UPDATE SET name=EXCLUDED.name,description=EXCLUDED.description,icon=EXCLUDED.icon,path=EXCLUDED.path,enabled=TRUE,
 backend_status_id=EXCLUDED.backend_status_id,frontend_status_id=EXCLUDED.frontend_status_id,overall_status_id=EXCLUDED.overall_status_id,
 is_available=TRUE,is_production_ready=TRUE,progress_percentage=100;

INSERT INTO permissions(module_id,action,code,name,is_system)
SELECT m.id,p.action,'purchase-payments:'||p.action,p.name,TRUE FROM modules m CROSS JOIN (VALUES
 ('create','Create own payment requests'),('view-own','View own payment requests'),('view-all','View all payment requests'),
 ('approve','Approve or reject payment requests'),('process','Process payments'),('manage-limits','Manage daily payment limits'),('report','View payment reports')) p(action,name)
WHERE m.code='purchase-payments' ON CONFLICT(module_id,action) DO UPDATE SET code=EXCLUDED.code,name=EXCLUDED.name,is_system=TRUE;

INSERT INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM roles r JOIN permissions p ON p.code LIKE 'purchase-payments:%'
WHERE r.is_super_admin OR lower(r.name) IN ('admin','administrator') ON CONFLICT DO NOTHING;

INSERT INTO scheduled_jobs(code,name,description,handler_key,schedule_kind,cron_expression,config,status_id,timezone,timeout_seconds,catch_up,run_on_startup,is_system,tenant_id)
SELECT 'purchase-payment-reminders','Purchase payment reminders','Creates idempotent reminders and marks overdue payment requests',
 'purchase-payment.reminders','CRON','0 * * * * *','{}'::jsonb,s.id,'Asia/Tehran',120,FALSE,FALSE,TRUE,t.id
FROM tenants t CROSS JOIN LATERAL (SELECT id FROM job_statuses WHERE code='active' ORDER BY tenant_id NULLS FIRST LIMIT 1) s
ON CONFLICT(tenant_id,code) DO NOTHING;
