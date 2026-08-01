package com.sami.app.purchasing;

import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.purchasing.domain.PurStatus;
import com.sami.app.purchasing.domain.PurType;
import com.sami.app.purchasing.domain.Purchase;
import com.sami.app.purchasing.dto.PurchaseDtos.PurchaseFilter;
import com.sami.app.purchasing.repository.PurchaseRepository;
import com.sami.app.purchasing.service.PurchaseReportingService;
import com.sami.app.supplier.domain.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseExportEncodingTest {

    @Mock PurchaseRepository purchases;
    @Mock TenantContext tenantContext;
    @InjectMocks PurchaseReportingService service;

    @Test
    void csvHasUtf8BomAndPreservesPersianContent() {
        when(tenantContext.requireTenantId()).thenReturn(41L);
        Purchase purchase = Purchase.builder().tenantId(41L).purchaseNumber("PUR-1")
                .type(PurType.builder().code("standard").name("خرید آزمایشی").build())
                .status(PurStatus.builder().code("draft").name("پیش‌نویس").build())
                .supplier(Supplier.builder().supplierCode("SUP-1")
                        .displayName("امیر جلیلی").build())
                .totalAmount(new BigDecimal("1250000")).build();
        when(purchases.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Sort.class))).thenReturn(List.of(purchase));

        byte[] csv = service.exportCsv(new PurchaseFilter(
                null, null, null, null, null, null, null, null, null));

        assertThat(csv).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        assertThat(new String(csv, 3, csv.length - 3, StandardCharsets.UTF_8))
                .contains("خرید آزمایشی", "امیر جلیلی", "1250000");
    }
}
