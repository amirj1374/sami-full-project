package com.sami.app.marketsync;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;
class MarketSyncContractTest {
 @Test void schemaProtectsIdentitySourceAndIdempotency()throws Exception{String s=Files.readString(Path.of("src/main/resources/db/migration/V42__market_sync_auto_pricing.sql"));assertThat(s).contains("uq_market_product_code UNIQUE(tenant_id,normalized_product_code)").contains("source_id BIGINT NOT NULL").contains("uq_market_publication UNIQUE(tenant_id,market_product_id,channel_code)");}
 @Test void coreDoesNotContainRondParsingOrInventoryWrites()throws Exception{String s=Files.readString(Path.of("src/main/java/com/sami/app/marketsync/MarketSyncService.java"));assertThat(s).doesNotContain("moshtarekin123").doesNotContain("samistore.rond").doesNotContain("inventory_balances").contains("inventory.setMarketAvailability").contains("SOURCE_COLLISION").contains("SOLD_LOCKED");}
 @Test void rawCodesAreNotMatchedByDisplayName()throws Exception{String s=Files.readString(Path.of("src/main/java/com/sami/app/marketsync/MarketSyncService.java"));assertThat(s).contains("MarketProductCode.normalize").contains("normalized_product_code=?");}
 @Test void credentialsRemainEnvironmentReferencesAndEndpointsAreHttps()throws Exception{String s=Files.readString(Path.of("src/main/java/com/sami/app/marketsync/MarketSyncService.java"));String a=Files.readString(Path.of("src/main/java/com/sami/app/marketsync/StructuredJsonMarketSourceAdapter.java"));assertThat(s).contains("rejectEmbeddedSecrets").contains("validateEndpoint").doesNotContain("put(\"password\"");assertThat(a).contains("System.getenv(authEnv)").contains("https://").contains("response exceeds 10 MB");}
}
