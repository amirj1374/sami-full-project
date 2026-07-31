package com.sami.app.automation.provider;

import com.sami.app.automation.service.AutomationService;
import com.sami.app.common.scheduler.spi.JobContext;
import com.sami.app.common.scheduler.spi.JobHandler;
import com.sami.app.common.scheduler.spi.JobResult;
import com.sami.app.common.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Drains one tenant's due Automation failures through the shared scheduler. */
@Component
@RequiredArgsConstructor
public class AutomationFailureRetryJobHandler implements JobHandler {

    public static final String KEY = "automation.failure-retry";

    private final AutomationService service;
    private final TenantContext tenantContext;

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String description() {
        return "Retry due Automation failures";
    }

    @Override
    public JobResult execute(JobContext context) {
        if (context.tenantId() == null) {
            return JobResult.failed("Trusted tenant scope is required");
        }
        int processed = tenantContext.callAsTenant(context.tenantId(),
                () -> service.retryDueFailures(context.tenantId(), context.integer("batchSize", 25)));
        return JobResult.ok("processed=%d".formatted(processed), processed);
    }
}
