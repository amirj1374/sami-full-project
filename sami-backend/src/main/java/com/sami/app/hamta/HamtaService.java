package com.sami.app.hamta;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.purchasing.domain.PurchaseItemCondition;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Canonical custody and one-time delivery service for HAMTA activation codes. */
@Service
@RequiredArgsConstructor
public class HamtaService {
    private final JdbcTemplate jdbc;
    private final TenantContext tenantContext;

    @Transactional(readOnly = true)
    public Map<String, Object> settings() {
        Long tenantId = tenantContext.requireTenantId();
        boolean enabled = Boolean.TRUE.equals(jdbc.query("select enforcement_enabled from hamta_settings where tenant_id=?",
                rs -> rs.next() && rs.getBoolean(1), tenantId));
        return Map.of("enforcementEnabled", enabled);
    }

    @Transactional
    public Map<String, Object> updateSettings(boolean enabled) {
        Long tenantId = tenantContext.requireTenantId();
        jdbc.update("""
                insert into hamta_settings(tenant_id,enforcement_enabled,updated_by)
                values(?,?,?) on conflict(tenant_id) do update set enforcement_enabled=excluded.enforcement_enabled,
                updated_by=excluded.updated_by,updated_at=now(),version=hamta_settings.version+1
                """, tenantId, enabled, CurrentActor.id());
        audit(tenantId, null, null, null, "SETTINGS_UPDATED");
        return Map.of("enforcementEnabled", enabled);
    }

    @Transactional(readOnly = true)
    public boolean required(Long productId, PurchaseItemCondition condition) {
        if (condition != PurchaseItemCondition.USED) return false;
        Long tenantId = tenantContext.requireTenantId();
        Integer count = jdbc.queryForObject("""
                select count(*) from products p join hamta_settings s on s.tenant_id=p.tenant_id
                where p.tenant_id=? and p.id=? and p.hamta_eligible and s.enforcement_enabled
                """, Integer.class, tenantId, productId);
        return count != null && count > 0;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void register(Long serialUnitId, String rawCode) {
        String code = normalize(rawCode);
        if (code == null) return;
        Long tenantId = tenantContext.requireTenantId();
        Integer owned = jdbc.queryForObject("select count(*) from inventory_serial_units where tenant_id=? and id=?",
                Integer.class, tenantId, serialUnitId);
        if (owned == null || owned == 0) throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Serialized inventory unit not found");
        List<Map<String,Object>> current = jdbc.queryForList("select id,delivered,activation_code from hamta_activations where tenant_id=? and serial_unit_id=? for update", tenantId, serialUnitId);
        Long activationId;
        if (current.isEmpty()) {
            activationId = jdbc.queryForObject("""
                    insert into hamta_activations(tenant_id,serial_unit_id,activation_code,created_by,created_by_email)
                    values(?,?,?,?,?) returning id
                    """, Long.class, tenantId, serialUnitId, code, CurrentActor.id(), CurrentActor.email());
            audit(tenantId, activationId, serialUnitId, null, "CODE_REGISTERED");
        } else {
            Map<String,Object> row = current.getFirst();
            if (Boolean.TRUE.equals(row.get("delivered"))) throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "A delivered HAMTA code is immutable");
            activationId = ((Number) row.get("id")).longValue();
            if (!code.equals(row.get("activation_code"))) {
                jdbc.update("update hamta_activations set activation_code=?,updated_at=now(),version=version+1 where id=?", code, activationId);
                audit(tenantId, activationId, serialUnitId, null, "CODE_CORRECTED");
            }
        }
    }

    @Transactional
    public Map<String,Object> correctByImei(String imei, String code) {
        Map<String,Object> serial = byImei(imei, false);
        register(((Number) serial.get("serial_unit_id")).longValue(), code);
        return byImei(imei, true);
    }

