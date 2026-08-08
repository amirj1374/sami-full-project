package com.sami.app.marketsync;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class MarketSourceAdapterRegistry {
    private final List<MarketSourceAdapter> adapters;
    public MarketSourceAdapterRegistry(List<MarketSourceAdapter> adapters) { this.adapters = adapters; }
    public MarketSourceAdapter require(String key) { return adapters.stream().filter(a -> a.key().equals(key)).findFirst()
            .orElseThrow(() -> new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "No authorized source adapter is configured for provider " + key)); }
}
