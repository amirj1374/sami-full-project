package com.sami.app.licensing.service;

/**
 * Result of comparing measured usage against the licensed ceiling.
 *
 * @param limitType the usage-limit-type code
 * @param limit     the ceiling (-1 = unlimited)
 * @param current   measured consumption
 * @param unlimited whether the plan grants an unlimited allowance
 * @param allowed   whether the caller may proceed
 */
public record UsageCheck(String limitType, long limit, long current, boolean unlimited, boolean allowed) {

    public long remaining() {
        return unlimited ? Long.MAX_VALUE : Math.max(0, limit - current);
    }
}
