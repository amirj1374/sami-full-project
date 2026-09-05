-- Treasury and cash management is intentionally independent from the planned
-- accounting ledger.  This migration creates operational cash evidence only;
-- no accounting journals are created from these records.

CREATE TABLE treasury_account_types (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT REFERENCES tenants(id) ON DELETE CASCADE,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    requires_bank_details BOOLEAN NOT NULL DEFAULT FALSE,
    allows_negative_balance BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_treasury_account_types_scope_code UNIQUE NULLS NOT DISTINCT(tenant_id, code)
);

CREATE TABLE treasury_transaction_types (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT REFERENCES tenants(id) ON DELETE CASCADE,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(100) NOT NULL,
    direction VARCHAR(12) NOT NULL CHECK(direction IN ('INFLOW','OUTFLOW','TRANSFER')),
    description VARCHAR(255), active BOOLEAN NOT NULL DEFAULT TRUE, is_system BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_treasury_transaction_types_scope_code UNIQUE NULLS NOT DISTINCT(tenant_id, code)
);

CREATE TABLE treasury_transaction_statuses (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT REFERENCES tenants(id) ON DELETE CASCADE,
    code VARCHAR(64) NOT NULL, name VARCHAR(100) NOT NULL, description VARCHAR(255),
    allows_editing BOOLEAN NOT NULL DEFAULT FALSE, is_draft_state BOOLEAN NOT NULL DEFAULT FALSE,
    is_pending_state BOOLEAN NOT NULL DEFAULT FALSE, is_approved_state BOOLEAN NOT NULL DEFAULT FALSE,
    is_completed_state BOOLEAN NOT NULL DEFAULT FALSE, is_rejected_state BOOLEAN NOT NULL DEFAULT FALSE,
    is_cancelled_state BOOLEAN NOT NULL DEFAULT FALSE, is_terminal BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE, is_system BOOLEAN NOT NULL DEFAULT FALSE, display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_treasury_transaction_statuses_scope_code UNIQUE NULLS NOT DISTINCT(tenant_id, code)
);

CREATE TABLE treasury_cheque_statuses (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT REFERENCES tenants(id) ON DELETE CASCADE,
    code VARCHAR(64) NOT NULL, name VARCHAR(100) NOT NULL, description VARCHAR(255),
    terminal BOOLEAN NOT NULL DEFAULT FALSE, active BOOLEAN NOT NULL DEFAULT TRUE,
    is_system BOOLEAN NOT NULL DEFAULT FALSE, display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_treasury_cheque_statuses_scope_code UNIQUE NULLS NOT DISTINCT(tenant_id, code)
);

CREATE TABLE treasury_categories (
    id BIGSERIAL PRIMARY KEY, tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    kind VARCHAR(12) NOT NULL CHECK(kind IN ('INCOME','EXPENSE')), code VARCHAR(64) NOT NULL, name VARCHAR(100) NOT NULL,
    description VARCHAR(255), active BOOLEAN NOT NULL DEFAULT TRUE, display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_treasury_categories_scope_code UNIQUE(tenant_id, kind, code)
);

CREATE TABLE treasury_accounts (
    id BIGSERIAL PRIMARY KEY, tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    company_id BIGINT REFERENCES companies(id) ON DELETE RESTRICT, branch_id BIGINT REFERENCES branches(id) ON DELETE RESTRICT,
    account_type_id BIGINT NOT NULL REFERENCES treasury_account_types(id) ON DELETE RESTRICT,
    code VARCHAR(64) NOT NULL, name VARCHAR(160) NOT NULL, currency_code VARCHAR(3) NOT NULL DEFAULT 'IRR',
    opening_balance NUMERIC(18,2) NOT NULL DEFAULT 0, current_balance NUMERIC(18,2) NOT NULL DEFAULT 0,
    allow_negative_balance BOOLEAN NOT NULL DEFAULT FALSE, responsible_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    bank_name VARCHAR(160), bank_branch VARCHAR(160), iban VARCHAR(64), account_number VARCHAR(80), card_number VARCHAR(32), account_holder VARCHAR(160),
    description VARCHAR(1000), active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_treasury_accounts_scope_code UNIQUE(tenant_id, code)
);
CREATE INDEX ix_treasury_accounts_scope_active ON treasury_accounts(tenant_id, active, name);

