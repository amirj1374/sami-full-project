package com.sami.app.user.service;

import com.sami.app.auth.RefreshTokenService;
import com.sami.app.authz.domain.Role;
import com.sami.app.authz.repository.RoleRepository;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.user.domain.User;
import com.sami.app.user.domain.UserProfile;
import com.sami.app.user.domain.UserStatus;
import com.sami.app.user.dto.BulkResultResponse;
import com.sami.app.user.dto.CreateUserRequest;
import com.sami.app.user.dto.CurrentUserResponse;
import com.sami.app.user.dto.UpdateUserRequest;
import com.sami.app.user.dto.UserDetailResponse;
import com.sami.app.user.dto.UserFilter;
import com.sami.app.user.dto.UserProfileRequest;
import com.sami.app.user.dto.UserResponse;
import com.sami.app.user.repository.UserProfileRepository;
import com.sami.app.user.repository.UserRepository;
import com.sami.app.user.repository.UserSpecifications;
import com.sami.app.user.repository.UserStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * User lifecycle management: CRUD, status transitions, archive / soft-delete /
 * restore / purge, and bulk operations.
 *
 * <p>Central invariants:
 * <ul>
 *   <li>Administrators never act on their own account (no accidental lockouts).</li>
 *   <li>Any transition into a status that forbids login revokes the user's
 *       refresh tokens, so open sessions die immediately.</li>
 *   <li>Rows are never physically removed except by the explicit, separately
 *       permissioned purge — and purge only accepts already soft-deleted users,
 *       so referenced records stay resolvable for other modules.</li>
 *   <li>Every mutation appends an audit entry (old/new snapshots) in the same
 *       transaction.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final RoleRepository roleRepository;
    private final UserStatusRepository statusRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final ProfileFieldService profileFieldService;
    private final UserAuditService auditService;

    // ------------------------------------------------------------------ reads

    /** The authenticated user's own profile, including the permission list. */
    @Transactional(readOnly = true)
    public CurrentUserResponse me(Long userId) {
        User user = userRepository.findWithRoleById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        return CurrentUserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(UserFilter filter, Pageable pageable) {
        return userRepository.findAll(toSpecification(filter), pageable).map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserDetailResponse getDetail(Long id) {
        User user = userRepository.findWithDetailsById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
        return UserDetailResponse.from(user);
    }

    /** All rows matching the filter as CSV (bulk export honors the active filters). */
    @Transactional(readOnly = true)
    public String exportCsv(UserFilter filter) {
        List<User> users = userRepository.findAll(toSpecification(filter),
                Sort.by(Sort.Direction.ASC, "id"));
        StringBuilder csv = new StringBuilder(
                "\uFEFFid,email,fullName,firstName,lastName,phoneNumber,employeeCode,nationalCode,status,role,createdAt\n");
        for (User user : users) {
            UserProfile p = user.getProfile();
            csv.append(csvRow(
                    String.valueOf(user.getId()),
                    user.getEmail(),
                    user.getFullName(),
                    p != null ? p.getFirstName() : null,
                    p != null ? p.getLastName() : null,
                    p != null ? p.getPhoneNumber() : null,
                    p != null ? p.getEmployeeCode() : null,
                    p != null ? p.getNationalCode() : null,
                    user.getStatus().getName(),
                    user.getRole().getName(),
                    user.getCreatedAt().toString()));
        }
        return csv.toString();
    }

    // ---------------------------------------------------------------- create

    @Transactional
    public UserDetailResponse create(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(findRoleOrThrow(request.roleId()))
                .status(findStatusOrThrow(request.statusId()))
                .build();
        userRepository.save(user);

        UserProfile profile = UserProfile.builder().user(user).build();
        applyProfile(profile, request.profile() != null ? request.profile() : UserProfileRequest.empty());
        user.setProfile(profileRepository.save(profile));

        auditService.record(user, UserAuditService.CREATED, null, snapshot(user));
        return UserDetailResponse.from(user);
    }

    // ---------------------------------------------------------------- update

    /**
     * Full update (PUT semantics): name, role, status and profile. Optimistic
     * concurrency via {@code expectedVersion}; a stale payload conflicts instead
     * of overwriting a concurrent edit.
     */
    @Transactional
    public UserDetailResponse update(Long id, UpdateUserRequest request, Long actorId) {
        rejectSelf(id, actorId, "You cannot modify your own account");
        User user = findWithDetailsOrThrow(id);

        if (request.expectedVersion() != null
                && !request.expectedVersion().equals(user.getVersion())) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "The user was modified by someone else; reload and retry");
        }

        Map<String, Object> before = snapshot(user);

        user.setFullName(request.fullName());
        user.setRole(findRoleOrThrow(request.roleId()));
        transitionStatus(user, findStatusOrThrow(request.statusId()));

        UserProfile profile = user.getProfile();
        if (profile == null) {
            profile = profileRepository.save(UserProfile.builder().user(user).build());
            user.setProfile(profile);
        }
        applyProfile(profile, request.profile() != null ? request.profile() : UserProfileRequest.empty());

        auditService.record(user, UserAuditService.UPDATED, diff(before, snapshot(user)),
                diffNew(before, snapshot(user)));
        return UserDetailResponse.from(user);
    }

    /** Status-only transition (activate / deactivate / suspend / custom status). */
    @Transactional
    public UserResponse changeStatus(Long id, Long statusId, Long actorId) {
        rejectSelf(id, actorId, "You cannot change your own account status");
        User user = findOrThrow(id);
        UserStatus target = findStatusOrThrow(statusId);
        if (target.isArchivedState() || target.isDeletedState()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Use the archive/delete operations for lifecycle statuses");
        }

        String oldStatus = user.getStatus().getName();
        transitionStatus(user, target);
        auditService.record(user, UserAuditService.STATUS_CHANGED,
                Map.of("status", oldStatus), Map.of("status", target.getName()));
        return UserResponse.from(user);
    }

    // ------------------------------------------------------------- lifecycle

    /** Archives a user: archived status + stamp; hidden from default listings. */
    @Transactional
    public UserResponse archive(Long id, Long actorId) {
        rejectSelf(id, actorId, "You cannot archive your own account");
        User user = findOrThrow(id);
        if (user.getStatus().isArchivedState() || user.getStatus().isDeletedState()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "The user is already archived or deleted");
        }

        String oldStatus = user.getStatus().getName();
        UserStatus archived = statusRepository.findByIsArchivedStateTrue()
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                        "No archived status is configured"));
        transitionStatus(user, archived);
        user.setArchivedAt(Instant.now());
        user.setArchivedBy(actorId);

        auditService.record(user, UserAuditService.ARCHIVED,
                Map.of("status", oldStatus), Map.of("status", archived.getName()));
        return UserResponse.from(user);
    }

    /** Restores an archived or soft-deleted user back to the default status. */
    @Transactional
    public UserResponse restore(Long id, Long actorId) {
        User user = findOrThrow(id);
        if (!user.getStatus().isArchivedState() && !user.getStatus().isDeletedState()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Only archived or deleted users can be restored");
        }

        String oldStatus = user.getStatus().getName();
        UserStatus target = statusRepository.findByIsDefaultTrue()
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                        "No default status is configured"));
        transitionStatus(user, target);
        user.setArchivedAt(null);
        user.setArchivedBy(null);
        user.setDeletedAt(null);
        user.setDeletedBy(null);

        auditService.record(user, UserAuditService.RESTORED,
                Map.of("status", oldStatus), Map.of("status", target.getName()));
        return UserResponse.from(user);
    }

    /** Soft delete: deleted status + stamp. The row and all references survive. */
    @Transactional
    public UserResponse softDelete(Long id, Long actorId) {
        rejectSelf(id, actorId, "You cannot delete your own account");
        User user = findOrThrow(id);
        if (user.getStatus().isDeletedState()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "The user is already deleted");
        }

        String oldStatus = user.getStatus().getName();
        UserStatus deleted = statusRepository.findByIsDeletedStateTrue()
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                        "No deleted status is configured"));
        transitionStatus(user, deleted);
        user.setDeletedAt(Instant.now());
        user.setDeletedBy(actorId);

        auditService.record(user, UserAuditService.DELETED,
                Map.of("status", oldStatus), Map.of("status", deleted.getName()));
        return UserResponse.from(user);
    }

    /**
     * Permanent delete. Separately permissioned and only accepted for users that
     * were soft-deleted first — a deliberate two-step gate before data loss. The
     * audit trail intentionally survives (it has no FK on users).
     */
    @Transactional
    public void purge(Long id, Long actorId) {
        rejectSelf(id, actorId, "You cannot delete your own account");
        User user = findWithDetailsOrThrow(id);
        if (!user.getStatus().isDeletedState()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Only soft-deleted users can be permanently removed");
        }

        auditService.record(user, UserAuditService.PURGED, snapshot(user), null);
        refreshTokenService.revokeAllForUser(user.getId());
        userRepository.delete(user);
    }

    // ------------------------------------------------------------------ bulk

    @Transactional
    public BulkResultResponse bulkChangeStatus(List<Long> ids, Long statusId, Long actorId) {
        return bulk(ids, id -> changeStatus(id, statusId, actorId));
    }

    @Transactional
    public BulkResultResponse bulkArchive(List<Long> ids, Long actorId) {
        return bulk(ids, id -> archive(id, actorId));
    }

    @Transactional
    public BulkResultResponse bulkRestore(List<Long> ids, Long actorId) {
        return bulk(ids, id -> restore(id, actorId));
    }

    // -------------------------------------------------------------- internals

    /**
     * Runs one operation per id, collecting per-item failures instead of aborting
     * the batch. Business rejections (self, wrong state, unknown id) become
     * skip reasons; anything unexpected still propagates.
     */
    private BulkResultResponse bulk(List<Long> ids, Function<Long, Object> operation) {
        int processed = 0;
        List<BulkResultResponse.SkippedItem> skipped = new ArrayList<>();
        for (Long id : ids.stream().distinct().toList()) {
            try {
                operation.apply(id);
                processed++;
            } catch (ApiException ex) {
                skipped.add(new BulkResultResponse.SkippedItem(id, ex.getMessage()));
            }
        }
        return new BulkResultResponse(processed, skipped);
    }

    private Specification<User> toSpecification(UserFilter filter) {
        boolean explicitStatus = filter.statusId() != null;
        boolean includeHidden = Boolean.TRUE.equals(filter.includeHidden());
        return Specification.allOf(
                UserSpecifications.searchContains(filter.search()),
                UserSpecifications.emailContains(filter.email()),
                UserSpecifications.phoneContains(filter.phone()),
                UserSpecifications.employeeCodeContains(filter.employeeCode()),
                UserSpecifications.nationalCodeContains(filter.nationalCode()),
                UserSpecifications.hasRole(filter.roleId()),
                UserSpecifications.hasStatus(filter.statusId()),
                explicitStatus || includeHidden ? null : UserSpecifications.visibleByDefault());
    }

    /**
     * Applies a profile payload after validating duplicates and custom fields.
     * Uniqueness (phone / employee code / national code) is checked here for a
     * clean 409 and enforced again by the partial unique indexes.
     */
    private void applyProfile(UserProfile profile, UserProfileRequest request) {
        Long userId = profile.getUser().getId();
        requireUnique(request.phoneNumber(), userId,
                profileRepository::existsByPhoneNumberAndUserIdNot, "phone number");
        requireUnique(request.employeeCode(), userId,
                profileRepository::existsByEmployeeCodeAndUserIdNot, "employee code");
        requireUnique(request.nationalCode(), userId,
                profileRepository::existsByNationalCodeAndUserIdNot, "national code");

        profile.setFirstName(blankToNull(request.firstName()));
        profile.setLastName(blankToNull(request.lastName()));
        profile.setDisplayName(blankToNull(request.displayName()));
        profile.setNationalCode(blankToNull(request.nationalCode()));
        profile.setEmployeeCode(blankToNull(request.employeeCode()));
        profile.setPhoneNumber(blankToNull(request.phoneNumber()));
        profile.setGender(blankToNull(request.gender()));
        profile.setBirthDate(request.birthDate());
        profile.setAddress(blankToNull(request.address()));
        profile.setNotes(blankToNull(request.notes()));
        profile.setCustomFields(profileFieldService.validateCustomFields(request.customFields()));
    }

    private interface UniquenessCheck {
        boolean exists(String value, Long excludedUserId);
    }

    private void requireUnique(String value, Long userId, UniquenessCheck check, String label) {
        if (value != null && !value.isBlank() && check.exists(value, userId == null ? -1L : userId)) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "A user with this %s already exists".formatted(label));
        }
    }

    /** Sets the status and revokes sessions when the new status forbids login. */
    private void transitionStatus(User user, UserStatus target) {
        boolean couldLogin = user.getStatus() == null || user.getStatus().isAllowsLogin();
        user.setStatus(target);
        if (couldLogin && !target.isAllowsLogin() && user.getId() != null) {
            refreshTokenService.revokeAllForUser(user.getId());
        }
    }

    /** Flat snapshot of auditable fields; diffed to produce old/new audit values. */
    private Map<String, Object> snapshot(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("email", user.getEmail());
        map.put("fullName", user.getFullName());
        map.put("role", user.getRole().getName());
        map.put("status", user.getStatus().getName());
        UserProfile p = user.getProfile();
        if (p != null) {
            map.put("firstName", p.getFirstName());
            map.put("lastName", p.getLastName());
            map.put("displayName", p.getDisplayName());
            map.put("nationalCode", p.getNationalCode());
            map.put("employeeCode", p.getEmployeeCode());
            map.put("phoneNumber", p.getPhoneNumber());
            map.put("gender", p.getGender());
            map.put("birthDate", p.getBirthDate() != null ? p.getBirthDate().toString() : null);
            map.put("address", p.getAddress());
            map.put("notes", p.getNotes());
            map.put("customFields", p.getCustomFields());
        }
        return map;
    }

    /** Keys whose values changed, with their OLD values. */
    private Map<String, Object> diff(Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> changed = new LinkedHashMap<>();
        after.forEach((key, newValue) -> {
            if (!Objects.equals(before.get(key), newValue)) {
                changed.put(key, before.get(key));
            }
        });
        return changed;
    }

    /** Keys whose values changed, with their NEW values. */
    private Map<String, Object> diffNew(Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> changed = new LinkedHashMap<>();
        after.forEach((key, newValue) -> {
            if (!Objects.equals(before.get(key), newValue)) {
                changed.put(key, newValue);
            }
        });
        return changed;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String csvRow(String... cells) {
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                row.append(',');
            }
            String cell = cells[i] == null ? "" : cells[i];
            if (cell.contains(",") || cell.contains("\"") || cell.contains("\n")) {
                cell = '"' + cell.replace("\"", "\"\"") + '"';
            }
            row.append(cell);
        }
        return row.append('\n').toString();
    }

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }

    private User findWithDetailsOrThrow(Long id) {
        return userRepository.findWithDetailsById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }

    private Role findRoleOrThrow(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> ResourceNotFoundException.of("Role", roleId));
    }

    private UserStatus findStatusOrThrow(Long statusId) {
        return statusRepository.findById(statusId)
                .orElseThrow(() -> ResourceNotFoundException.of("Status", statusId));
    }

    /** Self-modification guard: admins manage other accounts, never their own. */
    private void rejectSelf(Long targetId, Long actorId, String message) {
        if (targetId.equals(actorId)) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, message);
        }
    }
}
