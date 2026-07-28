package com.sami.app.metadata.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.metadata.dto.MetadataDtos.BindRequest;
import com.sami.app.metadata.dto.MetadataDtos.DraftRequest;
import com.sami.app.metadata.dto.MetadataDtos.EntityResponse;
import com.sami.app.metadata.dto.MetadataDtos.FieldRequest;
import com.sami.app.metadata.dto.MetadataDtos.FieldResponse;
import com.sami.app.metadata.dto.MetadataDtos.FieldTypeResponse;
import com.sami.app.metadata.dto.MetadataDtos.FormRequest;
import com.sami.app.metadata.dto.MetadataDtos.FormResponse;
import com.sami.app.metadata.dto.MetadataDtos.FormVersionResponse;
import com.sami.app.metadata.dto.MetadataDtos.LayoutRequest;
import com.sami.app.metadata.dto.MetadataDtos.LayoutResponse;
import com.sami.app.metadata.dto.MetadataDtos.ValuesRequest;
import com.sami.app.metadata.service.CustomFieldService;
import com.sami.app.metadata.service.CustomFieldValueService;
import com.sami.app.metadata.service.FormService;
import com.sami.app.metadata.spi.FieldTypeHandler;
import com.sami.app.metadata.spi.FieldTypeRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Custom fields, dynamic forms, versions and layouts. Business modules call the
 * value endpoints (or the services) instead of growing their own metadata engine.
 */
@RestController
@RequestMapping("/api/v1/metadata")
@RequiredArgsConstructor
@Tag(name = "Metadata", description = "Custom fields, dynamic forms, versioning and layouts")
public class MetadataController {

    private final CustomFieldService fieldService;
    private final CustomFieldValueService valueService;
    private final FormService formService;
    private final FieldTypeRegistry handlerRegistry;

    // ---- Catalogue ----------------------------------------------------------

    @GetMapping("/entities")
    @PreAuthorize("@authz.has('metadata:view')")
    @Operation(summary = "Entities registered as extensible")
    public ApiResponse<List<EntityResponse>> entities() {
        return ApiResponse.ok(fieldService.entities().stream().map(EntityResponse::from).toList());
    }

    @GetMapping("/field-types")
    @PreAuthorize("@authz.has('metadata:view')")
    @Operation(summary = "Field-type catalogue and registered handler plugins")
    public ApiResponse<Map<String, Object>> fieldTypes() {
        return ApiResponse.ok(Map.of(
                "types", fieldService.fieldTypes().stream().map(FieldTypeResponse::from).toList(),
                "handlers", handlerRegistry.all().stream().map(FieldTypeHandler::key).toList()));
    }

    // ---- Fields -------------------------------------------------------------

    @GetMapping("/fields")
    @PreAuthorize("@authz.has('metadata:view')")
    @Operation(summary = "List custom fields (optionally for one entity)")
    public ApiResponse<List<FieldResponse>> fields(@RequestParam(required = false) String moduleCode,
                                                   @RequestParam(required = false) String entityCode) {
        var fields = (moduleCode == null || entityCode == null)
                ? fieldService.list()
                : fieldService.forEntity(moduleCode, entityCode);
        return ApiResponse.ok(fields.stream().map(FieldResponse::from).toList());
    }

    @PostMapping("/fields")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('metadata:create')")
    @Operation(summary = "Create a custom field")
    public ApiResponse<FieldResponse> createField(@Valid @RequestBody FieldRequest r) {
        return ApiResponse.ok(FieldResponse.from(fieldService.create(
                r.moduleCode(), r.entityCode(), r.code(), r.label(), r.helpText(), r.fieldType(),
                r.required(), r.defaultValue(), r.minValue(), r.maxValue(), r.minLength(), r.maxLength(),
                r.pattern(), r.options(), r.searchable(), r.sortable(), r.reportable(), r.localized(),
                r.viewPermission(), r.editPermission(), r.qualityRuleCode(), r.displayOrder())));
    }

    @PatchMapping("/fields/{id}/active")
    @PreAuthorize("@authz.has('metadata:edit')")
    @Operation(summary = "Enable or disable a custom field")
    public ApiResponse<FieldResponse> setFieldActive(@PathVariable Long id, @RequestParam boolean active) {
        return ApiResponse.ok(FieldResponse.from(fieldService.setActive(id, active)));
    }

    @DeleteMapping("/fields/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.has('metadata:delete')")
    @Operation(summary = "Delete a custom field and its stored values")
    public void deleteField(@PathVariable Long id) {
        fieldService.delete(id);
    }

    // ---- Values -------------------------------------------------------------

    @GetMapping("/values")
    @PreAuthorize("@authz.has('metadata:view')")
    @Operation(summary = "Custom field values for one record")
    public ApiResponse<Map<String, Object>> values(@RequestParam String moduleCode,
                                                   @RequestParam String entityCode,
                                                   @RequestParam Long recordId) {
        return ApiResponse.ok(valueService.getValues(moduleCode, entityCode, recordId));
    }

    @PostMapping("/values")
    @PreAuthorize("@authz.has('metadata:write-values')")
    @Operation(summary = "Validate and store custom field values for a record")
    public ApiResponse<Map<String, Object>> setValues(@Valid @RequestBody ValuesRequest r) {
        return ApiResponse.ok(valueService.setValues(r.moduleCode(), r.entityCode(), r.recordId(), r.values()));
    }

