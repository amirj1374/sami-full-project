package com.sami.app.marketsync;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;

public final class MarketProductCode {
    private MarketProductCode() {}
    public static String normalize(String raw) {
        if (raw == null) throw invalid();
        String digits = raw.trim().replace(" ", "").replace("-", "");
        if (!digits.matches("0?9\\d{9}")) throw invalid();
        return digits.startsWith("0") ? digits.substring(1) : digits;
    }
    private static ApiException invalid() { return new ApiException(ErrorCode.VALIDATION_FAILED, "SIM number must be 10 digits, optionally prefixed by one zero"); }
}
