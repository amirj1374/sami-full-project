package com.sami.app.organization.web;
import com.sami.app.common.api.ApiResponse;
import com.sami.app.organization.dto.OrganizationContextDtos.*;
import com.sami.app.organization.service.OrganizationScopeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/organization/context") @RequiredArgsConstructor
public class OrganizationContextController {
 private final OrganizationScopeService service;
 @GetMapping public ApiResponse<ContextResponse> current(){ return ApiResponse.ok(service.current()); }
 @PutMapping public ApiResponse<ContextResponse> select(@Valid @RequestBody SelectRequest request){ return ApiResponse.ok(service.select(request)); }
}
