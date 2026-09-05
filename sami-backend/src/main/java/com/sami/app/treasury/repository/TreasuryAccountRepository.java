package com.sami.app.treasury.repository;
import com.sami.app.treasury.domain.TreasuryAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface TreasuryAccountRepository extends JpaRepository<TreasuryAccount,Long> {
 List<TreasuryAccount> findByTenantIdOrderByNameAsc(Long tenantId);
 Optional<TreasuryAccount> findByIdAndTenantId(Long id, Long tenantId);
 boolean existsByTenantIdAndCodeIgnoreCase(Long tenantId,String code);
 boolean existsByTenantIdAndCodeIgnoreCaseAndIdNot(Long tenantId,String code,Long id);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select a from TreasuryAccount a where a.tenantId=:tenantId and a.id in :ids order by a.id")
 List<TreasuryAccount> lockByTenantIdAndIdIn(@Param("tenantId") Long tenantId,@Param("ids") Collection<Long> ids);
}
