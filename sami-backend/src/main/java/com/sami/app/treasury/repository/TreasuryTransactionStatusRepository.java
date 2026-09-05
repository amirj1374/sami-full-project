package com.sami.app.treasury.repository;
import com.sami.app.treasury.domain.TreasuryTransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TreasuryTransactionStatusRepository extends JpaRepository<TreasuryTransactionStatus,Long>{List<TreasuryTransactionStatus> findByTenantIdIsNullOrTenantIdOrderByDisplayOrderAsc(Long tenantId);}