CREATE TABLE treasury_transactions (
    id BIGSERIAL PRIMARY KEY, tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    transaction_number VARCHAR(64) NOT NULL, transaction_type_id BIGINT NOT NULL REFERENCES treasury_transaction_types(id) ON DELETE RESTRICT,
    status_id BIGINT NOT NULL REFERENCES treasury_transaction_statuses(id) ON DELETE RESTRICT,
    category_id BIGINT REFERENCES treasury_categories(id) ON DELETE RESTRICT,
    source_account_id BIGINT REFERENCES treasury_accounts(id) ON DELETE RESTRICT,
    destination_account_id BIGINT REFERENCES treasury_accounts(id) ON DELETE RESTRICT,
    amount NUMERIC(18,2) NOT NULL CHECK(amount > 0), currency_code VARCHAR(3) NOT NULL DEFAULT 'IRR', occurred_at TIMESTAMPTZ NOT NULL,
    reference_module VARCHAR(64), reference_number VARCHAR(160), description VARCHAR(2000),
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL, approved_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    approved_at TIMESTAMPTZ, completed_at TIMESTAMPTZ, cancelled_at TIMESTAMPTZ, reversal_of_id BIGINT REFERENCES treasury_transactions(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_treasury_transactions_scope_number UNIQUE(tenant_id, transaction_number),
    CONSTRAINT ck_treasury_transaction_accounts CHECK(source_account_id IS NOT NULL OR destination_account_id IS NOT NULL),
    CONSTRAINT ck_treasury_transaction_distinct_accounts CHECK(source_account_id IS NULL OR destination_account_id IS NULL OR source_account_id <> destination_account_id)
);
CREATE INDEX ix_treasury_transactions_scope_date ON treasury_transactions(tenant_id, occurred_at DESC, id DESC);
CREATE INDEX ix_treasury_transactions_scope_status ON treasury_transactions(tenant_id, status_id, occurred_at DESC);

CREATE TABLE treasury_movements (
    id BIGSERIAL PRIMARY KEY, tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    transaction_id BIGINT NOT NULL REFERENCES treasury_transactions(id) ON DELETE RESTRICT,
    account_id BIGINT NOT NULL REFERENCES treasury_accounts(id) ON DELETE RESTRICT,
    amount NUMERIC(18,2) NOT NULL CHECK(amount <> 0), balance_after NUMERIC(18,2) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_treasury_movement_transaction_account UNIQUE(transaction_id, account_id)
);
CREATE INDEX ix_treasury_movements_scope_account_date ON treasury_movements(tenant_id, account_id, occurred_at DESC, id DESC);

CREATE TABLE treasury_cheques (
    id BIGSERIAL PRIMARY KEY, tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    direction VARCHAR(12) NOT NULL CHECK(direction IN ('RECEIVED','ISSUED')), cheque_number VARCHAR(100) NOT NULL,
    normalized_bank_name VARCHAR(160) NOT NULL, bank_name VARCHAR(160) NOT NULL, bank_branch VARCHAR(160),
    amount NUMERIC(18,2) NOT NULL CHECK(amount > 0), currency_code VARCHAR(3) NOT NULL DEFAULT 'IRR',
    owner_name VARCHAR(160), recipient_name VARCHAR(160), issue_date DATE, due_date DATE,
    status_id BIGINT NOT NULL REFERENCES treasury_cheque_statuses(id) ON DELETE RESTRICT,
    treasury_account_id BIGINT REFERENCES treasury_accounts(id) ON DELETE RESTRICT,
    transaction_id BIGINT REFERENCES treasury_transactions(id) ON DELETE RESTRICT,
    image_file_id BIGINT, description VARCHAR(2000), status_changed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_treasury_cheques_scope_identity UNIQUE(tenant_id, direction, normalized_bank_name, cheque_number)
);
CREATE INDEX ix_treasury_cheques_scope_due ON treasury_cheques(tenant_id, due_date, status_id);

CREATE TABLE treasury_daily_closings (
    id BIGSERIAL PRIMARY KEY, tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    account_id BIGINT NOT NULL REFERENCES treasury_accounts(id) ON DELETE RESTRICT, closing_date DATE NOT NULL,
    expected_balance NUMERIC(18,2) NOT NULL, declared_balance NUMERIC(18,2) NOT NULL, difference_amount NUMERIC(18,2) NOT NULL,
    note VARCHAR(1000), closed_by BIGINT REFERENCES users(id) ON DELETE SET NULL, closed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_treasury_daily_closings_scope UNIQUE(tenant_id, account_id, closing_date)
);

CREATE TABLE treasury_audit_log (
    id BIGSERIAL PRIMARY KEY, tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    entity_type VARCHAR(64) NOT NULL, entity_id BIGINT, action VARCHAR(64) NOT NULL,
    old_values JSONB, new_values JSONB, actor_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    actor_email VARCHAR(255), created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_treasury_audit_scope_entity ON treasury_audit_log(tenant_id, entity_type, entity_id, created_at DESC);

INSERT INTO treasury_account_types(code,name,requires_bank_details,allows_negative_balance,is_system,display_order) VALUES
 ('cashbox','Cash box',FALSE,FALSE,TRUE,10),('bank','Bank account',TRUE,FALSE,TRUE,20),('pos','POS terminal',FALSE,FALSE,TRUE,30),
 ('gateway','Payment gateway',FALSE,FALSE,TRUE,40),('petty-cash','Petty cash',FALSE,FALSE,TRUE,50),('safe','Safe',FALSE,FALSE,TRUE,60);
INSERT INTO treasury_transaction_types(code,name,direction,is_system,display_order) VALUES
 ('cash-in','Cash in','INFLOW',TRUE,10),('cash-out','Cash out','OUTFLOW',TRUE,20),('transfer','Transfer','TRANSFER',TRUE,30),
 ('deposit','Deposit','INFLOW',TRUE,40),('withdrawal','Withdrawal','OUTFLOW',TRUE,50),('income','Income','INFLOW',TRUE,60),
 ('expense','Expense','OUTFLOW',TRUE,70),('refund','Refund','OUTFLOW',TRUE,80),('cheque-collection','Cheque collection','INFLOW',TRUE,90),('cheque-payment','Cheque payment','OUTFLOW',TRUE,100),
 ('advance','Advance','OUTFLOW',TRUE,110),('salary','Salary','OUTFLOW',TRUE,120);
INSERT INTO treasury_transaction_statuses(code,name,allows_editing,is_draft_state,is_pending_state,is_approved_state,is_completed_state,is_rejected_state,is_cancelled_state,is_terminal,is_system,display_order) VALUES
 ('draft','Draft',TRUE,TRUE,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,TRUE,10),('pending','Pending approval',FALSE,FALSE,TRUE,FALSE,FALSE,FALSE,FALSE,FALSE,TRUE,20),
 ('approved','Approved',FALSE,FALSE,FALSE,TRUE,FALSE,FALSE,FALSE,FALSE,TRUE,30),('completed','Completed',FALSE,FALSE,FALSE,FALSE,TRUE,FALSE,FALSE,TRUE,TRUE,40),
 ('rejected','Rejected',FALSE,FALSE,FALSE,FALSE,FALSE,TRUE,FALSE,TRUE,TRUE,50),('cancelled','Cancelled',FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,TRUE,TRUE,TRUE,60);
INSERT INTO treasury_cheque_statuses(code,name,terminal,is_system,display_order) VALUES
 ('draft','Draft',FALSE,TRUE,10),('received','Received',FALSE,TRUE,20),('issued','Issued',FALSE,TRUE,30),('deposited','Deposited',FALSE,TRUE,40),
 ('cleared','Cleared',TRUE,TRUE,50),('bounced','Bounced',TRUE,TRUE,60),('cancelled','Cancelled',TRUE,TRUE,70),('returned','Returned',TRUE,TRUE,80);

INSERT INTO modules(code,name,description,icon,path,display_order,enabled,is_system,backend_status_id,frontend_status_id,overall_status_id,progress_percentage,development_notes,is_available,is_production_ready)
VALUES('treasury','Treasury','Cash, bank accounts, cheque lifecycle and cash-flow control','mdi-bank','/treasury',145,TRUE,FALSE,
 (SELECT id FROM module_statuses WHERE code='ACTIVE' AND tenant_id IS NULL),(SELECT id FROM module_statuses WHERE code='ACTIVE' AND tenant_id IS NULL),(SELECT id FROM module_statuses WHERE code='ACTIVE' AND tenant_id IS NULL),100,
 'Tenant-scoped operational treasury with immutable movements; independent from accounting.',TRUE,TRUE)
ON CONFLICT(code) DO UPDATE SET name=EXCLUDED.name,description=EXCLUDED.description,icon=EXCLUDED.icon,path=EXCLUDED.path,enabled=TRUE,is_available=TRUE,is_production_ready=TRUE,
 backend_status_id=EXCLUDED.backend_status_id,frontend_status_id=EXCLUDED.frontend_status_id,overall_status_id=EXCLUDED.overall_status_id,progress_percentage=100,development_notes=EXCLUDED.development_notes;

INSERT INTO permissions(module_id,action,code,name,is_system)
SELECT m.id,p.action,'treasury:'||p.action,p.name,TRUE FROM modules m CROSS JOIN (VALUES
 ('view','View treasury'),('manage','Manage treasury accounts and configuration'),('create','Create treasury transactions'),('edit','Edit treasury drafts'),
 ('approve','Approve treasury transactions'),('complete','Complete treasury transactions'),('cancel','Cancel treasury transactions'),('manage-cheques','Manage cheques'),
 ('close','Close cash accounts'),('report','View treasury reports'),('export','Export treasury data'),('import','Import treasury data'),('view-audit','View treasury audit')) p(action,name)
WHERE m.code='treasury' ON CONFLICT(module_id,action) DO UPDATE SET code=EXCLUDED.code,name=EXCLUDED.name,is_system=TRUE;

INSERT INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM roles r JOIN permissions p ON p.code LIKE 'treasury:%'
WHERE r.is_super_admin OR lower(r.name) IN ('admin','administrator') ON CONFLICT DO NOTHING;
