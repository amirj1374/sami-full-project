package com.sami.app.inventory.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sami.app.inventory.dto.InventoryDtos.AuditResponse;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/** Append-only tenant-scoped Inventory audit writer and reader. */
@Service
@RequiredArgsConstructor
public class InventoryAuditService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Long tenantId, String entityType, Long entityId, String action,
                       Map<String, Object> oldValues, Map<String, Object> newValues) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Trusted tenant is required for Inventory audit");
        }
        jdbc.update("""
                insert into inventory_audit_log(
                    tenant_id,entity_type,entity_id,action,old_values,new_values,actor_id,actor_email)
                values(?,?,?,?,cast(? as jsonb),cast(? as jsonb),?,?)
                """, tenantId, entityType, entityId, action, json(oldValues), json(newValues),
                CurrentActor.id(), CurrentActor.email());
    }

    @Transactional(readOnly = true)
    public List<AuditResponse> list(Long tenantId, String entityType, Long entityId, int limit) {
        StringBuilder sql = new StringBuilder("""
                select id,entity_type,entity_id,action,old_values::text,new_values::text,
                       actor_id,actor_email,created_at
                from inventory_audit_log where tenant_id=?
                """);
        java.util.ArrayList<Object> args = new java.util.ArrayList<>();
        args.add(tenantId);
        if (entityType != null && !entityType.isBlank()) {
            sql.append(" and entity_type=?");
            args.add(entityType.toUpperCase(java.util.Locale.ROOT));
        }
        if (entityId != null) {
            sql.append(" and entity_id=?");
            args.add(entityId);
        }
        sql.append(" order by created_at desc,id desc limit ?");
        args.add(Math.min(Math.max(limit, 1), 500));
        return jdbc.query(sql.toString(), (rs, row) -> new AuditResponse(
                rs.getLong("id"), rs.getString("entity_type"),
                (Long) rs.getObject("entity_id"), rs.getString("action"),
                parse(rs.getString("old_values")), parse(rs.getString("new_values")),
                (Long) rs.getObject("actor_id"), rs.getString("actor_email"),
                rs.getTimestamp("created_at").toInstant()), args.toArray());
    }

    private String json(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalArgumentException("Inventory audit snapshot is not serializable", ex);
        }
    }

    private Map<String, Object> parse(String value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readValue(value, MAP_TYPE);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException("Stored Inventory audit JSON is invalid", ex);
        }
    }
}
