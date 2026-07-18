package com.sami.app.supplier.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.supplier.domain.SupCategory;
import com.sami.app.supplier.domain.SupPaymentTerm;
import com.sami.app.supplier.domain.SupRatingCriterion;
import com.sami.app.supplier.domain.SupTag;
import com.sami.app.supplier.domain.SupType;
import com.sami.app.supplier.dto.SupLookupDtos.CategoryRequest;
import com.sami.app.supplier.dto.SupLookupDtos.CategoryResponse;
import com.sami.app.supplier.dto.SupLookupDtos.DocumentTypeResponse;
import com.sami.app.supplier.dto.SupLookupDtos.PaymentTermRequest;
import com.sami.app.supplier.dto.SupLookupDtos.PaymentTermResponse;
import com.sami.app.supplier.dto.SupLookupDtos.RatingCriterionRequest;
import com.sami.app.supplier.dto.SupLookupDtos.RatingCriterionResponse;
import com.sami.app.supplier.dto.SupLookupDtos.StatusResponse;
import com.sami.app.supplier.dto.SupLookupDtos.TagRequest;
import com.sami.app.supplier.dto.SupLookupDtos.TagResponse;
import com.sami.app.supplier.dto.SupLookupDtos.TypeRequest;
import com.sami.app.supplier.dto.SupLookupDtos.TypeResponse;
import com.sami.app.supplier.repository.SupCategoryRepository;
import com.sami.app.supplier.repository.SupDocumentTypeRepository;
import com.sami.app.supplier.repository.SupPaymentTermRepository;
import com.sami.app.supplier.repository.SupRatingCriterionRepository;
import com.sami.app.supplier.repository.SupStatusRepository;
import com.sami.app.supplier.repository.SupTagRepository;
import com.sami.app.supplier.repository.SupTypeRepository;
import com.sami.app.supplier.repository.SupplierRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
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
 * Supplier configuration: types, statuses (read), categories, tags, payment
 * terms, rating criteria and document types. Writes require
 * {@code suppliers:manage-config} (or suppliers:edit).
 */
@RestController
@RequestMapping("/api/v1/supplier-config")
@RequiredArgsConstructor
@Tag(name = "Supplier configuration", description = "Configurable supplier lookups and rules")
public class SupplierConfigController {

    private static final String MANAGE = "@authz.hasAny('suppliers:manage-config','suppliers:edit')";

    private final SupTypeRepository typeRepository;
    private final SupStatusRepository statusRepository;
    private final SupCategoryRepository categoryRepository;
    private final SupTagRepository tagRepository;
    private final SupPaymentTermRepository paymentTermRepository;
    private final SupRatingCriterionRepository criterionRepository;
    private final SupDocumentTypeRepository documentTypeRepository;
    private final SupplierRepository supplierRepository;

    @GetMapping("/statuses")
    @PreAuthorize("@authz.has('suppliers:view')")
    @Operation(summary = "List supplier statuses")
    public ApiResponse<List<StatusResponse>> statuses() {
        return ApiResponse.ok(statusRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(StatusResponse::from).toList());
    }

    @GetMapping("/document-types")
    @PreAuthorize("@authz.has('suppliers:view')")
    @Operation(summary = "List document types")
    public ApiResponse<List<DocumentTypeResponse>> documentTypes() {
        return ApiResponse.ok(documentTypeRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(DocumentTypeResponse::from).toList());
    }

    // ----------------------------------------------------------------- types

    @GetMapping("/types")
    @PreAuthorize("@authz.has('suppliers:view')")
    @Operation(summary = "List supplier types")
    public ApiResponse<List<TypeResponse>> types() {
        return ApiResponse.ok(typeRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(TypeResponse::from).toList());
    }

    @PostMapping("/types")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    @Operation(summary = "Create a supplier type")
    @Transactional
    public ApiResponse<TypeResponse> createType(@Valid @RequestBody TypeRequest request) {
        if (typeRepository.existsByCodeIgnoreCase(request.code())) {
            throw conflict("type", request.code());
        }
        return ApiResponse.ok(TypeResponse.from(typeRepository.save(SupType.builder()
                .code(request.code()).name(request.name()).description(request.description())
                .active(request.active()).displayOrder(request.displayOrder()).build())));
    }

