package com.sami.app.user.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.user.dto.ProfileFieldDefinitionRequest;
import com.sami.app.user.dto.ProfileFieldDefinitionResponse;
import com.sami.app.user.service.ProfileFieldService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Runtime-configurable custom profile fields. Reading the active definitions
 * needs {@code users:view} (the user form renders them); management requires
 * {@code users:edit}.
 */
@RestController
@RequestMapping("/api/v1/profile-fields")
@RequiredArgsConstructor
@Tag(name = "Profile fields", description = "Custom user profile field definitions")
public class ProfileFieldController {

    private final ProfileFieldService profileFieldService;

    @GetMapping
    @PreAuthorize("@authz.has('users:view')")
    @Operation(summary = "List field definitions (activeOnly=true for form rendering)")
    public ApiResponse<List<ProfileFieldDefinitionResponse>> list(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ApiResponse.ok(activeOnly
                ? profileFieldService.listActive()
                : profileFieldService.listAll());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('users:edit')")
    @Operation(summary = "Create a custom profile field")
    public ApiResponse<ProfileFieldDefinitionResponse> create(
            @Valid @RequestBody ProfileFieldDefinitionRequest request) {
        return ApiResponse.ok(profileFieldService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.has('users:edit')")
    @Operation(summary = "Update a field's label, rules or visibility (key and type immutable)")
    public ApiResponse<ProfileFieldDefinitionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProfileFieldDefinitionRequest request) {
        return ApiResponse.ok(profileFieldService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.has('users:edit')")
    @Operation(summary = "Delete a field definition (stored values remain in profiles)")
    public void delete(@PathVariable Long id) {
        profileFieldService.delete(id);
    }
}
