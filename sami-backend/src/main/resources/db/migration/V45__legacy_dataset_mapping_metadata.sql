ALTER TABLE legacy_datasets
    ADD COLUMN metadata JSONB NOT NULL DEFAULT '{}'::jsonb;

COMMENT ON COLUMN legacy_datasets.metadata IS
    'Source-manifest mapping, header evidence, legacy-key quality and read-only reconciliation totals.';
