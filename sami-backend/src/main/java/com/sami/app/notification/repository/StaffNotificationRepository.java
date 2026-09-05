package com.sami.app.notification.repository;

import com.sami.app.notification.domain.StaffNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface StaffNotificationRepository extends JpaRepository<StaffNotification, Long> {

    List<StaffNotification> findByTenantIdAndUserIdOrderByCreatedAtDesc(
            Long tenantId, Long userId, Pageable pageable);

    Optional<StaffNotification> findByIdAndTenantIdAndUserId(Long id, Long tenantId, Long userId);
    Optional<StaffNotification> findByTenantIdAndUserIdAndIdempotencyKey(Long tenantId, Long userId, String idempotencyKey);

    long countByTenantIdAndUserIdAndReadAtIsNull(Long tenantId, Long userId);

    @Modifying
    @Query("UPDATE StaffNotification n SET n.readAt = :readAt "
            + "WHERE n.tenantId = :tenantId AND n.userId = :userId AND n.readAt IS NULL")
    int markAllRead(@Param("tenantId") Long tenantId,
                    @Param("userId") Long userId,
                    @Param("readAt") Instant readAt);

    @Modifying
    @Query(value = """
            INSERT INTO staff_notifications
                (tenant_id, user_id, type, title_key, message_key, route,
                 idempotency_key, created_at, updated_at, version)
            VALUES (:tenantId, :userId, 'DEMO', 'notifications.demo.title',
                    :messageKey, '/', :idempotencyKey, now(), now(), 0)
            ON CONFLICT (tenant_id, user_id, idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int insertDemo(@Param("tenantId") Long tenantId,
                   @Param("userId") Long userId,
                   @Param("messageKey") String messageKey,
                   @Param("idempotencyKey") String idempotencyKey);
}
