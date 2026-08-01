package com.sami.app.inventory.repository;

import com.sami.app.inventory.domain.InventoryWarehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Tenant-scoped warehouse persistence shared with Purchasing by stable id. */
public interface InventoryWarehouseRepository extends JpaRepository<InventoryWarehouse, Long> {

    List<InventoryWarehouse> findByTenantIdOrderByDisplayOrderAsc(Long tenantId);

    Optional<InventoryWarehouse> findByIdAndTenantId(Long id, Long tenantId);

    Optional<InventoryWarehouse> findFirstByTenantIdAndBranchIdAndActiveTrueOrderByDefaultWarehouseDescDisplayOrderAsc(
            Long tenantId, Long branchId);

    Optional<InventoryWarehouse> findByTenantIdAndDefaultWarehouseTrueAndActiveTrue(Long tenantId);

    boolean existsByTenantIdAndCodeIgnoreCase(Long tenantId, String code);

    boolean existsByTenantIdAndCodeIgnoreCaseAndIdNot(Long tenantId, String code, Long id);
}
