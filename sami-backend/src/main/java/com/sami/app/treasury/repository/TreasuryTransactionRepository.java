package com.sami.app.treasury.repository;
import com.sami.app.treasury.domain.TreasuryTransaction;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface TreasuryTransactionRepository extends JpaRepository<TreasuryTransaction,Long> {
 Optional<TreasuryTransaction> findByIdAndTenantId(Long id,Long tenantId);
 Page<TreasuryTransaction> findByTenantIdOrderByOccurredAtDescIdDesc(Long tenantId, Pageable pageable);
 boolean existsByTenantIdAndReversalOfId(Long tenantId,Long reversalOfId);
 long countByTenantId(Long tenantId);
}
