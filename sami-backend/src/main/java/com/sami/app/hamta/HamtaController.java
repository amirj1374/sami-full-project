package com.sami.app.hamta;

import com.sami.app.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/hamta")
@RequiredArgsConstructor
@Validated
public class HamtaController {
    private final HamtaService service;

    public record SettingsRequest(boolean enforcementEnabled) {}
    public record CodeRequest(@NotBlank @Size(max=128) String activationCode) {}

    @GetMapping("/settings") @PreAuthorize("@authz.has('hamta:view')")
    public ApiResponse<Map<String,Object>> settings() { return ApiResponse.ok(service.settings()); }

    @PutMapping("/settings") @PreAuthorize("@authz.has('hamta:settings-update')")
    public ApiResponse<Map<String,Object>> update(@RequestBody SettingsRequest request) { return ApiResponse.ok(service.updateSettings(request.enforcementEnabled())); }

    @GetMapping("/serials/{imei}") @PreAuthorize("@authz.has('hamta:view')")
    public ApiResponse<Map<String,Object>> serial(@PathVariable String imei) { return ApiResponse.ok(service.byImei(imei, false)); }

    @GetMapping("/serials/{imei}/code") @PreAuthorize("@authz.has('hamta:view-code')")
    public ApiResponse<Map<String,Object>> code(@PathVariable String imei) { return ApiResponse.ok(service.byImei(imei, true)); }

    @PutMapping("/serials/{imei}/code") @PreAuthorize("@authz.has('hamta:edit')")
    public ApiResponse<Map<String,Object>> code(@PathVariable String imei, @Valid @RequestBody CodeRequest request) {
        return ApiResponse.ok(service.correctByImei(imei, request.activationCode()));
    }

    @GetMapping("/sales/{saleId}/invoice") @PreAuthorize("@authz.has('hamta:view-code')")
    public ApiResponse<List<Map<String,Object>>> invoice(@PathVariable Long saleId) { return ApiResponse.ok(service.invoice(saleId)); }

    @PostMapping("/sales/{saleId}/deliver") @PreAuthorize("@authz.has('hamta:deliver')")
    public ApiResponse<Map<String,Object>> deliver(@PathVariable Long saleId) { return ApiResponse.ok(service.deliver(saleId)); }

    @GetMapping("/report") @PreAuthorize("@authz.has('hamta:report')")
    public ApiResponse<List<Map<String,Object>>> report(@RequestParam(required=false) Boolean delivered,
                                                        @RequestParam(required=false) Long productId) {
        return ApiResponse.ok(service.report(delivered, productId));
    }
}
