package com.sami.app.purchasing;

import com.sami.app.purchasing.service.PurchaseReceivingService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PurchaseImeiValidationTest {
    @Test void acceptsValidImei() {
        assertTrue(PurchaseReceivingService.isValidImei("490154203237518"));
    }

    @Test void rejectsWrongLengthCharactersAndChecksum() {
        assertFalse(PurchaseReceivingService.isValidImei("49015420323751"));
        assertFalse(PurchaseReceivingService.isValidImei("49015420323751x"));
        assertFalse(PurchaseReceivingService.isValidImei("490154203237519"));
    }
}
