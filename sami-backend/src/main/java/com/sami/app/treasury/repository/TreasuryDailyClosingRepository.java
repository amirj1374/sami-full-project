package com.sami.app.treasury.repository;
import com.sami.app.treasury.domain.TreasuryDailyClosing;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TreasuryDailyClosingRepository extends JpaRepository<TreasuryDailyClosing,Long> {
 boolean existsByTenantIdAndAccountIdAndClosingDate(Long tenantId,Long accountId,java.time.LocalDate closingDate);
}