    @PutMapping("/types/{id}")
    @PreAuthorize(MANAGE)
    @Operation(summary = "Update a supplier type")
    @Transactional
    public ApiResponse<TypeResponse> updateType(@PathVariable Long id,
                                                @Valid @RequestBody TypeRequest request) {
        SupType type = typeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Supplier type", id));
        if (type.isSystem() && !type.getCode().equals(request.code())) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "System type codes are immutable");
        }
        if (typeRepository.existsByCodeIgnoreCaseAndIdNot(request.code(), id)) {
            throw conflict("type", request.code());
        }
        type.setCode(request.code());
        type.setName(request.name());
        type.setDescription(request.description());
        type.setActive(request.active());
        type.setDisplayOrder(request.displayOrder());
        return ApiResponse.ok(TypeResponse.from(type));
    }

    @DeleteMapping("/types/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(MANAGE)
    @Operation(summary = "Delete a supplier type (blocked for system/in-use entries)")
    @Transactional
    public void deleteType(@PathVariable Long id) {
        SupType type = typeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Supplier type", id));
        if (type.isSystem()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "System types cannot be deleted");
        }
        if (supplierRepository.countByTypeId(id) > 0) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Cannot delete a type that is assigned to suppliers");
        }
        typeRepository.delete(type);
    }

    // ------------------------------------------------------------ categories

    @GetMapping("/categories")
    @PreAuthorize("@authz.has('suppliers:view')")
    @Operation(summary = "List categories")
    public ApiResponse<List<CategoryResponse>> categories() {
        return ApiResponse.ok(categoryRepository.findAllByOrderByNameAsc().stream()
                .map(CategoryResponse::from).toList());
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    @Operation(summary = "Create a category")
    @Transactional
    public ApiResponse<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw conflict("category", request.name());
        }
        return ApiResponse.ok(CategoryResponse.from(categoryRepository.save(SupCategory.builder()
                .name(request.name()).description(request.description())
                .active(request.active()).build())));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize(MANAGE)
    @Operation(summary = "Update a category")
    @Transactional
    public ApiResponse<CategoryResponse> updateCategory(@PathVariable Long id,
                                                        @Valid @RequestBody CategoryRequest request) {
        SupCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id));
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw conflict("category", request.name());
        }
        category.setName(request.name());
        category.setDescription(request.description());
        category.setActive(request.active());
        return ApiResponse.ok(CategoryResponse.from(category));
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(MANAGE)
    @Operation(summary = "Delete a category (unlinks it from suppliers)")
    @Transactional
    public void deleteCategory(@PathVariable Long id) {
        categoryRepository.delete(categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id)));
    }

    // ------------------------------------------------------------------ tags

    @GetMapping("/tags")
    @PreAuthorize("@authz.has('suppliers:view')")
    @Operation(summary = "List tags")
    public ApiResponse<List<TagResponse>> tags() {
        return ApiResponse.ok(tagRepository.findAllByOrderByNameAsc().stream()
                .map(TagResponse::from).toList());
    }

    @PostMapping("/tags")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    @Operation(summary = "Create a tag")
    @Transactional
    public ApiResponse<TagResponse> createTag(@Valid @RequestBody TagRequest request) {
        if (tagRepository.existsByNameIgnoreCase(request.name())) {
            throw conflict("tag", request.name());
        }
        return ApiResponse.ok(TagResponse.from(tagRepository.save(SupTag.builder()
                .name(request.name()).color(request.color()).active(request.active()).build())));
    }

    @PutMapping("/tags/{id}")
    @PreAuthorize(MANAGE)
    @Operation(summary = "Update a tag")
    @Transactional
    public ApiResponse<TagResponse> updateTag(@PathVariable Long id,
                                              @Valid @RequestBody TagRequest request) {
        SupTag tag = tagRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Tag", id));
        if (tagRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw conflict("tag", request.name());
        }
        tag.setName(request.name());
        tag.setColor(request.color());
        tag.setActive(request.active());
        return ApiResponse.ok(TagResponse.from(tag));
    }

    @DeleteMapping("/tags/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(MANAGE)
    @Operation(summary = "Delete a tag (unlinks it from suppliers)")
    @Transactional
    public void deleteTag(@PathVariable Long id) {
        tagRepository.delete(tagRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Tag", id)));
    }

    // ---------------------------------------------------------- payment terms

    @GetMapping("/payment-terms")
    @PreAuthorize("@authz.has('suppliers:view')")
    @Operation(summary = "List payment terms")
    public ApiResponse<List<PaymentTermResponse>> paymentTerms() {
        return ApiResponse.ok(paymentTermRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(PaymentTermResponse::from).toList());
    }

    @PostMapping("/payment-terms")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    @Operation(summary = "Create a payment term")
    @Transactional
    public ApiResponse<PaymentTermResponse> createPaymentTerm(
            @Valid @RequestBody PaymentTermRequest request) {
        if (paymentTermRepository.existsByCodeIgnoreCase(request.code())) {
            throw conflict("payment term", request.code());
        }
        return ApiResponse.ok(PaymentTermResponse.from(paymentTermRepository.save(
                SupPaymentTerm.builder()
                        .code(request.code()).name(request.name()).days(request.days())
                        .active(request.active()).displayOrder(request.displayOrder()).build())));
    }

    @PutMapping("/payment-terms/{id}")
    @PreAuthorize(MANAGE)
    @Operation(summary = "Update a payment term")
    @Transactional
    public ApiResponse<PaymentTermResponse> updatePaymentTerm(
            @PathVariable Long id, @Valid @RequestBody PaymentTermRequest request) {
        SupPaymentTerm term = paymentTermRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Payment term", id));
        if (term.isSystem() && !term.getCode().equals(request.code())) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "System term codes are immutable");
        }
        if (paymentTermRepository.existsByCodeIgnoreCaseAndIdNot(request.code(), id)) {
            throw conflict("payment term", request.code());
        }
        term.setCode(request.code());
        term.setName(request.name());
        term.setDays(request.days());
        term.setActive(request.active());
        term.setDisplayOrder(request.displayOrder());
        return ApiResponse.ok(PaymentTermResponse.from(term));
    }

    @DeleteMapping("/payment-terms/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(MANAGE)
    @Operation(summary = "Delete a payment term (blocked for system/in-use entries)")
    @Transactional
    public void deletePaymentTerm(@PathVariable Long id) {
        SupPaymentTerm term = paymentTermRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Payment term", id));
        if (term.isSystem()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "System payment terms cannot be deleted");
        }
        if (supplierRepository.countByPaymentTermId(id) > 0) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Cannot delete a payment term assigned to suppliers");
        }
        paymentTermRepository.delete(term);
    }

    // -------------------------------------------------------- rating criteria

    @GetMapping("/rating-criteria")
    @PreAuthorize("@authz.has('suppliers:view')")
    @Operation(summary = "List rating criteria (with weights)")
    public ApiResponse<List<RatingCriterionResponse>> ratingCriteria() {
        return ApiResponse.ok(criterionRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(RatingCriterionResponse::from).toList());
    }

    @PostMapping("/rating-criteria")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    @Operation(summary = "Create a rating criterion")
    @Transactional
    public ApiResponse<RatingCriterionResponse> createCriterion(
            @Valid @RequestBody RatingCriterionRequest request) {
        if (criterionRepository.existsByCodeIgnoreCase(request.code())) {
            throw conflict("rating criterion", request.code());
        }
        return ApiResponse.ok(RatingCriterionResponse.from(criterionRepository.save(
                SupRatingCriterion.builder()
                        .code(request.code()).name(request.name()).weight(request.weight())
                        .active(request.active()).displayOrder(request.displayOrder()).build())));
    }

    @PutMapping("/rating-criteria/{id}")
    @PreAuthorize(MANAGE)
    @Operation(summary = "Update a rating criterion (weight changes reshape the overall rating)")
    @Transactional
    public ApiResponse<RatingCriterionResponse> updateCriterion(
            @PathVariable Long id, @Valid @RequestBody RatingCriterionRequest request) {
        SupRatingCriterion criterion = criterionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Rating criterion", id));
        criterion.setName(request.name());
        criterion.setWeight(request.weight());
        criterion.setActive(request.active());
        criterion.setDisplayOrder(request.displayOrder());
        return ApiResponse.ok(RatingCriterionResponse.from(criterion));
    }

    private ApiException conflict(String kind, String value) {
        return new ApiException(ErrorCode.RESOURCE_CONFLICT,
                "A supplier %s named '%s' already exists".formatted(kind, value));
    }
}
