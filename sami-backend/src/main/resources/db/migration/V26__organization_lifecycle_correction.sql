-- =====================================================================
-- V26 — Correct the `organization` module's lifecycle record
--
-- V25 backfilled every pre-existing module with BACKEND_READY on the
-- assumption that "it shipped, therefore it has a working backend". That
-- assumption does not hold for `organization`.
--
-- What actually exists (verified by repository audit):
--   * V22__organization.sql creates companies / branch_types / branches
--     and seeds the module row and its permissions;
--   * there is NO Java implementation — no entity, repository, service or
--     controller anywhere in any branch or commit. The tables are unreachable
--     through the API.
--
-- BACKEND_READY promises a usable server-side implementation, so the row was
-- claiming an API that cannot be called. The whole point of V25 was to make
-- status honest data rather than an optimistic inference; leaving this row
-- untouched would reintroduce exactly the dishonesty it removed.
--
-- IN_DEVELOPMENT rather than PLANNED: schema work has been delivered, so the
-- module is not un-started — it is part-built. Progress reflects "database
-- layer only".
--
-- Values only, no schema change: this is ordinary editable lifecycle data
-- that an administrator may revise once the Java layer lands.
-- =====================================================================

UPDATE modules SET
    backend_status_id   = (SELECT id FROM module_statuses WHERE code = 'IN_DEVELOPMENT' AND tenant_id IS NULL),
    frontend_status_id  = (SELECT id FROM module_statuses WHERE code = 'PLANNED'        AND tenant_id IS NULL),
    progress_percentage = 15,
    is_production_ready = FALSE,
    development_notes   = 'Schema only (V22): companies, branch_types, branches. No Java implementation yet.'
WHERE code = 'organization';


DO $$
DECLARE
    wrong BIGINT;
BEGIN
    SELECT count(*) INTO wrong
    FROM modules m
    JOIN module_statuses bs ON bs.id = m.backend_status_id
    WHERE m.code = 'organization' AND bs.is_production_ready;

    IF wrong > 0 THEN
        RAISE EXCEPTION 'V26: organization must not hold a production-ready backend status';
    END IF;
END $$;
