package com.sami.app.treasury.repository;
import com.sami.app.treasury.domain.TreasuryChequeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TreasuryChequeStatusRepository extends JpaRepository<TreasuryChequeStatus,Long>{List<TreasuryChequeStatus> findByTenantIdIsNullOrTenantIdOrderByDisplayOrderAsc(Long tenantId);}
