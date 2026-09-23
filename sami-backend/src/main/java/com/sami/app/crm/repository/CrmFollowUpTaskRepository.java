package com.sami.app.crm.repository;
import com.sami.app.crm.domain.CrmFollowUpTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface CrmFollowUpTaskRepository extends JpaRepository<CrmFollowUpTask, Long> {
    List<CrmFollowUpTask> findByTenantIdOrderByDueAtAsc(Long tenantId);
    Optional<CrmFollowUpTask> findByIdAndTenantId(Long id, Long tenantId);
    Optional<CrmFollowUpTask> findByTenantIdAndIdempotencyKey(Long tenantId, String idempotencyKey);
}
