package com.sami.app.organization.service;

import com.sami.app.security.CurrentActor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FoundationAuditService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    public void record(Long tenantId, Long companyId, Long branchId, String type, Long id, String action, String reason, Map<String, Object> context) {
        jdbc.update("insert into foundation_audit_logs(tenant_id,company_id,branch_id,actor_id,actor_email,subject_type,subject_id,action,reason,context) values(?,?,?,?,?,?,?,?,?,?::jsonb)",
                tenantId, companyId, branchId, CurrentActor.id(), CurrentActor.email(), type, id, action, reason, json(context));
    }
    private String json(Map<String, Object> value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalArgumentException("Foundation audit context cannot be serialized", exception); }
    }
}
