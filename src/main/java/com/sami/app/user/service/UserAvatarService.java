package com.sami.app.user.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.storage.FileStorage;
import com.sami.app.common.storage.StorageProperties;
import com.sami.app.user.domain.User;
import com.sami.app.user.domain.UserProfile;
import com.sami.app.user.repository.UserProfileRepository;
import com.sami.app.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Profile images, built on the {@link FileStorage} port — the service knows
 * nothing about where bytes live (local disk today, MinIO/S3 later).
 *
 * <p>Upload limits (size, content types) come from configuration, not constants.
 * A missing image is a normal state: {@code load} returns empty and the UI falls
 * back to initials.
 */
@Service
@RequiredArgsConstructor
public class UserAvatarService {

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final FileStorage fileStorage;
    private final StorageProperties storageProperties;
    private final UserAuditService auditService;

    @Transactional
    public void upload(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "An image file is required");
        }
        if (file.getSize() > storageProperties.maxImageSizeBytes()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Image must be at most %d KB".formatted(storageProperties.maxImageSizeBytes() / 1024));
        }
        String contentType = file.getContentType();
        if (contentType == null || !storageProperties.allowedImageTypes().contains(contentType)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Allowed image types: " + String.join(", ", storageProperties.allowedImageTypes()));
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> profileRepository.save(UserProfile.builder().user(user).build()));

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Could not read the uploaded file");
        }

        String oldKey = profile.getAvatarKey();
        String newKey = fileStorage.store("avatars/" + userId, content, contentType);
        profile.setAvatarKey(newKey);
        if (oldKey != null) {
            fileStorage.delete(oldKey);
        }

        auditService.record(user, UserAuditService.AVATAR_UPDATED,
                Map.of("avatar", oldKey != null ? "replaced" : "none"),
                Map.of("avatar", "updated"));
    }

    @Transactional(readOnly = true)
    public Optional<FileStorage.StoredFile> load(Long userId) {
        return profileRepository.findByUserId(userId)
                .map(UserProfile::getAvatarKey)
                .flatMap(fileStorage::load);
    }

    @Transactional
    public void remove(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        profileRepository.findByUserId(userId).ifPresent(profile -> {
            if (profile.getAvatarKey() != null) {
                fileStorage.delete(profile.getAvatarKey());
                profile.setAvatarKey(null);
                auditService.record(user, UserAuditService.AVATAR_UPDATED,
                        Map.of("avatar", "present"), Map.of("avatar", "removed"));
            }
        });
    }
}
