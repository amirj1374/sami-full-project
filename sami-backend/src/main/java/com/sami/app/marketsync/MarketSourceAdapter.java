package com.sami.app.marketsync;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface MarketSourceAdapter {
    String key();
    FetchResult fetch(SourceConfig source);
    record SourceConfig(Long id, String code, String endpointUrl, Map<String,Object> config) {}
    record SourceItem(String rawIdentifier, BigDecimal sourcePrice, BigDecimal acquisitionCost) {}
    record FetchResult(List<SourceItem> items, List<String> warnings) {}
}