    @Transactional(readOnly = true)
    public Map<String,Object> byImei(String imei, boolean includeCode) {
        Long tenantId = tenantContext.requireTenantId();
        List<Map<String,Object>> rows = jdbc.queryForList("""
                select h.id,i.id serial_unit_id,i.imei,i.serial_number,p.id product_id,p.name product_name,
                       h.activation_code,h.delivered,h.delivery_datetime,h.delivered_by_user_id,h.delivered_sale_id
                from inventory_serial_units i join products p on p.id=i.product_id
                left join hamta_activations h on h.serial_unit_id=i.id and h.tenant_id=i.tenant_id
                where i.tenant_id=? and i.imei=?
                """, tenantId, imei == null ? "" : imei.trim());
        if (rows.isEmpty()) throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Serialized inventory unit not found");
        Map<String,Object> result = new LinkedHashMap<>(rows.getFirst());
        if (!includeCode) result.remove("activation_code");
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> invoice(Long saleId) {
        requireSale(saleId, false);
        return jdbc.queryForList("""
                select s.invoice_number,si.product_name,si.product_sku,si.imei,si.serial_number,
                       h.activation_code,h.delivered,h.delivery_datetime
                from sales s join sale_items si on si.sale_id=s.id and si.tenant_id=s.tenant_id
                left join inventory_serial_units iu on iu.tenant_id=si.tenant_id and
                     ((si.imei is not null and iu.imei=si.imei) or (si.imei is null and si.serial_number is not null and iu.serial_number=si.serial_number))
                left join hamta_activations h on h.tenant_id=s.tenant_id and h.serial_unit_id=iu.id
                where s.tenant_id=? and s.id=? order by si.id
                """, tenantContext.requireTenantId(), saleId);
    }

    @Transactional
    public Map<String,Object> deliver(Long saleId) {
        Long tenantId = tenantContext.requireTenantId();
        requireSale(saleId, true);
        List<Map<String,Object>> activations = jdbc.queryForList("""
                select h.id,h.delivered from sale_items si join inventory_serial_units iu on iu.tenant_id=si.tenant_id and
                  ((si.imei is not null and iu.imei=si.imei) or (si.imei is null and si.serial_number is not null and iu.serial_number=si.serial_number))
                join hamta_activations h on h.tenant_id=si.tenant_id and h.serial_unit_id=iu.id
                where si.tenant_id=? and si.sale_id=? order by h.id for update of h
                """, tenantId, saleId);
        if (activations.isEmpty()) throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "This sale has no HAMTA activation code");
        if (activations.stream().anyMatch(row -> Boolean.TRUE.equals(row.get("delivered"))))
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "A HAMTA code for this sale has already been delivered");
        for (Map<String,Object> activation : activations) {
            Long id = ((Number)activation.get("id")).longValue();
            jdbc.update("update hamta_activations set delivered=true,delivery_datetime=now(),delivered_by_user_id=?,delivered_sale_id=?,updated_at=now(),version=version+1 where id=?",
                    CurrentActor.id(), saleId, id);
            audit(tenantId, id, null, saleId, "CODE_DELIVERED");
        }
        return Map.of("saleId", saleId, "deliveredCount", activations.size(), "deliveredAt", Instant.now());
    }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> report(Boolean delivered, Long productId) {
        Long tenantId = tenantContext.requireTenantId();
        StringBuilder sql = new StringBuilder("""
                select h.id,p.name product_name,p.sku,iu.imei,iu.serial_number,h.activation_code,h.delivered,
                       h.delivery_datetime,h.delivered_by_user_id,h.delivered_sale_id,h.created_at
                from hamta_activations h join inventory_serial_units iu on iu.id=h.serial_unit_id and iu.tenant_id=h.tenant_id
                join products p on p.id=iu.product_id and p.tenant_id=h.tenant_id
                where h.tenant_id=?
                """);
        List<Object> parameters = new ArrayList<>(List.of(tenantId));
        if (delivered != null) {
            sql.append(" and h.delivered=?");
            parameters.add(delivered);
        }
        if (productId != null) {
            sql.append(" and p.id=?");
            parameters.add(productId);
        }
        sql.append(" order by h.created_at desc");
        return jdbc.queryForList(sql.toString(), parameters.toArray());
    }

    private void requireSale(Long saleId, boolean completed) {
        String sql = "select count(*) from sales where tenant_id=? and id=?" + (completed ? " and status='COMPLETED'" : "");
        Integer count = jdbc.queryForObject(sql, Integer.class, tenantContext.requireTenantId(), saleId);
        if (count == null || count == 0) throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                completed ? "HAMTA delivery requires a completed sale" : "Sale not found");
    }

    private String normalize(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim().replaceAll("\\s+", " ");
        if (value.length() > 128) throw new ApiException(ErrorCode.VALIDATION_FAILED, "HAMTA activation code must not exceed 128 characters");
        return value;
    }

    private void audit(Long tenantId, Long activationId, Long serialUnitId, Long saleId, String action) {
        jdbc.update("insert into hamta_audit_logs(tenant_id,activation_id,serial_unit_id,sale_id,action,actor_id,actor_email) values(?,?,?,?,?,?,?)",
                tenantId, activationId, serialUnitId, saleId, action, CurrentActor.id(), CurrentActor.email());
    }
}
