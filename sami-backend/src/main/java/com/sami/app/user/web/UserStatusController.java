package com.sami.app.user.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.user.dto.UserStatusRequest;
import com.sami.app.user.dto.UserStatusResponse;
import com.sami.app.user.service.UserStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Configurable user statuses. Listing needs only {@code users:view} (the users
 * page shows status dropdowns); management requires {@code users:edit}.
 */
@RestController
@RequestMapping("/api/v1/user-statuses")
@RequiredArgsConstructor
@Tag(name = "User statuses", description = "Configurable user lifecycle statuses")
public class UserStatusController {

    private final UserStatusService statusService;

    @GetMapping
    @PreAuthorize("@authz.has('users:view')")
    @Operation(summary = "List all statuses in display order")
    public ApiResponse<List<UserStatusResponse>> list() {
        return ApiResponse.ok(statusService.list());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('users:edit')")
    @Operation(summary = "Create a status")
    public ApiResponse<UserStatusResponse> create(@Valid @RequestBody UserStatusRequest request) {
        return ApiResponse.ok(statusService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.has('users:edit')")
    @Operation(summary = "Update a status (system status codes are immutable)")
    public ApiResponse<UserStatusResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody UserStatusRequest request) {
        return ApiResponse.ok(statusService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.has('users:edit')")
    @Operation(summary = "Delete a status (blocked for system statuses and statuses in use)")
    public void delete(@PathVariable Long id) {
        statusService.delete(id);
    }
}
