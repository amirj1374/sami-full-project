package com.sami.app.user.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.common.api.PageResponse;
import com.sami.app.common.storage.FileStorage;
import com.sami.app.security.SecurityUser;
import com.sami.app.user.dto.BulkResultResponse;
import com.sami.app.user.dto.BulkStatusRequest;
import com.sami.app.user.dto.BulkUserRequest;
import com.sami.app.user.dto.CreateUserRequest;
import com.sami.app.user.dto.CurrentUserResponse;
import com.sami.app.user.dto.UpdateUserRequest;
import com.sami.app.user.dto.UpdateUserStatusRequest;
import com.sami.app.user.dto.UserAuditLogResponse;
import com.sami.app.user.dto.UserDetailResponse;
import com.sami.app.user.dto.UserFilter;
import com.sami.app.user.dto.UserResponse;
import com.sami.app.user.service.UserAuditService;
import com.sami.app.user.service.UserAvatarService;
import com.sami.app.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * User management endpoints: self-service profile plus permission-guarded
 * administration of the full user lifecycle.
 *
 * <p>{@code /me} only requires authentication; every admin endpoint declares its
 * required permission code via the {@code @authz} bean. DELETE is a soft delete;
 * physical removal is the separately permissioned {@code /permanent} endpoint.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile and lifecycle administration")
public class UserController {

    private final UserService userService;
    private final UserAvatarService avatarService;
    private final UserAuditService auditService;

    // ------------------------------------------------------------------ self

    @GetMapping("/me")
    @Operation(summary = "Get the currently authenticated user's profile and permissions")
    public ApiResponse<CurrentUserResponse> me(@AuthenticationPrincipal SecurityUser principal) {
        return ApiResponse.ok(userService.me(principal.getId()));
    }

    @GetMapping("/me/avatar")
    @Operation(summary = "Get the current user's profile image")
    public ResponseEntity<byte[]> myAvatar(@AuthenticationPrincipal SecurityUser principal) {
        return avatarResponse(principal.getId());
    }

    // ----------------------------------------------------------------- reads

