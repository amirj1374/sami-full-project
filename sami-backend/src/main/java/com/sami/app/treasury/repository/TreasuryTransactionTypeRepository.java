package com.sami.app.treasury.repository;
import com.sami.app.treasury.domain.TreasuryTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TreasuryTransactionTypeRepository extends JpaRepository<TreasuryTransactionType,Long>{List<TreasuryTransactionType> findByTenantIdIsNullOrTenantIdOrderByDisplayOrderAsc(Long tenantId);}
