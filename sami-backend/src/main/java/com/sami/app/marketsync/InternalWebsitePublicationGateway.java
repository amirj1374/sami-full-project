package com.sami.app.marketsync;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;

/** Fails closed until a concrete website connector is configured. */
@Component
public class InternalWebsitePublicationGateway implements MarketPublicationGateway {
    @Override public String channel() { return "WEBSITE"; }
    @Override public PublicationResult apply(String code, BigDecimal price, boolean published, String externalId) {
        if (!published && externalId == null) {
            return new PublicationResult(true, null, null);
        }
        return new PublicationResult(false, externalId, "No website publication connector is configured");
    }
}
