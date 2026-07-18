package com.sami.app.purchasing.service;

import com.sami.app.purchasing.PurchasingProperties;
import com.sami.app.purchasing.domain.PurType;
import com.sami.app.purchasing.repository.PurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Generates purchase numbers from configuration: the type's prefix, an
 * optional year segment, and a zero-padded DB sequence — e.g.
 * {@code PUR-2026-000001} or {@code PUR-MOB-000145}. The sequence plus the
 * unique constraint make duplicates impossible, even under concurrency.
 */
@Component
@RequiredArgsConstructor
public class PurchaseNumberGenerator {

    private final PurchaseRepository purchaseRepository;
    private final PurchasingProperties properties;

    public String next(PurType type) {
        long number = purchaseRepository.nextNumber();
        StringBuilder result = new StringBuilder(type.getNumberPrefix());
        if (properties.numberIncludeYear()) {
            result.append('-').append(LocalDate.now(ZoneOffset.UTC).getYear());
        }
        result.append('-')
                .append(String.format("%0" + properties.numberPadding() + "d", number));
        return result.toString();
    }
}
