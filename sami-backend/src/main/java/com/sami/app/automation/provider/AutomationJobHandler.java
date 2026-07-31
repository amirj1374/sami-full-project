package com.sami.app.automation.provider;

import com.sami.app.automation.engine.AutomationEngine;
import com.sami.app.automation.domain.AutomationExecution;
import com.sami.app.automation.spi.AutomationContext;
import com.sami.app.common.scheduler.spi.JobContext;
import com.sami.app.common.scheduler.spi.JobHandler;
import com.sami.app.common.scheduler.spi.JobResult;
import com.sami.app.common.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;

/**
 * Scheduler plugin for configured Automation rules. The shared scheduler owns
 * clocks, cron, time zones, retries, timeouts and execution history; this
 * handler only converts a tenant-scoped scheduled job into an Automation
 * context. Configure a scheduled job with {@code handlerKey=automation.rule}
 * and {@code config.ruleId}.
 */
@Component
@RequiredArgsConstructor
public class AutomationJobHandler implements JobHandler {

    public static final String KEY = "automation.rule";

    private final AutomationEngine engine;
    private final TenantContext tenantContext;

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String description() {
        return "Execute a configured Automation rule";
    }

    @Override
    public JobResult execute(JobContext job) {
        Long ruleId = longValue(job.config().get("ruleId"));
        if (ruleId == null) {
            return JobResult.failed("config.ruleId is required");
        }
        if (job.tenantId() == null) {
            return JobResult.failed("Trusted tenant scope is required");
        }

        Instant scheduledFor = job.scheduledFor() == null ? Instant.now() : job.scheduledFor();
        HashMap<String, Object> data = new HashMap<>(job.config());
        data.put("jobCode", job.jobCode());
        data.put("scheduledFor", scheduledFor.toString());
        data.put("manual", job.manual());
        AutomationExecution.Status status = tenantContext.callAsTenant(job.tenantId(),
                () -> engine.executeRule(ruleId, new AutomationContext(
                        "automation.schedule.DUE", "automation", "rule", ruleId, data,
                        job.tenantId(), longValue(job.config().get("companyId")),
                        longValue(job.config().get("branchId")), null,
                        scheduledFor, 0)));
        if (status == AutomationExecution.Status.FAILED) {
            return JobResult.failed("Automation rule execution failed");
        }
        return JobResult.ok("Automation rule " + status.name().toLowerCase(),
                status == AutomationExecution.Status.SUCCEEDED ? 1 : 0);
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.valueOf(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
