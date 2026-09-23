-- Scheduled, idempotent CRM reminder for completed sales.
INSERT INTO scheduled_jobs (code, name, description, handler_key, schedule_kind,
    cron_expression, config, status_id, timezone, timeout_seconds, catch_up,
    run_on_startup, is_system, tenant_id)
SELECT 'crm-satisfaction-reminders', 'CRM satisfaction reminders',
       'Create one seller follow-up three days after a completed sale',
       'crm.satisfaction-reminders', 'CRON', '0 15 2 * * *', '{}'::jsonb,
       s.id, 'UTC', 120, true, false, true, t.id
FROM tenants t CROSS JOIN (SELECT id FROM job_statuses WHERE code='draft' AND tenant_id IS NULL LIMIT 1) s
WHERE NOT EXISTS (SELECT 1 FROM scheduled_jobs j WHERE j.tenant_id=t.id AND j.code='crm-satisfaction-reminders');