    @GetMapping
    @PreAuthorize("@authz.has('users:view')")
    @Operation(summary = "List users (paginated, sortable; global search and field filters; "
            + "archived/deleted hidden unless requested)")
    public ApiResponse<PageResponse<UserResponse>> list(
            UserFilter filter,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(userService.list(filter, pageable)));
    }

    @GetMapping("/export")
    @PreAuthorize("@authz.has('users:export')")
    @Operation(summary = "Export users matching the current filters as CSV")
    public ResponseEntity<String> export(UserFilter filter) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header("Content-Disposition", "attachment; filename=\"users.csv\"")
                .body(userService.exportCsv(filter));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.has('users:view')")
    @Operation(summary = "Get a user with their full profile")
    public ApiResponse<UserDetailResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(userService.getDetail(id));
    }

    @GetMapping("/{id}/audit")
    @PreAuthorize("@authz.has('users:view')")
    @Operation(summary = "Audit trail of a user (who did what, old/new values)")
    public ApiResponse<PageResponse<UserAuditLogResponse>> audit(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(auditService.listForUser(id, pageable)));
    }

    // ----------------------------------------------------------------- write

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('users:create')")
    @Operation(summary = "Create a user with role, status and profile")
    public ApiResponse<UserDetailResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.ok(userService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.has('users:edit')")
    @Operation(summary = "Update a user's name, role, status and profile")
    public ApiResponse<UserDetailResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody UpdateUserRequest request,
                                                  @AuthenticationPrincipal SecurityUser principal) {
        return ApiResponse.ok(userService.update(id, request, principal.getId()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@authz.has('users:edit')")
    @Operation(summary = "Transition a user to another status (activate/deactivate/suspend/custom)")
    public ApiResponse<UserResponse> changeStatus(@PathVariable Long id,
                                                  @Valid @RequestBody UpdateUserStatusRequest request,
                                                  @AuthenticationPrincipal SecurityUser principal) {
        return ApiResponse.ok(userService.changeStatus(id, request.statusId(), principal.getId()));
    }

    // ------------------------------------------------------------- lifecycle

    @PostMapping("/{id}/archive")
    @PreAuthorize("@authz.has('users:archive')")
    @Operation(summary = "Archive a user (hidden from default listings)")
    public ApiResponse<UserResponse> archive(@PathVariable Long id,
                                             @AuthenticationPrincipal SecurityUser principal) {
        return ApiResponse.ok(userService.archive(id, principal.getId()));
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("@authz.has('users:restore')")
    @Operation(summary = "Restore an archived or soft-deleted user")
    public ApiResponse<UserResponse> restore(@PathVariable Long id,
                                             @AuthenticationPrincipal SecurityUser principal) {
        return ApiResponse.ok(userService.restore(id, principal.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.has('users:delete')")
    @Operation(summary = "Soft-delete a user (record and references preserved)")
    public ApiResponse<UserResponse> softDelete(@PathVariable Long id,
                                                @AuthenticationPrincipal SecurityUser principal) {
        return ApiResponse.ok(userService.softDelete(id, principal.getId()));
    }

    @DeleteMapping("/{id}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.has('users:purge')")
    @Operation(summary = "Permanently remove a soft-deleted user (audit trail survives)")
    public void purge(@PathVariable Long id, @AuthenticationPrincipal SecurityUser principal) {
        userService.purge(id, principal.getId());
    }

    // ------------------------------------------------------------------ bulk

    @PostMapping("/bulk/status")
    @PreAuthorize("@authz.has('users:edit')")
    @Operation(summary = "Bulk status transition (activate / deactivate / any status)")
    public ApiResponse<BulkResultResponse> bulkStatus(@Valid @RequestBody BulkStatusRequest request,
                                                      @AuthenticationPrincipal SecurityUser principal) {
        return ApiResponse.ok(userService.bulkChangeStatus(
                request.ids(), request.statusId(), principal.getId()));
    }

    @PostMapping("/bulk/archive")
    @PreAuthorize("@authz.has('users:archive')")
    @Operation(summary = "Bulk archive")
    public ApiResponse<BulkResultResponse> bulkArchive(@Valid @RequestBody BulkUserRequest request,
                                                       @AuthenticationPrincipal SecurityUser principal) {
        return ApiResponse.ok(userService.bulkArchive(request.ids(), principal.getId()));
    }

    @PostMapping("/bulk/restore")
    @PreAuthorize("@authz.has('users:restore')")
    @Operation(summary = "Bulk restore")
    public ApiResponse<BulkResultResponse> bulkRestore(@Valid @RequestBody BulkUserRequest request,
                                                       @AuthenticationPrincipal SecurityUser principal) {
        return ApiResponse.ok(userService.bulkRestore(request.ids(), principal.getId()));
    }

    // ---------------------------------------------------------------- avatar

    @PutMapping(value = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authz.has('users:edit')")
    @Operation(summary = "Upload or replace a user's profile image")
    public ApiResponse<Void> uploadAvatar(@PathVariable Long id,
                                          @RequestParam("file") MultipartFile file) {
        avatarService.upload(id, file);
        return ApiResponse.ok();
    }

    @GetMapping("/{id}/avatar")
    @PreAuthorize("@authz.has('users:view')")
    @Operation(summary = "Get a user's profile image")
    public ResponseEntity<byte[]> avatar(@PathVariable Long id) {
        return avatarResponse(id);
    }

    @DeleteMapping("/{id}/avatar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.has('users:edit')")
    @Operation(summary = "Remove a user's profile image")
    public void deleteAvatar(@PathVariable Long id) {
        avatarService.remove(id);
    }

    private ResponseEntity<byte[]> avatarResponse(Long userId) {
        return avatarService.load(userId)
                .map(file -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(file.contentType()))
                        .body(file.content()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
