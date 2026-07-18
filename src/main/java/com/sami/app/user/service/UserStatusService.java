package com.sami.app.user.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.user.domain.UserStatus;
import com.sami.app.user.dto.UserStatusRequest;
import com.sami.app.user.dto.UserStatusResponse;
import com.sami.app.user.repository.UserRepository;
import com.sami.app.user.repository.UserStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Configurable user statuses.
 *
 * <p>Guards: system statuses (the seeded rows carrying the structural flags)
 * cannot be deleted and keep their code; statuses still assigned to users cannot
 * be deleted. Everything else — names, descriptions, login behavior, visibility,
 * ordering, entirely new statuses — is freely editable at runtime.
 */
@Service
@RequiredArgsConstructor
public class UserStatusService {

    private final UserStatusRepository statusRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserStatusResponse> list() {
        return statusRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(UserStatusResponse::from)
                .toList();
    }

    @Transactional
    public UserStatusResponse create(UserStatusRequest request) {
        if (statusRepository.existsByCodeIgnoreCase(request.code())) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "A status with code '%s' already exists".formatted(request.code()));
        }

        UserStatus status = UserStatus.builder()
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .allowsLogin(request.allowsLogin())
                .hiddenByDefault(request.hiddenByDefault())
                .displayOrder(request.displayOrder())
                .build();
        return UserStatusResponse.from(statusRepository.save(status));
    }

    @Transactional
    public UserStatusResponse update(Long id, UserStatusRequest request) {
        UserStatus status = findOrThrow(id);
        if (status.isSystem() && !status.getCode().equals(request.code())) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "System status codes cannot be changed");
        }
        if (statusRepository.existsByCodeIgnoreCaseAndIdNot(request.code(), id)) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "A status with code '%s' already exists".formatted(request.code()));
        }

        status.setCode(request.code());
        status.setName(request.name());
        status.setDescription(request.description());
        status.setAllowsLogin(request.allowsLogin());
        status.setHiddenByDefault(request.hiddenByDefault());
        status.setDisplayOrder(request.displayOrder());
        return UserStatusResponse.from(status);
    }

    @Transactional
    public void delete(Long id) {
        UserStatus status = findOrThrow(id);
        if (status.isSystem()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "System statuses cannot be deleted");
        }
        if (userRepository.countByStatusId(id) > 0) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Cannot delete a status that is still assigned to users");
        }
        statusRepository.delete(status);
    }

    private UserStatus findOrThrow(Long id) {
        return statusRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User status", id));
    }
}
