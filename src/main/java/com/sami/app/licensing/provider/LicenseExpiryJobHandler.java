package com.sami.app.licensing.provider;

import com.sami.app.common.scheduler.spi.JobContext;
import com.sami.app.common.scheduler.spi.JobHandler;
import com.sami.app.common.scheduler.spi.JobResult;
import com.sami.app.licensing.service.LicenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Activates the licence expiry sweep, which had an endpoint but no clock.
 */
@Component
@RequiredArgsConstructor
public class LicenseExpiryJobHandler implements JobHandler {

    private final LicenseService licenseService;

    @Override
    public String key() {
        return "licensing-expiry";
    }

    @Override
    public String description() {
        return "Moves lapsed licences to the expired state";
    }

    @Override
    public JobResult execute(JobContext context) {
        int expired = licenseService.expireLapsed();
        return JobResult.ok("expired=%d".formatted(expired), expired);
    }
}
