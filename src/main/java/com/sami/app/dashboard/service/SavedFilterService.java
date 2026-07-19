package com.sami.app.dashboard.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.dashboard.domain.DashboardSavedFilter;
import com.sami.app.dashboard.dto.DashboardDtos.SavedFilterRequest;
import com.sami.app.dashboard.dto.DashboardDtos.SavedFilterResponse;
import com.sami.app.dashboard.repository.DashboardSavedFilterRepository;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Per-user saved dashboard filters (the filter engine's persistence). Each user
 * only sees and manages their own saved filters.
 */
@Service
@RequiredArgsConstructor
public class SavedFilterService {

    private final DashboardSavedFilterRepository repository;

    @Transactional(readOnly = true)
    public List<SavedFilterResponse> list() {
        return repository.findByOwnerIdOrderByNameAsc(CurrentActor.id()).stream()
                .map(f -> new SavedFilterResponse(f.getId(), f.getName(), f.getFilter()))
                .toList();
    }

    @Transactional
    public SavedFilterResponse create(SavedFilterRequest request) {
        Long ownerId = CurrentActor.id();
        if (repository.existsByOwnerIdAndNameIgnoreCase(ownerId, request.name())) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "You already have a saved filter named '%s'".formatted(request.name()));
        }
        DashboardSavedFilter saved = repository.save(DashboardSavedFilter.builder()
                .ownerId(ownerId).name(request.name()).filter(request.filter()).build());
        return new SavedFilterResponse(saved.getId(), saved.getName(), saved.getFilter());
    }

    @Transactional
    public void delete(Long id) {
        DashboardSavedFilter filter = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Saved filter", id));
        if (!filter.getOwnerId().equals(CurrentActor.id())) {
            throw ResourceNotFoundException.of("Saved filter", id);
        }
        repository.delete(filter);
    }
}
