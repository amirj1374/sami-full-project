package com.sami.app.inventory.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.inventory.domain.InventoryWarehouse;
import com.sami.app.inventory.dto.InventoryDtos.LocationRequest;
import com.sami.app.inventory.dto.InventoryDtos.LocationResponse;
import com.sami.app.inventory.dto.InventoryDtos.WarehouseRequest;
import com.sami.app.inventory.dto.InventoryDtos.WarehouseResponse;
import com.sami.app.inventory.repository.InventoryWarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Warehouse and internal-location management using the promoted registry. */
@Service
@RequiredArgsConstructor
public class InventoryWarehouseService {

    private static final Set<String> WAREHOUSE_TYPES = Set.of(
            "STANDARD", "RETAIL", "TRANSIT", "QUARANTINE", "RETURNS");
    private static final Set<String> LOCATION_TYPES = Set.of(
            "RECEIVING", "STORAGE", "PICKING", "TRANSIT", "QUARANTINE", "RETURNS");

    private final TenantContext tenantContext;
    private final InventoryWarehouseRepository repository;
    private final JdbcTemplate jdbc;
    private final InventoryLedgerService ledger;

    @Transactional(readOnly = true)
    public List<WarehouseResponse> list() {
        return repository.findByTenantIdOrderByDisplayOrderAsc(tenantContext.requireTenantId())
                .stream().map(WarehouseResponse::from).toList();
    }

