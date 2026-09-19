package com.sami.app.product;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.organization.service.FoundationAuditService;
import com.sami.app.product.domain.Product;
import com.sami.app.product.domain.ProductVariant;
import com.sami.app.product.dto.ProductVariantDtos.Request;
import com.sami.app.product.repository.ProductRepository;
import com.sami.app.product.repository.ProductVariantRepository;
import com.sami.app.product.service.ProductVariantService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductVariantServiceTest {
    @Mock ProductVariantRepository variants; @Mock ProductRepository products; @Mock TenantContext tenants; @Mock FoundationAuditService audit;
    ProductVariantService service;
    @BeforeEach void setUp(){ service=new ProductVariantService(variants,products,tenants,audit); when(tenants.requireTenantId()).thenReturn(7L); }
    @Test void productMayHaveZeroVariants(){ when(products.findByIdAndTenantId(10L,7L)).thenReturn(Optional.of(Product.builder().tenantId(7L).build())); when(variants.findAllByTenantIdAndProductIdOrderById(7L,10L)).thenReturn(List.of()); assertTrue(service.list(10L).isEmpty()); }
    @Test void missingProductIsRejected(){ when(products.findByIdAndTenantId(10L,7L)).thenReturn(Optional.empty()); assertThrows(RuntimeException.class,()->service.list(10L)); verifyNoInteractions(variants); }
    @Test void duplicateVariantSkuIsRejected(){ when(products.findByIdAndTenantId(10L,7L)).thenReturn(Optional.of(Product.builder().tenantId(7L).build())); when(variants.existsByTenantIdAndProductIdAndVariantCode(7L,10L,"RED")).thenReturn(false); when(variants.existsByTenantIdAndSku(7L,"SKU-RED")).thenReturn(true); ApiException ex=assertThrows(ApiException.class,()->service.create(10L,new Request("RED","Red","SKU-RED","ACTIVE"))); assertEquals(ErrorCode.RESOURCE_CONFLICT,ex.getErrorCode()); verify(variants,never()).saveAndFlush(any()); }
    @Test void multipleVariantsAreAllowedWithDistinctCodes(){ when(products.findByIdAndTenantId(10L,7L)).thenReturn(Optional.of(Product.builder().tenantId(7L).build())); when(variants.findAllByTenantIdAndProductIdOrderById(7L,10L)).thenReturn(List.of(ProductVariant.builder().productId(10L).variantCode("RED").build(),ProductVariant.builder().productId(10L).variantCode("BLUE").build())); assertEquals(2,service.list(10L).size()); }
}
