package com.sami.app.accounting.publicapi;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ReceivablePostingPortTest {

    @Test
    void requiresScopedIdempotentInvoiceEvidence() {
        assertThatThrownBy(() -> new ReceivablePostingPort.SalesInvoiceReceivableCommand(
                1L, 2L, 3L, 4L, 5L, BigDecimal.ZERO, "IRR", Instant.now(), "invoice-5"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
