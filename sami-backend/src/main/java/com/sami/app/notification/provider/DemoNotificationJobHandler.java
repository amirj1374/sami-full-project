package com.sami.app.notification.provider;

import com.sami.app.common.scheduler.spi.JobContext;
import com.sami.app.common.scheduler.spi.JobHandler;
import com.sami.app.common.scheduler.spi.JobResult;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.notification.service.StaffNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DemoNotificationJobHandler implements JobHandler {

    public static final String KEY = "notification.demo-hourly";

    private final StaffNotificationService service;
    private final TenantContext tenantContext;

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String description() {
        return "Create opt-in hourly in-app demo notifications";
    }

    @Override
    public JobResult execute(JobContext context) {
        if (context.tenantId() == null) {
            return JobResult.failed("Trusted tenant scope is required");
        }
        int created = tenantContext.callAsTenant(context.tenantId(),
                () -> service.generateHourlyDemo(context.tenantId(), context.scheduledFor()));
        return JobResult.ok("Created " + created + " demo notification(s)", created);
    }
}
