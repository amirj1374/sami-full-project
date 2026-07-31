package com.sami.app.automation.provider;

import com.sami.app.automation.engine.AutomationEngine;
import com.sami.app.automation.spi.AutomationContext;
import com.sami.app.common.scheduler.spi.JobContext;
import com.sami.app.common.scheduler.spi.JobHandler;
import com.sami.app.common.scheduler.spi.JobResult;
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

        HashMap<String, Object> data = new HashMap<>(job.config());
        data.put("jobCode", job.jobCode());
        data.put("scheduledFor", job.scheduledFor().toString());
        data.put("manual", job.manual());
        engine.executeRule(ruleId, new AutomationContext(
                "automation.schedule.DUE", "automation", "rule", ruleId, data,
                job.tenantId(), longValue(job.config().get("companyId")),
                longValue(job.config().get("branchId")), null,
                job.scheduledFor() == null ? Instant.now() : job.scheduledFor(), 0));
        return JobResult.ok("Automation rule executed", 1);
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
