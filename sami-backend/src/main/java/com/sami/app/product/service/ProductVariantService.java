package com.sami.app.product.service;

import com.sami.app.common.exception.*;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.organization.service.FoundationAuditService;
import com.sami.app.product.domain.ProductVariant;
import com.sami.app.product.dto.ProductVariantDtos.*;
import com.sami.app.product.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor
public class ProductVariantService {
    private final ProductVariantRepository variants; private final ProductRepository products;
    private final TenantContext tenants; private final FoundationAuditService audit;
    @Transactional(readOnly=true) public List<Response> list(Long productId){ requireProduct(productId); return variants.findAllByTenantIdAndProductIdOrderById(tenants.requireTenantId(),productId).stream().map(this::response).toList(); }
    @Transactional(readOnly=true) public Response get(Long id){ return response(find(id)); }
    @Transactional public Response create(Long productId, Request r){ long t=tenants.requireTenantId(); requireProduct(productId); if(variants.existsByTenantIdAndProductIdAndVariantCode(t,productId,r.variantCode())) throw new ApiException(ErrorCode.RESOURCE_CONFLICT,"Variant code already exists"); if(r.sku()!=null&&!r.sku().isBlank()&&variants.existsByTenantIdAndSku(t,r.sku())) throw new ApiException(ErrorCode.RESOURCE_CONFLICT,"Variant SKU already exists"); ProductVariant v=variants.saveAndFlush(ProductVariant.builder().tenantId(t).productId(productId).variantCode(r.variantCode()).name(r.name()).sku(blank(r.sku())).status(normalize(r.status())).build()); audit.record(t,null,null,"PRODUCT_VARIANT",v.getId(),"CREATED",null,Map.of("productId",productId)); return response(v); }
    @Transactional public Response update(Long id, Request r){ ProductVariant v=find(id); long t=tenants.requireTenantId(); if(r.sku()!=null&&!r.sku().isBlank()&&!Objects.equals(v.getSku(),r.sku())&&variants.existsByTenantIdAndSku(t,r.sku())) throw new ApiException(ErrorCode.RESOURCE_CONFLICT,"Variant SKU already exists"); v.setVariantCode(r.variantCode()); v.setName(r.name()); v.setSku(blank(r.sku())); v.setStatus(normalize(r.status())); audit.record(t,null,null,"PRODUCT_VARIANT",id,"UPDATED",null,Map.of("productId",v.getProductId())); return response(v); }
    private ProductVariant find(Long id){return variants.findByTenantIdAndId(tenants.requireTenantId(),id).orElseThrow(()->ResourceNotFoundException.of("Product variant",id));}
    private void requireProduct(Long id){if(!products.findByIdAndTenantId(id,tenants.requireTenantId()).isPresent()) throw ResourceNotFoundException.of("Product",id);}
    private Response response(ProductVariant v){return new Response(v.getId(),v.getProductId(),v.getVariantCode(),v.getName(),v.getSku(),v.getStatus(),v.getCreatedAt(),v.getUpdatedAt(),v.getVersion());}
    private String normalize(String s){if(!Set.of("ACTIVE","INACTIVE").contains(s)) throw new ApiException(ErrorCode.VALIDATION_FAILED,"Invalid variant status"); return s;}
    private String blank(String s){return s==null||s.isBlank()?null:s.trim();}
}
