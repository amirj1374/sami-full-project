# Files and communications

## Managed files

The `files` module provides managed metadata, upload, content retrieval,
versions, rollback, folders/tags, derivatives, scans, quota, retention, audit,
reports, and a local storage provider. Its API is under `/api/v1/files`.

The repository also retains `common.storage` for avatars, supplier documents,
and purchase attachments. Configuration therefore has both `app.storage` and
`app.files`. Production Compose explicitly persists the legacy uploads path;
the operational persistence contract for the newer file and staging paths is
not complete.

Do not migrate consumers between these systems without defining ownership,
authorization, metadata migration, checksum validation, and rollback.

## Communication

The communication hub persists providers, templates, messages, attempts,
preferences, and audit-oriented state. Queueing a message is durable, but
delivery depends on a `CommProviderHandler`. No handler implementation is
currently registered.

Portal OTP similarly depends on `OtpDeliveryChannel`, with no implementation
found. External delivery must therefore be classified as unavailable until an
adapter, secret contract, retry behavior, and non-production test provider are
verified.
