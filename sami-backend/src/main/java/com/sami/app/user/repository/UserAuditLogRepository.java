package com.sami.app.user.repository;

import com.sami.app.user.domain.UserAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data-access for the append-only {@link UserAuditLog}. */
public interface UserAuditLogRepository extends JpaRepository<UserAuditLog, Long> {

    Page<UserAuditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
