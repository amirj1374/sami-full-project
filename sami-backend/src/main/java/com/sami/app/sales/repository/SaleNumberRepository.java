package com.sami.app.sales.repository;
import com.sami.app.sales.domain.SaleNumber; import org.springframework.data.jpa.repository.*; import jakarta.persistence.LockModeType; import java.util.*;
public interface SaleNumberRepository extends JpaRepository<SaleNumber,Long> { @Lock(LockModeType.PESSIMISTIC_WRITE) Optional<SaleNumber> findByTenantIdAndSequenceYear(Long tenantId,int year); }
