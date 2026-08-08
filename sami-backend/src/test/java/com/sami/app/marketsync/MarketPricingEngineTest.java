package com.sami.app.marketsync;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketPricingEngineTest {
    @Test void normalizesOnlyOptionalLeadingZero(){assertThat(MarketProductCode.normalize("09121573758")).isEqualTo("9121573758");assertThat(MarketProductCode.normalize("9121573758")).isEqualTo("9121573758");assertThatThrownBy(()->MarketProductCode.normalize("009121573758")).isInstanceOf(RuntimeException.class);}
    @Test void appliesPercentageFixedProfitAndNearestRounding(){var p=new MarketPricingEngine.Profile(new BigDecimal("10"),new BigDecimal("500"),new BigDecimal("2000"),new BigDecimal("1000"),"NEAREST",null,null,1);var r=MarketPricingEngine.calculate(new BigDecimal("10000"),new BigDecimal("10000"),p);assertThat(r.finalPrice()).isEqualByComparingTo("12000");assertThat(r.publishable()).isTrue();}
    @Test void supportsDecreaseAndDirectionalRounding(){var p=new MarketPricingEngine.Profile(new BigDecimal("-10"),new BigDecimal("-1"),null,new BigDecimal("1000"),"DOWN",null,null,1);assertThat(MarketPricingEngine.calculate(new BigDecimal("10500"),null,p).finalPrice()).isEqualByComparingTo("9000");}
    @Test void refusesMinimumProfitVerificationWithoutCost(){var p=new MarketPricingEngine.Profile(BigDecimal.ZERO,BigDecimal.ZERO,new BigDecimal("1000"),BigDecimal.ONE,"NEAREST",null,null,1);var r=MarketPricingEngine.calculate(new BigDecimal("10000"),null,p);assertThat(r.status()).isEqualTo("COST_UNVERIFIED");assertThat(r.publishable()).isFalse();}
    @Test void enforcesPublicationRange(){var p=new MarketPricingEngine.Profile(BigDecimal.ZERO,BigDecimal.ZERO,null,BigDecimal.ONE,"NEAREST",new BigDecimal("11000"),new BigDecimal("20000"),1);assertThat(MarketPricingEngine.calculate(new BigDecimal("10000"),null,p).publicationReasons()).containsExactly("BELOW_MIN_PRICE");}
}
