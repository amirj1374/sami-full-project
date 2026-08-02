package com.sami.app.notification.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.notification.DemoNotificationProperties;
import com.sami.app.notification.domain.StaffNotification;
import com.sami.app.notification.dto.StaffNotificationResponse;
import com.sami.app.notification.repository.StaffNotificationRepository;
import com.sami.app.user.domain.User;
import com.sami.app.user.repository.UserExperiencePreferenceRepository;
import com.sami.app.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffNotificationService {

    private static final int DEMO_MESSAGE_COUNT = 4;

    private final StaffNotificationRepository notificationRepository;
    private final UserExperiencePreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final TenantContext tenantContext;
    private final DemoNotificationProperties demoProperties;

    @Transactional(readOnly = true)
    public List<StaffNotificationResponse> list(Long userId) {
        Long tenantId = tenantContext.requireTenantId();
        return notificationRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(
                        tenantId, userId, PageRequest.of(0, 30)).stream()
                .map(StaffNotificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByTenantIdAndUserIdAndReadAtIsNull(
                tenantContext.requireTenantId(), userId);
    }

    @Transactional
    public StaffNotificationResponse markRead(Long userId, Long notificationId) {
        Long tenantId = tenantContext.requireTenantId();
        StaffNotification notification = notificationRepository
                .findByIdAndTenantIdAndUserId(notificationId, tenantId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
        }
        return StaffNotificationResponse.from(notificationRepository.save(notification));
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepository.markAllRead(tenantContext.requireTenantId(), userId, Instant.now());
    }

    /** Called only by the shared scheduler handler inside its persisted tenant scope. */
    @Transactional
    public int generateHourlyDemo(Long tenantId, Instant scheduledFor) {
        tenantContext.requireAccessTo(tenantId);
        if (!demoProperties.enabled()) {
            return 0;
        }
        Instant window = (scheduledFor == null ? Instant.now() : scheduledFor)
                .truncatedTo(ChronoUnit.HOURS);
        String windowKey = "demo-hourly:" + window.atOffset(ZoneOffset.UTC);
        int messageIndex = Math.floorMod(window.atOffset(ZoneOffset.UTC).getHour(), DEMO_MESSAGE_COUNT);
        String messageKey = "notifications.demo.messages." + messageIndex;
        int created = 0;
        for (User user : userRepository.findActiveStaffByTenantId(tenantId)) {
            if (preferenceRepository.existsByTenantIdAndUserIdAndDemoNotificationsEnabledTrue(
                    tenantId, user.getId())) {
                created += notificationRepository.insertDemo(
                        tenantId, user.getId(), messageKey, windowKey);
            }
        }
        return created;
    }
}
