package com.sami.app.files.repository;

import com.sami.app.files.domain.FileAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileAuditLogRepository extends JpaRepository<FileAuditLog, Long> {

    Page<FileAuditLog> findAllByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType, Long entityId, Pageable pageable);

    Page<FileAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
