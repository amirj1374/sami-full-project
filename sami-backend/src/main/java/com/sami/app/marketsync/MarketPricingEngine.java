package com.sami.app.marketsync;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class MarketPricingEngine {
    private MarketPricingEngine() {}
    public record Profile(BigDecimal percent, BigDecimal fixed, BigDecimal minimumProfit,
                          BigDecimal roundingStep, String roundingMode,
                          BigDecimal minimumPublication, BigDecimal maximumPublication, long version) {}
    public record Result(BigDecimal finalPrice, String status, List<String> publicationReasons) {
        public boolean publishable() { return publicationReasons.isEmpty() && "CALCULATED".equals(status); }
    }
    public static Result calculate(BigDecimal sourcePrice, BigDecimal acquisitionCost, Profile profile) {
        if (sourcePrice == null || sourcePrice.signum() <= 0) return new Result(null, "INVALID_PRICE", List.of("INVALID_PRICE"));
        BigDecimal value = sourcePrice.setScale(2, RoundingMode.HALF_UP);
        value = value.add(value.multiply(zero(profile.percent())).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        value = value.add(zero(profile.fixed()));
        String status = "CALCULATED";
        if (profile.minimumProfit() != null) {
            if (acquisitionCost == null) status = "COST_UNVERIFIED";
            else value = value.max(acquisitionCost.add(profile.minimumProfit()));
        }
        BigDecimal step = profile.roundingStep() == null ? BigDecimal.ONE : profile.roundingStep();
        RoundingMode mode = switch (profile.roundingMode() == null ? "NEAREST" : profile.roundingMode()) {
            case "UP" -> RoundingMode.CEILING; case "DOWN" -> RoundingMode.FLOOR; default -> RoundingMode.HALF_UP;
        };
        value = value.divide(step, 0, mode).multiply(step).setScale(2, RoundingMode.HALF_UP);
        List<String> reasons = new ArrayList<>();
        if (!"CALCULATED".equals(status)) reasons.add(status);
        if (profile.minimumPublication() != null && value.compareTo(profile.minimumPublication()) < 0) reasons.add("BELOW_MIN_PRICE");
        if (profile.maximumPublication() != null && value.compareTo(profile.maximumPublication()) > 0) reasons.add("ABOVE_MAX_PRICE");
        return new Result(value, status, List.copyOf(reasons));
    }
    private static BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
