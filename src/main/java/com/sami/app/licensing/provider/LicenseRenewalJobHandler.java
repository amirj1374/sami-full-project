package com.sami.app.licensing.provider;

import com.sami.app.common.scheduler.spi.JobContext;
import com.sami.app.common.scheduler.spi.JobHandler;
import com.sami.app.common.scheduler.spi.JobResult;
import com.sami.app.licensing.service.LicenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Activates automatic licence renewal, which had an endpoint but no clock.
 */
@Component
@RequiredArgsConstructor
public class LicenseRenewalJobHandler implements JobHandler {

    private final LicenseService licenseService;

    @Override
    public String key() {
        return "licensing-renewals";
    }

    @Override
    public String description() {
        return "Processes licences flagged for automatic renewal";
    }

    @Override
    public JobResult execute(JobContext context) {
        int renewed = licenseService.processAutoRenewals();
        return JobResult.ok("renewed=%d".formatted(renewed), renewed);
    }
}
