package com.sami.app.purchasepayment.web;
import com.sami.app.common.api.ApiResponse;
import com.sami.app.purchasepayment.dto.PurchasePaymentDtos.*;
import com.sami.app.purchasepayment.service.PurchasePaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@RestController @RequestMapping("/api/v1/purchase-payment-requests") @RequiredArgsConstructor
public class PurchasePaymentController {
 private final PurchasePaymentService service;
 @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("@authz.has('purchase-payments:create')") public ApiResponse<Map<String,Object>> create(@Valid @RequestBody CreateRequest r){return ApiResponse.ok(service.create(r));}
 @GetMapping("/mine") @PreAuthorize("@authz.hasAny('purchase-payments:view-own','purchase-payments:view-all')") public ApiResponse<List<Map<String,Object>>> mine(){return ApiResponse.ok(service.mine());}
 @GetMapping @PreAuthorize("@authz.has('purchase-payments:view-all')") public ApiResponse<List<Map<String,Object>>> all(){return ApiResponse.ok(service.all());}
 @GetMapping("/{id}") @PreAuthorize("@authz.has('purchase-payments:view-all')") public ApiResponse<Map<String,Object>> get(@PathVariable Long id){return ApiResponse.ok(service.view(id,false));}
 @GetMapping("/mine/{id}") @PreAuthorize("@authz.hasAny('purchase-payments:view-own','purchase-payments:view-all')") public ApiResponse<Map<String,Object>> mine(@PathVariable Long id){return ApiResponse.ok(service.view(id,true));}
 @PostMapping("/{id}/decision") @PreAuthorize("@authz.has('purchase-payments:approve')") public ApiResponse<Map<String,Object>> decide(@PathVariable Long id,@Valid @RequestBody DecisionRequest r){return ApiResponse.ok(service.decide(id,r));}
 @PostMapping("/{id}/payments") @PreAuthorize("@authz.has('purchase-payments:process')") public ApiResponse<Map<String,Object>> pay(@PathVariable Long id,@Valid @RequestBody PaymentRequest r){return ApiResponse.ok(service.pay(id,r));}
 @PostMapping("/{id}/cancel") @PreAuthorize("@authz.has('purchase-payments:create')") public ApiResponse<Map<String,Object>> cancel(@PathVariable Long id){return ApiResponse.ok(service.cancel(id));}
 @PostMapping("/{id}/delay") @PreAuthorize("@authz.has('purchase-payments:process')") public ApiResponse<Map<String,Object>> delay(@PathVariable Long id,@Valid @RequestBody DelayRequest r){return ApiResponse.ok(service.delay(id,r));}
 @PutMapping("/limits") @PreAuthorize("@authz.has('purchase-payments:manage-limits')") public ApiResponse<Map<String,Object>> limit(@Valid @RequestBody LimitRequest r){return ApiResponse.ok(service.setLimit(r));}
 @GetMapping("/limits/capacity") @PreAuthorize("@authz.hasAny('purchase-payments:create','purchase-payments:process')") public ApiResponse<Map<String,Object>> capacity(@RequestParam Long treasuryAccountId,@RequestParam LocalDate date,@RequestParam String method){return ApiResponse.ok(service.capacity(treasuryAccountId,date,method));}
}
