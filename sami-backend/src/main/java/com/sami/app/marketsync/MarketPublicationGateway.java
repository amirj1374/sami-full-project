package com.sami.app.marketsync;

import java.math.BigDecimal;

public interface MarketPublicationGateway {
    String channel();
    PublicationResult apply(String productCode, BigDecimal price, boolean published, String externalId);
    record PublicationResult(boolean success, String externalId, String error) {}
}
