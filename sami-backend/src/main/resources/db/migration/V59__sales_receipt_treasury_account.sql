alter table sales_receipts add column if not exists treasury_account_id bigint references treasury_accounts(id);
