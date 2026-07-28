package com.sami.app.common.scheduler.spi;

import java.time.Instant;
import java.util.Map;

/**
 * What a handler is told about the run it is performing.
 *
 * @param jobCode        the job being executed
 * @param config         the job's JSON configuration
 * @param tenantId       the tenant this run belongs to; handlers that touch
 *                       tenant data must scope by it
 * @param scheduledFor   when the run was due (may be earlier than now if the
 *                       process was down)
 * @param manual         true when triggered by an operator rather than the clock
 * @param previousRunAt  when this job last ran, or null if never
 */
public record JobContext(String jobCode,
                         Map<String, Object> config,
                         Long tenantId,
                         Instant scheduledFor,
                         boolean manual,
                         Instant previousRunAt) {

    public String string(String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    public int integer(String key, int fallback) {
        Object value = config.get(key);
        return value instanceof Number n ? n.intValue() : fallback;
    }

    public boolean flag(String key, boolean fallback) {
        Object value = config.get(key);
        return value instanceof Boolean b ? b : fallback;
    }
}
