package com.sami.app.treasury.repository;
import com.sami.app.treasury.domain.TreasuryAccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TreasuryAccountTypeRepository extends JpaRepository<TreasuryAccountType,Long>{List<TreasuryAccountType> findByTenantIdIsNullOrTenantIdOrderByDisplayOrderAsc(Long tenantId);}
