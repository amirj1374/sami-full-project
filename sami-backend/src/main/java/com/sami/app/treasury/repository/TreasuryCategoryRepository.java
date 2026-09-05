package com.sami.app.treasury.repository;
import com.sami.app.treasury.domain.TreasuryCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TreasuryCategoryRepository extends JpaRepository<TreasuryCategory,Long>{List<TreasuryCategory> findByTenantIdOrderByKindAscDisplayOrderAsc(Long tenantId);}
