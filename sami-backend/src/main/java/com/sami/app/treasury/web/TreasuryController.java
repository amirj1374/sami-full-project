package com.sami.app.treasury.web;

import com.sami.app.common.api.*;
import com.sami.app.treasury.dto.TreasuryDtos.*;
import com.sami.app.treasury.service.TreasuryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** Operational treasury APIs. The authenticated tenant context is the only scope authority. */
@RestController @RequestMapping("/api/v1/treasury") @RequiredArgsConstructor @Tag(name="Treasury")
public class TreasuryController {
 private final TreasuryService service;
 @GetMapping("/dashboard") @PreAuthorize("@authz.has('treasury:view')") public ApiResponse<DashboardResponse> dashboard(){return ApiResponse.ok(service.dashboard());}
 @GetMapping("/accounts") @PreAuthorize("@authz.has('treasury:view')") public ApiResponse<List<AccountResponse>> accounts(){return ApiResponse.ok(service.accounts());}
 @PostMapping("/accounts") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("@authz.has('treasury:manage')") public ApiResponse<AccountResponse> createAccount(@Valid @RequestBody AccountRequest r){return ApiResponse.ok(service.createAccount(r));}
 @PutMapping("/accounts/{id}") @PreAuthorize("@authz.has('treasury:manage')") public ApiResponse<AccountResponse> updateAccount(@PathVariable Long id,@Valid @RequestBody AccountRequest r){return ApiResponse.ok(service.updateAccount(id,r));}
 @GetMapping("/account-types") @PreAuthorize("@authz.has('treasury:view')") public ApiResponse<List<AccountTypeResponse>> accountTypes(){return ApiResponse.ok(service.accountTypes());}
 @GetMapping("/transaction-types") @PreAuthorize("@authz.has('treasury:view')") public ApiResponse<List<TransactionTypeResponse>> transactionTypes(){return ApiResponse.ok(service.transactionTypes());}
 @GetMapping("/transaction-statuses") @PreAuthorize("@authz.has('treasury:view')") public ApiResponse<List<TransactionStatusResponse>> transactionStatuses(){return ApiResponse.ok(service.transactionStatuses());}
 @GetMapping("/categories") @PreAuthorize("@authz.has('treasury:view')") public ApiResponse<List<CategoryResponse>> categories(){return ApiResponse.ok(service.categories());}
 @GetMapping("/transactions") @PreAuthorize("@authz.has('treasury:view')") public ApiResponse<PageResponse<TransactionResponse>> transactions(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ApiResponse.ok(service.transactions(page,size));}
 @PostMapping("/transactions") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("@authz.has('treasury:create')") public ApiResponse<TransactionResponse> createTransaction(@Valid @RequestBody TransactionRequest r){return ApiResponse.ok(service.createTransaction(r));}
 @PutMapping("/transactions/{id}") @PreAuthorize("@authz.has('treasury:edit')") public ApiResponse<TransactionResponse> updateTransaction(@PathVariable Long id,@Valid @RequestBody TransactionRequest r){return ApiResponse.ok(service.updateTransaction(id,r));}
 @PostMapping("/transactions/{id}/submit") @PreAuthorize("@authz.has('treasury:create')") public ApiResponse<TransactionResponse> submit(@PathVariable Long id){return ApiResponse.ok(service.submit(id));}
 @PostMapping("/transactions/{id}/approve") @PreAuthorize("@authz.has('treasury:approve')") public ApiResponse<TransactionResponse> approve(@PathVariable Long id){return ApiResponse.ok(service.approve(id));}
 @PostMapping("/transactions/{id}/complete") @PreAuthorize("@authz.has('treasury:complete')") public ApiResponse<TransactionResponse> complete(@PathVariable Long id){return ApiResponse.ok(service.complete(id));}
 @PostMapping("/transactions/{id}/reject") @PreAuthorize("@authz.has('treasury:approve')") public ApiResponse<TransactionResponse> reject(@PathVariable Long id){return ApiResponse.ok(service.reject(id));}
 @PostMapping("/transactions/{id}/cancel") @PreAuthorize("@authz.has('treasury:cancel')") public ApiResponse<TransactionResponse> cancel(@PathVariable Long id){return ApiResponse.ok(service.cancel(id));}
 @GetMapping("/accounts/{id}/movements") @PreAuthorize("@authz.has('treasury:view')") public ApiResponse<PageResponse<MovementResponse>> movements(@PathVariable Long id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ApiResponse.ok(service.movements(id,page,size));}
 @GetMapping("/cheque-statuses") @PreAuthorize("@authz.has('treasury:view')") public ApiResponse<List<ChequeStatusResponse>> chequeStatuses(){return ApiResponse.ok(service.chequeStatuses());}
 @GetMapping("/cheques") @PreAuthorize("@authz.has('treasury:view')") public ApiResponse<PageResponse<ChequeResponse>> cheques(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ApiResponse.ok(service.cheques(page,size));}
 @PostMapping("/cheques") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("@authz.has('treasury:manage-cheques')") public ApiResponse<ChequeResponse> createCheque(@Valid @RequestBody ChequeRequest r){return ApiResponse.ok(service.createCheque(r));}
 @PutMapping("/cheques/{id}") @PreAuthorize("@authz.has('treasury:manage-cheques')") public ApiResponse<ChequeResponse> updateCheque(@PathVariable Long id,@Valid @RequestBody ChequeRequest r){return ApiResponse.ok(service.updateCheque(id,r));}
 @PostMapping("/cheques/{id}/status") @PreAuthorize("@authz.has('treasury:manage-cheques')") public ApiResponse<ChequeResponse> chequeStatus(@PathVariable Long id,@Valid @RequestBody ChequeStatusUpdate r){return ApiResponse.ok(service.updateChequeStatus(id,r));}
 @PostMapping("/daily-closings") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("@authz.has('treasury:close')") public ApiResponse<DailyClosingResponse> close(@Valid @RequestBody DailyClosingRequest r){return ApiResponse.ok(service.close(r));}
}
