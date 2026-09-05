package com.sami.app.treasury.repository;
import com.sami.app.treasury.domain.TreasuryMovement;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TreasuryMovementRepository extends JpaRepository<TreasuryMovement,Long> {
 Page<TreasuryMovement> findByTenantIdAndAccountIdOrderByOccurredAtDescIdDesc(Long tenantId,Long accountId, Pageable pageable);
}