    @GetMapping("/values/search")
    @PreAuthorize("@authz.has('metadata:view')")
    @Operation(summary = "Find record ids by a custom field value (search/filter on extensions)")
    public ApiResponse<List<Long>> search(@RequestParam String moduleCode,
                                          @RequestParam String entityCode,
                                          @RequestParam String fieldCode,
                                          @RequestParam String value) {
        return ApiResponse.ok(valueService.searchByText(moduleCode, entityCode, fieldCode, value));
    }

    // ---- Forms, versions, layouts -------------------------------------------

    @GetMapping("/forms")
    @PreAuthorize("@authz.has('metadata:view')")
    @Operation(summary = "List dynamic forms")
    public ApiResponse<List<FormResponse>> forms() {
        return ApiResponse.ok(formService.list().stream().map(FormResponse::from).toList());
    }

    @PostMapping("/forms")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('metadata:manage-forms')")
    @Operation(summary = "Create a dynamic form")
    public ApiResponse<FormResponse> createForm(@Valid @RequestBody FormRequest r) {
        return ApiResponse.ok(FormResponse.from(
                formService.create(r.moduleCode(), r.entityCode(), r.code(), r.name(), r.description())));
    }

    @PostMapping("/forms/{formId}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('metadata:manage-forms')")
    @Operation(summary = "Open a new draft version")
    public ApiResponse<FormVersionResponse> createDraft(@PathVariable Long formId,
                                                        @RequestBody(required = false) DraftRequest r) {
        return ApiResponse.ok(FormVersionResponse.from(formService.createDraft(formId,
                r == null ? null : r.schema(), r == null ? null : r.changeNote())));
    }

    @PostMapping("/form-versions/{versionId}/publish")
    @PreAuthorize("@authz.has('metadata:publish')")
    @Operation(summary = "Publish a draft (archives the previous published version)")
    public ApiResponse<FormVersionResponse> publish(@PathVariable Long versionId) {
        return ApiResponse.ok(FormVersionResponse.from(formService.publish(versionId)));
    }

    @GetMapping("/forms/{formId}/versions")
    @PreAuthorize("@authz.has('metadata:view')")
    @Operation(summary = "Version history of a form")
    public ApiResponse<List<FormVersionResponse>> versions(@PathVariable Long formId) {
        return ApiResponse.ok(formService.versions(formId).stream().map(FormVersionResponse::from).toList());
    }

    @GetMapping("/forms/{formCode}/published")
    @PreAuthorize("@authz.has('metadata:view')")
    @Operation(summary = "Currently published version of a form")
    public ApiResponse<FormVersionResponse> published(@PathVariable String formCode) {
        return ApiResponse.ok(FormVersionResponse.from(formService.publishedVersion(formCode)));
    }

    @PostMapping("/form-versions/{versionId}/layouts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('metadata:manage-layouts')")
    @Operation(summary = "Add a layout variant (role / company / branch / device / stage)")
    public ApiResponse<LayoutResponse> addLayout(@PathVariable Long versionId,
                                                 @Valid @RequestBody LayoutRequest r) {
        return ApiResponse.ok(LayoutResponse.from(formService.addLayout(
                versionId, r.targetType(), r.targetValue(), r.layout(), r.priority())));
    }

    @GetMapping("/form-versions/{versionId}/layout")
    @PreAuthorize("@authz.has('metadata:view')")
    @Operation(summary = "Resolve the layout for a render context (most specific wins)")
    public ApiResponse<LayoutResponse> resolveLayout(@PathVariable Long versionId,
                                                     @RequestParam(required = false) String role,
                                                     @RequestParam(required = false) String company,
                                                     @RequestParam(required = false) String branch,
                                                     @RequestParam(required = false) String device,
                                                     @RequestParam(required = false) String stage) {
        Map<String, String> context = new java.util.HashMap<>();
        if (role != null) context.put("role", role);
        if (company != null) context.put("company", company);
        if (branch != null) context.put("branch", branch);
        if (device != null) context.put("device", device);
        if (stage != null) context.put("stage", stage);
        var layout = formService.resolveLayout(versionId, context);
        return ApiResponse.ok(layout == null ? null : LayoutResponse.from(layout));
    }

    @PostMapping("/records/bind")
    @PreAuthorize("@authz.has('metadata:write-values')")
    @Operation(summary = "Bind a record to the form version it was captured with")
    public ApiResponse<Void> bind(@Valid @RequestBody BindRequest r) {
        formService.bindRecord(r.moduleCode(), r.entityCode(), r.recordId(), r.formVersionId());
        return ApiResponse.ok();
    }

    @GetMapping("/records/form-version")
    @PreAuthorize("@authz.has('metadata:view')")
    @Operation(summary = "The form version a historical record must be rendered with")
    public ApiResponse<FormVersionResponse> versionForRecord(@RequestParam String moduleCode,
                                                             @RequestParam String entityCode,
                                                             @RequestParam Long recordId) {
        var version = formService.versionForRecord(moduleCode, entityCode, recordId);
        return ApiResponse.ok(version == null ? null : FormVersionResponse.from(version));
    }
}
