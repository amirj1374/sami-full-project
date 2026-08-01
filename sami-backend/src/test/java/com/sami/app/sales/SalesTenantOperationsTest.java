package com.sami.app.sales;

import com.sami.app.authz.domain.Role;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.crm.repository.CustomerRepository;
import com.sami.app.inventory.publicapi.InventoryStockOperations;
import com.sami.app.product.repository.ProductRepository;
import com.sami.app.sales.domain.LostSale;
import com.sami.app.sales.dto.SalesDtos.LostSaleRequest;
import com.sami.app.sales.event.SaleDomainEvent;
import com.sami.app.sales.repository.*;
import com.sami.app.sales.service.SalesService;
import com.sami.app.sales.spi.SalesRecommendationProvider;
import com.sami.app.security.SecurityUser;
import com.sami.app.user.domain.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesTenantOperationsTest {
    @Mock SaleRepository sales;
    @Mock SaleNumberRepository numbers;
    @Mock SaleReturnNumberRepository returnNumbers;
    @Mock SaleDiscountRepository discounts;
    @Mock SaleAuditRepository audits;
    @Mock SaleReturnRepository returns;
    @Mock LostSaleRepository lostSales;
    @Mock SaleAiRecommendationRepository recommendations;
    @Mock ProductRepository products;
    @Mock CustomerRepository customers;
    @Mock TenantContext tenantContext;
    @Mock InventoryStockOperations inventory;
    @Mock JdbcTemplate jdbc;
    @Mock ApplicationEventPublisher events;
    @Mock List<SalesRecommendationProvider> providers;
    @InjectMocks SalesService service;

    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    @Test
    void lostSaleUsesTrustedTenantForPersistenceAndEvent() {
        authenticate(41L);
        when(tenantContext.requireTenantId()).thenReturn(41L);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(8L), eq(7L), eq(41L), eq(41L))).thenReturn(1);
        when(lostSales.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.recordLostSale(new LostSaleRequest(7L, 8L, null, null,
                "price", "Customer chose a competitor", new BigDecimal("1250000"), null));

        ArgumentCaptor<LostSale> row = ArgumentCaptor.forClass(LostSale.class);
        verify(lostSales).save(row.capture());
        assertThat(row.getValue().getTenantId()).isEqualTo(41L);
        assertThat(row.getValue().getSellerId()).isEqualTo(91L);
        assertThat(row.getValue().getReasonCode()).isEqualTo("PRICE");

        ArgumentCaptor<SaleDomainEvent> event = ArgumentCaptor.forClass(SaleDomainEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().tenantId()).isEqualTo(41L);
        assertThat(event.getValue().action()).isEqualTo("LOST_SALE_RECORDED");
    }

    private void authenticate(Long tenantId) {
        User user = User.builder().tenantId(tenantId).email("sales@example.com")
                .passwordHash("hash").fullName("Sales User")
                .role(Role.builder().isPlatform(false).build()).build();
        SecurityUser principal = new SecurityUser(user);
        // User id is intentionally mocked through a spy because BaseEntity ids are persistence-owned.
        User persisted = spy(user);
        doReturn(91L).when(persisted).getId();
        principal = new SecurityUser(persisted);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
