package com.sami.app.common.scheduler;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param enabled       master switch; false leaves jobs configurable but never runs them
 * @param pollSeconds   how often the runner looks for due work
 * @param batchSize     maximum jobs claimed per poll, bounding a burst after downtime
 * @param startupDelaySeconds grace period before the first poll, so the application
 *                            finishes starting before any sweep begins
 */
@ConfigurationProperties(prefix = "app.scheduler")
public record SchedulerProperties(boolean enabled,
                                  int pollSeconds,
                                  int batchSize,
                                  int startupDelaySeconds) {

    public SchedulerProperties {
        pollSeconds = pollSeconds <= 0 ? 30 : pollSeconds;
        batchSize = batchSize <= 0 ? 20 : batchSize;
        startupDelaySeconds = startupDelaySeconds <= 0 ? 60 : startupDelaySeconds;
    }
}
