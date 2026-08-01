package com.sami.app.crm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.crm.domain.Segment;
import com.sami.app.crm.dto.CustomerFilter;
import com.sami.app.crm.dto.CustomerResponse;
import com.sami.app.crm.dto.SegmentDtos.SegmentRequest;
import com.sami.app.crm.dto.SegmentDtos.SegmentResponse;
import com.sami.app.crm.repository.SegmentRepository;
import com.sami.app.common.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Stored segments: named, reusable customer filters (marketing lists, saved
 * searches, report inputs). A segment holds the SAME filter shape the listing
 * uses, so anything filterable — including event-recency rules like "purchased
 * in the last 30 days" or "no activity for 6 months" — can be saved and re-run
 * against live data at any time. The foundation for future campaigns.
 */
@Service
@RequiredArgsConstructor
public class SegmentService {

    private final SegmentRepository segmentRepository;
    private final CustomerService customerService;
    private final ObjectMapper objectMapper;
    private final TenantContext tenantContext;

    @Transactional(readOnly = true)
    public List<SegmentResponse> list() {
        return segmentRepository.findAllByTenantIdOrderByNameAsc(tenantContext.requireTenantId()).stream()
                .map(SegmentResponse::from)
                .toList();
    }

    @Transactional
    public SegmentResponse create(SegmentRequest request) {
        Long tenantId = tenantContext.requireTenantId();
        if (segmentRepository.existsByTenantIdAndNameIgnoreCase(tenantId, request.name())) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "A segment named '%s' already exists".formatted(request.name()));
        }
        requireValidFilter(request.filter());
        return SegmentResponse.from(segmentRepository.save(Segment.builder()
                .tenantId(tenantId)
                .name(request.name())
                .description(request.description())
                .filter(request.filter())
                .build()));
    }

    @Transactional
    public SegmentResponse update(Long id, SegmentRequest request) {
        Segment segment = findOrThrow(id);
        if (segmentRepository.existsByTenantIdAndNameIgnoreCaseAndIdNot(
                tenantContext.requireTenantId(), request.name(), id)) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "A segment named '%s' already exists".formatted(request.name()));
        }
        requireValidFilter(request.filter());
        segment.setName(request.name());
        segment.setDescription(request.description());
        segment.setFilter(request.filter());
        return SegmentResponse.from(segment);
    }

    @Transactional
    public void delete(Long id) {
        segmentRepository.delete(findOrThrow(id));
    }

    /** Evaluates the stored filter against live data. */
    @Transactional(readOnly = true)
    public Page<CustomerResponse> run(Long id, Pageable pageable) {
        return customerService.list(toFilter(findOrThrow(id).getFilter()), pageable);
    }

    private CustomerFilter toFilter(java.util.Map<String, Object> filter) {
        return objectMapper.convertValue(filter, CustomerFilter.class);
    }

    /** A stored filter must deserialize into the listing filter shape. */
    private void requireValidFilter(java.util.Map<String, Object> filter) {
        try {
            toFilter(filter);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "The segment filter does not match the customer filter shape");
        }
    }

    private Segment findOrThrow(Long id) {
        return segmentRepository.findByIdAndTenantId(id, tenantContext.requireTenantId())
                .orElseThrow(() -> ResourceNotFoundException.of("Segment", id));
    }
}
