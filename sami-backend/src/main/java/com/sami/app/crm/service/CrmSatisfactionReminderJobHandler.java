package com.sami.app.crm.service;

import com.sami.app.common.scheduler.spi.JobContext;
import com.sami.app.common.scheduler.spi.JobHandler;
import com.sami.app.common.scheduler.spi.JobResult;
import com.sami.app.common.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CrmSatisfactionReminderJobHandler implements JobHandler {
    public static final String KEY = "crm.satisfaction-reminders";
    private final TenantContext tenants;
    private final CrmReminderService service;
    @Override public String key() { return KEY; }
    @Override public String description() { return "Create three-day post-sale satisfaction follow-ups"; }
    @Override public JobResult execute(JobContext context) {
        if (context.tenantId() == null) return JobResult.failed("Trusted tenant scope is required");
        int count = tenants.callAsTenant(context.tenantId(), () -> service.processSatisfactionReminders(context.scheduledFor()));
        return JobResult.ok("Created " + count + " satisfaction follow-up(s)", count);
    }
}
