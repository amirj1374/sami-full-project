package com.sami.app.files.provider;

import com.sami.app.common.scheduler.spi.JobContext;
import com.sami.app.common.scheduler.spi.JobHandler;
import com.sami.app.common.scheduler.spi.JobResult;
import com.sami.app.files.service.RetentionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Activates the file retention sweep, which existed as a service with no clock.
 *
 * <p>Lives in the files module, not the scheduler: the scheduler must not depend
 * on any module it runs work for.
 */
@Component
@RequiredArgsConstructor
public class RetentionJobHandler implements JobHandler {

    private final RetentionService retentionService;

    @Override
    public String key() {
        return "files-retention";
    }

    @Override
    public String description() {
        return "Applies expired file retention policies and reclaims stale upload sessions";
    }

    @Override
    public JobResult execute(JobContext context) {
        RetentionService.Summary summary = retentionService.applyExpired();
        int touched = summary.archived() + summary.deleted() + summary.notified();
        return JobResult.ok(
                "examined=%d archived=%d deleted=%d notified=%d skipped=%d reclaimedUploads=%d"
                        .formatted(summary.examined(), summary.archived(), summary.deleted(),
                                summary.notified(), summary.skipped(), summary.reclaimedUploads()),
                touched);
    }
}
