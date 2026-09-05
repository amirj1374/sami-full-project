package com.sami.app.treasury.repository;
import com.sami.app.treasury.domain.TreasuryCheque;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface TreasuryChequeRepository extends JpaRepository<TreasuryCheque,Long> {
 Optional<TreasuryCheque> findByIdAndTenantId(Long id,Long tenantId);
 Page<TreasuryCheque> findByTenantIdOrderByDueDateAscIdDesc(Long tenantId, Pageable pageable);
 long countByTenantId(Long tenantId);
 boolean existsByTenantIdAndDirectionAndNormalizedBankNameAndChequeNumber(Long tenantId,TreasuryCheque.Direction direction,String bank,String number);
 boolean existsByTenantIdAndDirectionAndNormalizedBankNameAndChequeNumberAndIdNot(Long tenantId,TreasuryCheque.Direction direction,String bank,String number,Long id);
}
