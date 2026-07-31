package com.sami.app.automation.repository;

import com.sami.app.automation.domain.AutomationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AutomationStatusRepository extends JpaRepository<AutomationStatus, Long> {

    @Query("""
            select status from AutomationStatus status
            where status.tenantId is null or status.tenantId = :tenantId
            order by case when status.tenantId = :tenantId then 0 else 1 end,
                     status.displayOrder asc
            """)
    List<AutomationStatus> findVisible(@Param("tenantId") Long tenantId);

    @Query("""
            select status from AutomationStatus status
            where status.code = :code
              and (status.tenantId is null or status.tenantId = :tenantId)
            order by case when status.tenantId = :tenantId then 0 else 1 end
            """)
    List<AutomationStatus> findVisibleByCode(@Param("tenantId") Long tenantId,
                                             @Param("code") String code);
}