    @Transactional
    public WarehouseResponse create(WarehouseRequest request) {
        Long tenantId = tenantContext.requireTenantId();
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (repository.existsByTenantIdAndCodeIgnoreCase(tenantId, code)) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "A warehouse with this code already exists");
        }
        validateScope(tenantId, request.companyId(), request.branchId());
        String type = warehouseType(request.warehouseType());
        boolean first = repository.findByTenantIdOrderByDisplayOrderAsc(tenantId).isEmpty();
        boolean makeDefault = first || request.defaultWarehouse();
        if (makeDefault) {
            clearWarehouseDefault(tenantId);
        }
        InventoryWarehouse warehouse = repository.saveAndFlush(InventoryWarehouse.builder()
                .tenantId(tenantId).companyId(request.companyId()).branchId(request.branchId())
                .code(code).name(request.name().trim()).description(blank(request.description()))
                .warehouseType(type).allowsNegativeStock(false).defaultWarehouse(makeDefault)
                .active(request.active()).displayOrder(request.displayOrder()).build());
        Long locationId = jdbc.queryForObject("""
                insert into inventory_locations(
                    tenant_id,warehouse_id,code,name,location_type,is_default,active)
                values(?,?, 'DEFAULT','Default Location','STORAGE',true,true) returning id
                """, Long.class, tenantId, warehouse.getId());
        ledger.audit(tenantId, "WAREHOUSE", warehouse.getId(), "CREATED", null,
                Map.of("code", warehouse.getCode(), "defaultLocationId", locationId));
        ledger.publish(tenantId, "WarehouseCreated", "WAREHOUSE", warehouse.getId(),
                Map.of("code", warehouse.getCode()));
        return WarehouseResponse.from(warehouse);
    }

    @Transactional
    public WarehouseResponse update(Long id, WarehouseRequest request) {
        Long tenantId = tenantContext.requireTenantId();
        InventoryWarehouse warehouse = require(id, tenantId);
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (repository.existsByTenantIdAndCodeIgnoreCaseAndIdNot(tenantId, code, id)) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "A warehouse with this code already exists");
        }
        validateScope(tenantId, request.companyId(), request.branchId());
        if (warehouse.isDefaultWarehouse() && !request.active()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "The default warehouse cannot be disabled");
        }
        Map<String, Object> before = snapshot(warehouse);
        if (request.defaultWarehouse() && !warehouse.isDefaultWarehouse()) {
            clearWarehouseDefault(tenantId);
            warehouse.setDefaultWarehouse(true);
        } else if (!request.defaultWarehouse() && warehouse.isDefaultWarehouse()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Choose another default warehouse before clearing this one");
        }
        warehouse.setCompanyId(request.companyId());
        warehouse.setBranchId(request.branchId());
        warehouse.setCode(code);
        warehouse.setName(request.name().trim());
        warehouse.setDescription(blank(request.description()));
        warehouse.setWarehouseType(warehouseType(request.warehouseType()));
        warehouse.setActive(request.active());
        warehouse.setDisplayOrder(request.displayOrder());
        repository.flush();
        ledger.audit(tenantId, "WAREHOUSE", id, "UPDATED", before, snapshot(warehouse));
        ledger.publish(tenantId, "WarehouseUpdated", "WAREHOUSE", id,
                Map.of("code", warehouse.getCode()));
        return WarehouseResponse.from(warehouse);
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = tenantContext.requireTenantId();
        InventoryWarehouse warehouse = require(id, tenantId);
        if (warehouse.isDefaultWarehouse()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "The default warehouse cannot be deleted");
        }
        Integer references = jdbc.queryForObject("""
                select (select count(*) from purchases where warehouse_id=?)
                     + (select count(*) from inventory_balances where warehouse_id=? and on_hand<>0)
                     + (select count(*) from inventory_movements
                        where from_warehouse_id=? or to_warehouse_id=?)
                """, Integer.class, id, id, id, id);
        if (references != null && references > 0) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Warehouse with purchasing or stock history cannot be deleted");
        }
        jdbc.update("delete from inventory_locations where tenant_id=? and warehouse_id=?",
                tenantId, id);
        repository.delete(warehouse);
        ledger.audit(tenantId, "WAREHOUSE", id, "DELETED", snapshot(warehouse), null);
        ledger.publish(tenantId, "WarehouseDeleted", "WAREHOUSE", id, Map.of());
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> locations(Long warehouseId) {
        Long tenantId = tenantContext.requireTenantId();
        require(warehouseId, tenantId);
        return jdbc.query("""
                select id,warehouse_id,code,name,location_type,description,is_default,active,
                       created_at,updated_at,version
                from inventory_locations where tenant_id=? and warehouse_id=? order by is_default desc,code
                """, (rs, row) -> location(rs), tenantId, warehouseId);
    }

    @Transactional
    public LocationResponse createLocation(Long warehouseId, LocationRequest request) {
        Long tenantId = tenantContext.requireTenantId();
        require(warehouseId, tenantId);
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        Integer duplicates = jdbc.queryForObject("""
                select count(*) from inventory_locations where tenant_id=? and warehouse_id=? and lower(code)=lower(?)
                """, Integer.class, tenantId, warehouseId, code);
        if (duplicates != null && duplicates > 0) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "A location with this code already exists in the warehouse");
        }
        if (request.defaultLocation()) {
            clearLocationDefault(tenantId, warehouseId);
        }
        Long id = jdbc.queryForObject("""
                insert into inventory_locations(
                    tenant_id,warehouse_id,code,name,location_type,description,is_default,active)
                values(?,?,?,?,?,?,?,?) returning id
                """, Long.class, tenantId, warehouseId, code, request.name().trim(),
                locationType(request.locationType()), blank(request.description()),
                request.defaultLocation(), request.active());
        ledger.audit(tenantId, "LOCATION", id, "CREATED", null,
                Map.of("warehouseId", warehouseId, "code", code));
        ledger.publish(tenantId, "LocationCreated", "LOCATION", id,
                Map.of("warehouseId", warehouseId));
        return requireLocationResponse(tenantId, warehouseId, id);
    }

    @Transactional
    public LocationResponse updateLocation(Long warehouseId, Long id, LocationRequest request) {
        Long tenantId = tenantContext.requireTenantId();
        LocationResponse before = requireLocationResponse(tenantId, warehouseId, id);
        if (before.defaultLocation() && !request.defaultLocation()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Choose another default location before clearing this one");
        }
        if (before.defaultLocation() && !request.active()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "The default location cannot be disabled");
        }
        if (request.defaultLocation() && !before.defaultLocation()) {
            clearLocationDefault(tenantId, warehouseId);
        }
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        int changed = jdbc.update("""
                update inventory_locations set code=?,name=?,location_type=?,description=?,
                    is_default=?,active=?,updated_at=now(),version=version+1
                where id=? and tenant_id=? and warehouse_id=?
                """, code, request.name().trim(), locationType(request.locationType()),
                blank(request.description()), request.defaultLocation(), request.active(),
                id, tenantId, warehouseId);
        if (changed != 1) {
            throw ResourceNotFoundException.of("Inventory location", id);
        }
        LocationResponse updated = requireLocationResponse(tenantId, warehouseId, id);
        ledger.audit(tenantId, "LOCATION", id, "UPDATED",
                Map.of("code", before.code(), "active", before.active()),
                Map.of("code", updated.code(), "active", updated.active()));
        ledger.publish(tenantId, "LocationUpdated", "LOCATION", id,
                Map.of("warehouseId", warehouseId));
        return updated;
    }

    @Transactional
    public void deleteLocation(Long warehouseId, Long id) {
        Long tenantId = tenantContext.requireTenantId();
        LocationResponse location = requireLocationResponse(tenantId, warehouseId, id);
        if (location.defaultLocation()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "The default location cannot be deleted");
        }
        Integer references = jdbc.queryForObject("""
                select (select count(*) from inventory_balances where location_id=? and on_hand<>0)
                     + (select count(*) from inventory_movements
                        where from_location_id=? or to_location_id=?)
                """, Integer.class, id, id, id);
        if (references != null && references > 0) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Location with stock or movement history cannot be deleted");
        }
        jdbc.update("delete from inventory_balances where tenant_id=? and location_id=? and on_hand=0 and reserved=0",
                tenantId, id);
        jdbc.update("delete from inventory_locations where id=? and tenant_id=? and warehouse_id=?",
                id, tenantId, warehouseId);
        ledger.audit(tenantId, "LOCATION", id, "DELETED",
                Map.of("warehouseId", warehouseId, "code", location.code()), null);
        ledger.publish(tenantId, "LocationDeleted", "LOCATION", id, Map.of());
    }

    private InventoryWarehouse require(Long id, Long tenantId) {
        return repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Warehouse", id));
    }

    private void validateScope(Long tenantId, Long companyId, Long branchId) {
        if (companyId == null && branchId == null) {
            return;
        }
        Integer count = jdbc.queryForObject("""
                select count(*) from companies c
                left join branches b on b.company_id=c.id and b.id=? and b.tenant_id=c.tenant_id
                where c.id=? and c.tenant_id=? and c.is_active
                  and (? is null or (b.id is not null and b.is_active))
                """, Integer.class, branchId, companyId, tenantId, branchId);
        if (count == null || count != 1) {
            throw new ApiException(ErrorCode.ACCESS_DENIED,
                    "Company or branch is outside the trusted tenant");
        }
    }

    private void clearWarehouseDefault(Long tenantId) {
        jdbc.update("update pur_warehouses set is_default=false,updated_at=now(),version=version+1 where tenant_id=? and is_default",
                tenantId);
    }

    private void clearLocationDefault(Long tenantId, Long warehouseId) {
        jdbc.update("""
                update inventory_locations set is_default=false,updated_at=now(),version=version+1
                where tenant_id=? and warehouse_id=? and is_default
                """, tenantId, warehouseId);
    }

    private LocationResponse requireLocationResponse(Long tenantId, Long warehouseId, Long id) {
        List<LocationResponse> rows = jdbc.query("""
                select id,warehouse_id,code,name,location_type,description,is_default,active,
                       created_at,updated_at,version
                from inventory_locations where id=? and tenant_id=? and warehouse_id=?
                """, (rs, row) -> location(rs), id, tenantId, warehouseId);
        if (rows.isEmpty()) {
            throw ResourceNotFoundException.of("Inventory location", id);
        }
        return rows.getFirst();
    }

    private LocationResponse location(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new LocationResponse(rs.getLong("id"), rs.getLong("warehouse_id"),
                rs.getString("code"), rs.getString("name"), rs.getString("location_type"),
                rs.getString("description"), rs.getBoolean("is_default"),
                rs.getBoolean("active"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(), rs.getLong("version"));
    }

    private String warehouseType(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!WAREHOUSE_TYPES.contains(normalized)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unsupported warehouse type");
        }
        return normalized;
    }

    private String locationType(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!LOCATION_TYPES.contains(normalized)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unsupported location type");
        }
        return normalized;
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Map<String, Object> snapshot(InventoryWarehouse warehouse) {
        java.util.LinkedHashMap<String, Object> values = new java.util.LinkedHashMap<>();
        values.put("code", warehouse.getCode());
        values.put("name", warehouse.getName());
        values.put("companyId", warehouse.getCompanyId());
        values.put("branchId", warehouse.getBranchId());
        values.put("warehouseType", warehouse.getWarehouseType());
        values.put("defaultWarehouse", warehouse.isDefaultWarehouse());
        values.put("active", warehouse.isActive());
        return values;
    }
}
