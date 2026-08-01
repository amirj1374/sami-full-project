package com.sami.app.sales.repository;

import com.sami.app.sales.domain.SaleReturnNumber;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;

public interface SaleReturnNumberRepository extends JpaRepository<SaleReturnNumber, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from SaleReturnNumber n where n.tenantId = :tenantId")
    Optional<SaleReturnNumber> findForUpdate(Long tenantId);
}
