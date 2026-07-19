package com.sami.app.common.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Enables Spring scheduling for the single poller in {@code JobRunner}.
 *
 * <p>This is the ONLY {@code @EnableScheduling} in the codebase, and the only
 * {@code @Scheduled} method is the poller. Modules never annotate their own
 * scheduled methods — they register a {@code JobHandler} bean and a job row, so
 * their work is listable, pausable, auditable and tenant-scoped.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulerConfig {

    /**
     * A small dedicated pool: the poller must not be starved by, or starve, any
     * other scheduled work, and long-running sweeps must not block each other.
     */
    @Bean
    public ThreadPoolTaskScheduler jobTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("sami-job-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        return scheduler;
    }
}
