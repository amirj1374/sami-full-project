package com.sami.app.marketsync;

import com.sami.app.common.scheduler.spi.JobContext;
import com.sami.app.common.scheduler.spi.JobHandler;
import com.sami.app.common.scheduler.spi.JobResult;
import com.sami.app.common.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class MarketSyncJobHandler implements JobHandler {
    public static final String KEY="market-sync.source";
    private final MarketSyncService service; private final TenantContext tenants;
    @Override public String key(){return KEY;} @Override public String description(){return "Synchronize one configured market source";}
    @Override public JobResult execute(JobContext context){if(context.tenantId()==null)return JobResult.failed("Trusted tenant scope is required");Object source=context.config().get("sourceId");if(source==null)return JobResult.failed("sourceId parameter is required");
        return tenants.callAsTenant(context.tenantId(),()->{var result=service.sync(Long.valueOf(String.valueOf(source)),"SCHEDULED");return JobResult.ok("Market source synchronized",((Number)result.get("fetched_count")).intValue());});}
}
