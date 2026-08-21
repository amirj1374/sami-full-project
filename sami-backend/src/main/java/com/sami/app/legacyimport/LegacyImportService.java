package com.sami.app.legacyimport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.storage.FileStorage;
import com.sami.app.common.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LegacyImportService {
    private final JdbcTemplate jdbc;
    private final TenantContext tenants;
    private final FileStorage storage;
    private final ObjectMapper json;
    private final List<LegacyImportAdapter> adapters;
    private final LegacyImportProperties properties;

    @Transactional
    public Map<String,Object> upload(MultipartFile file, Long migrationGroupId) {
        if (file == null || file.isEmpty()) throw badRequest("A non-empty RAR, ZIP, or XLSX report is required");
        if (file.getSize() > properties.maxUploadBytes()) throw badRequest("Archive exceeds the configured upload limit");
        String name = file.getOriginalFilename() == null ? "legacy.rar" : file.getOriginalFilename();
        try {
            byte[] bytes = file.getBytes();
            LegacyImportAdapter adapter = selectAdapter(name, bytes);
            Long tenant = tenants.requireTenantId();
            if (migrationGroupId != null) group(migrationGroupId);
            String hash = sha256(bytes);
            String key = storage.store("legacy-imports/" + tenant, bytes, adapter.mediaType());
            try {
                Long id = jdbc.queryForObject("""
                    INSERT INTO legacy_import_batches(tenant_id,migration_group_id,source_system,evidence_type,original_filename,storage_key,sha256,parser_version,status,metadata)
                    VALUES (?,?,?,?,?,?,?,?,'UPLOADED',cast(? as jsonb)) RETURNING id
                    """, Long.class, tenant, migrationGroupId, adapter.sourceSystem(), evidenceType(adapter), basename(name), key, hash, adapter.parserVersion(),
                        json(Map.of("uploadBytes", bytes.length, "stagingOnly", true)));
                audit(tenant, id, "UPLOAD", Map.of("sha256", hash, "filename", basename(name), "evidenceType", evidenceType(adapter)));
                return batch(id);
            } catch (DuplicateKeyException ex) {
                storage.delete(key);
                throw badRequest("This archive has already been uploaded for the current tenant");
            }
        } catch (IOException e) { throw badRequest("Archive upload could not be read"); }
    }

    public Map<String,Object> upload(MultipartFile file) { return upload(file, null); }

    @Transactional
    public Map<String,Object> createGroup(String name) {
        Long tenant = tenants.requireTenantId();
        String safeName = name == null || name.isBlank() ? "Asan accounting migration" : name.trim();
        Long id = jdbc.queryForObject("INSERT INTO legacy_migration_groups(tenant_id,name,status) VALUES (?,?,'ACTIVE') RETURNING id", Long.class, tenant, safeName);
        return group(id);
    }
    public List<Map<String,Object>> groups() { return jdbc.queryForList("SELECT id,name,status,acceptance_status,created_at,updated_at FROM legacy_migration_groups WHERE tenant_id=? ORDER BY created_at DESC", tenants.requireTenantId()); }
    public Map<String,Object> group(Long id) { return jdbc.queryForMap("SELECT id,name,status,acceptance_status,metadata,created_at,updated_at FROM legacy_migration_groups WHERE tenant_id=? AND id=?", tenants.requireTenantId(), id); }

    public List<Map<String,Object>> list() {
        return jdbc.queryForList("SELECT id,migration_group_id,source_system,evidence_type,original_filename,sha256,status,dataset_count,record_count,warning_count,error_count,created_at,completed_at FROM legacy_import_batches WHERE tenant_id=? ORDER BY created_at DESC", tenants.requireTenantId());
    }

    public Map<String,Object> batch(Long id) {
        return jdbc.queryForMap("SELECT id,migration_group_id,source_system,evidence_type,original_filename,sha256,parser_version,status,dataset_count,record_count,warning_count,error_count,metadata,created_at,started_at,completed_at FROM legacy_import_batches WHERE tenant_id=? AND id=?", tenants.requireTenantId(), id);
    }

    @Transactional
    public Map<String,Object> analyze(Long id) {
        Long tenant = tenants.requireTenantId();
        Map<String,Object> batch = batch(id);
        String status = String.valueOf(batch.get("status"));
        if (!List.of("UPLOADED","READY","FAILED").contains(status)) throw new IllegalStateException("Batch cannot be analyzed in status " + status);
        jdbc.update("UPDATE legacy_import_batches SET status='ANALYZING',started_at=now(),updated_at=now() WHERE tenant_id=? AND id=?", tenant, id);
        try {
            byte[] bytes = storage.load(storageKey(tenant,id)).orElseThrow(() -> new IllegalStateException("Stored archive is unavailable")).content();
            LegacyImportAdapter adapter = selectAdapter(String.valueOf(batch.get("original_filename")), bytes);
            LegacyImportAdapter.Analysis analysis = adapter.analyze(String.valueOf(batch.get("original_filename")), bytes, false);
            replaceAnalysis(tenant, id, analysis, false);
            jdbc.update("UPDATE legacy_import_batches SET status='READY',dataset_count=?,warning_count=?,error_count=?,metadata=cast(? as jsonb),updated_at=now() WHERE tenant_id=? AND id=?",
                    analysis.datasets().size(), severity(analysis,"WARNING"), severity(analysis,"ERROR"), json(summary(analysis)), tenant, id);
            audit(tenant,id,"ANALYZE",summary(analysis));
            return batch(id);
        } catch (RuntimeException ex) {
            jdbc.update("UPDATE legacy_import_batches SET status='FAILED',error_count=error_count+1,updated_at=now() WHERE tenant_id=? AND id=?",tenant,id);
            throw ex;
        }
    }

    @Transactional
    public Map<String,Object> execute(Long id) {
        Long tenant = tenants.requireTenantId();
        if (!"READY".equals(String.valueOf(batch(id).get("status")))) throw new IllegalStateException("Only an analyzed READY batch can be imported");
        jdbc.update("UPDATE legacy_import_batches SET status='IMPORTING',updated_at=now() WHERE tenant_id=? AND id=?",tenant,id);
        try {
            byte[] bytes = storage.load(storageKey(tenant,id)).orElseThrow(() -> new IllegalStateException("Stored archive is unavailable")).content();
            Map<String,Object> current = batch(id);
            LegacyImportAdapter adapter = selectAdapter(String.valueOf(current.get("original_filename")), bytes);
            LegacyImportAdapter.Analysis analysis = adapter.analyze(String.valueOf(current.get("original_filename")), bytes, true);
            replaceAnalysis(tenant,id,analysis,true);
            long count = analysis.datasets().stream().mapToLong(d -> d.records().size()).sum();
            int warnings = severity(analysis,"WARNING"), errors = severity(analysis,"ERROR");
            String completed = warnings > 0 || errors > 0 ? "COMPLETED_WITH_WARNINGS" : "COMPLETED";
            jdbc.update("UPDATE legacy_import_batches SET status=?,record_count=?,dataset_count=?,warning_count=?,error_count=?,completed_at=now(),updated_at=now() WHERE tenant_id=? AND id=?",completed,count,analysis.datasets().size(),warnings,errors,tenant,id);
            audit(tenant,id,"IMPORT",Map.of("stagedRecords",count,"canonicalWrites",0));
            return batch(id);
        } catch (RuntimeException ex) {
            jdbc.update("UPDATE legacy_import_batches SET status='FAILED',error_count=error_count+1,updated_at=now() WHERE tenant_id=? AND id=?",tenant,id);
            throw ex;
        }
    }

    public List<Map<String,Object>> files(Long id) { batch(id); return jdbc.queryForList("SELECT id,source_path,safe_name,size_bytes,sha256,format,support_status,metadata FROM legacy_import_files WHERE tenant_id=? AND import_batch_id=? ORDER BY id",tenants.requireTenantId(),id); }
    public List<Map<String,Object>> datasets(Long id) { batch(id); return jdbc.queryForList("SELECT id,dataset_key,source_table,semantic_type,support_status,source_encoding,source_record_count,imported_record_count,field_dictionary,metadata FROM legacy_datasets WHERE tenant_id=? AND import_batch_id=? ORDER BY id",tenants.requireTenantId(),id); }
    public List<Map<String,Object>> messages(Long id) { batch(id); return jdbc.queryForList("SELECT severity,code,message,source_record_id,context,created_at FROM legacy_import_messages WHERE tenant_id=? AND import_batch_id=? ORDER BY id",tenants.requireTenantId(),id); }
    public List<Map<String,Object>> records(Long id, int limit, int offset) { batch(id); return jdbc.queryForList("SELECT r.id,d.dataset_key,d.semantic_type,r.source_record_id,r.legacy_code,r.raw_record,r.normalized_record FROM legacy_records r JOIN legacy_datasets d ON d.id=r.dataset_id AND d.tenant_id=r.tenant_id WHERE r.tenant_id=? AND r.import_batch_id=? ORDER BY r.id LIMIT ? OFFSET ?",tenants.requireTenantId(),id,Math.min(Math.max(limit,1),200),Math.max(offset,0)); }

    @Transactional
    public Map<String,Object> confirmDatasetType(Long batchId, Long datasetId, String reportType) {
        Long tenant = tenants.requireTenantId(); batch(batchId);
        if (!List.of("GENERAL_LEDGER","SUBSIDIARY_LEDGER","DETAILED_LEDGER").contains(reportType)) throw badRequest("Unsupported accounting report type");
        int updated = jdbc.update("UPDATE legacy_datasets SET semantic_type=?, metadata=jsonb_set(metadata,'{reportTypeConfirmed}', 'true'::jsonb), updated_at=now() WHERE tenant_id=? AND import_batch_id=? AND id=?", reportType, tenant, batchId, datasetId);
        if (updated == 0) throw badRequest("Dataset is outside this import batch");
        audit(tenant, batchId, "CONFIRM_REPORT_TYPE", Map.of("datasetId", datasetId, "reportType", reportType));
        return jdbc.queryForMap("SELECT id,dataset_key,semantic_type,metadata FROM legacy_datasets WHERE tenant_id=? AND id=?", tenant, datasetId);
    }

    @Transactional
    public Map<String,Object> reconcileGroup(Long groupId) {
        Long tenant = tenants.requireTenantId(); group(groupId);
        jdbc.update("UPDATE legacy_migration_groups SET status='RECONCILING',updated_at=now() WHERE tenant_id=? AND id=?", tenant, groupId);
        List<Map<String,Object>> datasets = jdbc.queryForList("SELECT d.semantic_type,count(r.id) AS records FROM legacy_datasets d JOIN legacy_import_batches b ON b.id=d.import_batch_id AND b.tenant_id=d.tenant_id LEFT JOIN legacy_records r ON r.dataset_id=d.id AND r.tenant_id=d.tenant_id WHERE d.tenant_id=? AND b.migration_group_id=? GROUP BY d.semantic_type", tenant, groupId);
        BigDecimal debit = amount(groupId, tenant, "debit"); BigDecimal credit = amount(groupId, tenant, "credit");
        long journals = datasetRecords(groupId, tenant, "ACCOUNTING_JOURNAL"); long trial = datasetRecords(groupId, tenant, "TRIAL_BALANCE"); long cheques = datasetRecords(groupId, tenant, "CHEQUE_RECEIVABLE") + datasetRecords(groupId, tenant, "CHEQUE_PAYABLE");
        Map<String,Object> summary = new LinkedHashMap<>(); summary.put("datasets", datasets); summary.put("journalRows", journals); summary.put("journalDebit", debit); summary.put("journalCredit", credit); summary.put("journalDifference", debit.subtract(credit)); summary.put("trialBalanceRows", trial); summary.put("chequeRows", cheques); summary.put("canonicalAccountingAvailable", false); summary.put("stagingOnly", true);
        upsertCheck(tenant, groupId, "JOURNAL_DEBIT_CREDIT", journals == 0 ? "PENDING" : debit.compareTo(credit) == 0 ? "PASS" : "FAIL", Map.of("debit",debit,"credit",credit));
        upsertCheck(tenant, groupId, "TRIAL_BALANCE_STAGED", trial > 0 ? "PASS" : "PENDING", Map.of("rows",trial));
        upsertCheck(tenant, groupId, "CHEQUES_STAGED", cheques > 0 ? "PASS" : "WARNING", Map.of("rows",cheques));
        upsertCheck(tenant, groupId, "CANONICAL_FINAL_IMPORT", "PENDING", Map.of("reason","Canonical accounting, bank, and cheque owners are not implemented; final import remains fail-closed."));
        upsertKnownBankTimingException(tenant, groupId);
        jdbc.update("UPDATE legacy_migration_groups SET status='READY_FOR_ACCEPTANCE',acceptance_status='BLOCKED',metadata=cast(? as jsonb),updated_at=now() WHERE tenant_id=? AND id=?", json(summary), tenant, groupId);
        auditGroup(tenant, groupId, "RECONCILE", Map.of("journalRows", journals, "chequeRows", cheques, "canonicalWrites", 0));
        return reconciliation(groupId);
    }

    public Map<String,Object> reconciliation(Long groupId) {
        Long tenant=tenants.requireTenantId(); Map<String,Object> group=group(groupId); Map<String,Object> result=new LinkedHashMap<>(group);
        result.put("checks", jdbc.queryForList("SELECT id,check_code,status,evidence,reviewed_at FROM legacy_migration_acceptance_checks WHERE tenant_id=? AND migration_group_id=? ORDER BY check_code",tenant,groupId));
        result.put("exceptions", jdbc.queryForList("SELECT id,domain,source_key,classification,legacy_value,sami_value,difference_value,explanation,source_evidence,approval_status,reviewed_at FROM legacy_reconciliation_exceptions WHERE tenant_id=? AND migration_group_id=? ORDER BY domain,source_key",tenant,groupId));
        result.put("batches", jdbc.queryForList("SELECT id,original_filename,evidence_type,status,record_count,warning_count,error_count FROM legacy_import_batches WHERE tenant_id=? AND migration_group_id=? ORDER BY created_at",tenant,groupId));
        return result;
    }

    @Transactional
    public Map<String,Object> reviewException(Long groupId, Long exceptionId, String approvalStatus, String explanation) {
        Long tenant=tenants.requireTenantId(); group(groupId);
        if (!List.of("NEEDS_INVESTIGATION","EXPLAINED","ACCEPTED","REJECTED").contains(approvalStatus)) throw badRequest("Unsupported reconciliation approval status");
        int updated=jdbc.update("UPDATE legacy_reconciliation_exceptions SET approval_status=?,explanation=?,reviewed_at=now(),updated_at=now() WHERE tenant_id=? AND migration_group_id=? AND id=?",approvalStatus,explanation,tenant,groupId,exceptionId);
        if(updated==0) throw badRequest("Reconciliation exception is outside this migration group");
        auditGroup(tenant,groupId,"REVIEW_EXCEPTION",Map.of("exceptionId",exceptionId,"approvalStatus",approvalStatus));
        return jdbc.queryForMap("SELECT id,approval_status,explanation,reviewed_at FROM legacy_reconciliation_exceptions WHERE tenant_id=? AND id=?",tenant,exceptionId);
    }

    @Transactional
    public Map<String,Object> compare(Long id) {
        Long tenant = tenants.requireTenantId(); Map<String,Object> batch = batch(id);
        long started = System.nanoTime();
        String comparisonVersion = String.valueOf(batch.getOrDefault("parser_version", "legacy-1.0"));
        Long run = jdbc.queryForObject("INSERT INTO legacy_comparison_runs(tenant_id,import_batch_id,status,comparison_version) VALUES (?,?,'RUNNING',?) RETURNING id",Long.class,tenant,id,comparisonVersion);
        long total = jdbc.queryForObject("SELECT count(*) FROM legacy_records WHERE tenant_id=? AND import_batch_id=?",Long.class,tenant,id);
        long parties = count(id, tenant, "d.semantic_type IN ('CUSTOMER','PARTY')");
        long products = count(id, tenant, "d.semantic_type IN ('PRODUCT_MOVEMENT','INVENTORY_BALANCE')");
        long customerMatches = count(id, tenant, "d.semantic_type IN ('CUSTOMER','PARTY') AND EXISTS (SELECT 1 FROM customers c WHERE c.tenant_id=r.tenant_id AND c.customer_code=r.legacy_code)");
        long supplierMatches = count(id, tenant, "d.semantic_type='PARTY' AND EXISTS (SELECT 1 FROM suppliers s WHERE s.tenant_id=r.tenant_id AND s.supplier_code=r.legacy_code)");
        long ambiguousParties = count(id, tenant, "d.semantic_type='PARTY' AND EXISTS (SELECT 1 FROM customers c WHERE c.tenant_id=r.tenant_id AND c.customer_code=r.legacy_code) AND EXISTS (SELECT 1 FROM suppliers s WHERE s.tenant_id=r.tenant_id AND s.supplier_code=r.legacy_code)");
        long productMatches = count(id, tenant, "d.semantic_type IN ('PRODUCT_MOVEMENT','INVENTORY_BALANCE') AND EXISTS (SELECT 1 FROM products p WHERE p.tenant_id=r.tenant_id AND lower(p.sku)=lower(r.legacy_code))");
        long matched = customerMatches + supplierMatches - ambiguousParties + productMatches;
        Map<String,Object> counts = new LinkedHashMap<>();
        counts.put("total", total); counts.put("eligibleParties", parties); counts.put("eligibleProductRows", products);
        counts.put("matchedByCustomerCode", customerMatches); counts.put("matchedBySupplierCode", supplierMatches);
        counts.put("matchedByProductSku", productMatches); counts.put("ambiguousParties", ambiguousParties);
        counts.put("unmapped", Math.max(0, total - matched));
        List<Map<String,Object>> results=jdbc.queryForList("""
            SELECT r.id AS legacy_record_id,d.dataset_key,d.semantic_type,r.source_record_id,r.legacy_code,
                   CASE
                     WHEN c.id IS NOT NULL AND s.id IS NOT NULL THEN 'AMBIGUOUS'
                     WHEN c.id IS NOT NULL OR s.id IS NOT NULL OR p.id IS NOT NULL THEN 'MATCHED'
                     ELSE 'UNMAPPED'
                   END AS classification,
                   c.id AS sami_customer_id,s.id AS sami_supplier_id,p.id AS sami_product_id,
                   CASE
                     WHEN c.id IS NOT NULL AND s.id IS NOT NULL THEN 'CUSTOMER_AND_SUPPLIER_CODE_COLLISION'
                     WHEN c.id IS NOT NULL THEN 'EXACT_CUSTOMER_CODE'
                     WHEN s.id IS NOT NULL THEN 'EXACT_SUPPLIER_CODE'
                     WHEN p.id IS NOT NULL THEN 'EXACT_PRODUCT_SKU'
                     ELSE 'NO_APPROVED_MATCH_RULE'
                   END AS reason
            FROM legacy_records r JOIN legacy_datasets d ON d.id=r.dataset_id AND d.tenant_id=r.tenant_id
            LEFT JOIN customers c ON d.semantic_type IN ('CUSTOMER','PARTY') AND c.tenant_id=r.tenant_id AND c.customer_code=r.legacy_code
            LEFT JOIN suppliers s ON d.semantic_type='PARTY' AND s.tenant_id=r.tenant_id AND s.supplier_code=r.legacy_code
            LEFT JOIN products p ON d.semantic_type IN ('PRODUCT_MOVEMENT','INVENTORY_BALANCE') AND p.tenant_id=r.tenant_id AND lower(p.sku)=lower(r.legacy_code)
            WHERE r.tenant_id=? AND r.import_batch_id=? ORDER BY r.id LIMIT 200
            """,tenant,id);
        long duration = (System.nanoTime()-started)/1_000_000;
        jdbc.update("UPDATE legacy_comparison_runs SET status='COMPLETED',counts=cast(? as jsonb),duration_ms=?,completed_at=now(),updated_at=now() WHERE tenant_id=? AND id=?",json(counts),duration,tenant,run);
        audit(tenant,id,"COMPARE",counts);
        Map<String,Object> result = new LinkedHashMap<>(counts); result.put("runId",run); result.put("durationMs",duration); result.put("matchingPolicy","Exact tenant-scoped customer/supplier codes and product SKUs only; accounting and inventory totals remain staging evidence until canonical mappings are approved"); result.put("results",results); return result;
    }

    @Transactional
    public Map<String,Object> review(Long batchId, Long runId, Long recordId, String classification, String note) {
        Long tenant=tenants.requireTenantId(); batch(batchId);
        if (!List.of("CONFIRMED_MATCH","EXPECTED_DIFFERENCE","NEEDS_INVESTIGATION","SAMI_ISSUE","LEGACY_DATA_ISSUE").contains(classification)) throw new IllegalArgumentException("Unsupported review classification");
        Integer valid=jdbc.queryForObject("SELECT count(*) FROM legacy_comparison_runs c JOIN legacy_records r ON r.import_batch_id=c.import_batch_id AND r.tenant_id=c.tenant_id WHERE c.tenant_id=? AND c.import_batch_id=? AND c.id=? AND r.id=?",Integer.class,tenant,batchId,runId,recordId);
        if (valid==null || valid==0) throw new IllegalArgumentException("Comparison record is outside this batch");
        Long id=jdbc.queryForObject("""
            INSERT INTO legacy_comparison_reviews(tenant_id,comparison_run_id,legacy_record_id,classification,note)
            VALUES (?,?,?,?,?) ON CONFLICT(comparison_run_id,legacy_record_id) DO UPDATE SET classification=excluded.classification,note=excluded.note,reviewed_at=now(),updated_at=now()
            RETURNING id
            """,Long.class,tenant,runId,recordId,classification,note);
        audit(tenant,batchId,"REVIEW_COMPARISON",Map.of("reviewId",id,"recordId",recordId,"classification",classification));
        return Map.of("id",id,"comparisonRunId",runId,"legacyRecordId",recordId,"classification",classification,"note",note==null?"":note);
    }

    @Transactional
    public void delete(Long id) { Long tenant=tenants.requireTenantId(); String key=storageKey(tenant,id); jdbc.update("DELETE FROM legacy_import_batches WHERE tenant_id=? AND id=?",tenant,id); storage.delete(key); }

    private void replaceAnalysis(Long tenant, Long batchId, LegacyImportAdapter.Analysis analysis, boolean includeRecords) {
        jdbc.update("DELETE FROM legacy_import_files WHERE tenant_id=? AND import_batch_id=?",tenant,batchId);
        Map<String,Long> fileIds = new LinkedHashMap<>();
        for (var f:analysis.files()) {
            Long fileId=jdbc.queryForObject("INSERT INTO legacy_import_files(tenant_id,import_batch_id,source_path,safe_name,extension,size_bytes,sha256,format,support_status,metadata) VALUES (?,?,?,?,?,?,?,?,?,cast(? as jsonb)) RETURNING id",Long.class,tenant,batchId,f.sourcePath(),f.safeName(),extension(f.safeName()),f.size(),f.sha256(),f.format(),f.supportStatus(),json(f.metadata()));
            fileIds.put(f.sourcePath(),fileId);
        }
        for (var d:analysis.datasets()) {
            Long datasetId=jdbc.queryForObject("INSERT INTO legacy_datasets(tenant_id,import_batch_id,import_file_id,dataset_key,source_table,semantic_type,support_status,source_encoding,source_record_count,imported_record_count,field_dictionary,metadata) VALUES (?,?,?,?,?,?,?,?,?,?,cast(? as jsonb),cast(? as jsonb)) RETURNING id",Long.class,tenant,batchId,fileIds.get(d.sourcePath()),d.key(),d.sourceTable(),d.semanticType(),d.supportStatus(),d.encoding(),d.sourceCount(),includeRecords?d.records().size():0,json(d.dictionary()),json(d.metadata()));
            if (includeRecords) insertRecords(tenant, batchId, datasetId, d.records());
        }
        for (var m:analysis.messages()) jdbc.update("INSERT INTO legacy_import_messages(tenant_id,import_batch_id,import_file_id,severity,code,message) VALUES (?,?,?,?,?,?)",tenant,batchId,fileIds.get(m.sourcePath()),m.severity(),m.code(),m.message());
    }

    private String storageKey(Long tenant,Long id) { return jdbc.queryForObject("SELECT storage_key FROM legacy_import_batches WHERE tenant_id=? AND id=?",String.class,tenant,id); }
    private void audit(Long tenant,Long batch,String action,Map<String,?> detail) { jdbc.update("INSERT INTO legacy_import_audit_logs(tenant_id,import_batch_id,action,detail) VALUES (?,?,?,cast(? as jsonb))",tenant,batch,action,json(detail)); }
    private String json(Object value) { try { return json.writeValueAsString(value); } catch (JsonProcessingException e) { throw new IllegalStateException(e); } }
    private static int severity(LegacyImportAdapter.Analysis a,String s) { return (int)a.messages().stream().filter(m->s.equals(m.severity())).count(); }
    private Map<String,Object> summary(LegacyImportAdapter.Analysis a) {
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("fileCount",a.files().size()); result.put("datasetCount",a.datasets().size());
        result.put("supportedFiles",a.files().stream().filter(f->"SUPPORTED".equals(f.supportStatus())).count());
        result.put("partialFiles",a.files().stream().filter(f->"PARTIAL".equals(f.supportStatus())).count());
        result.put("unsupportedFiles",a.files().stream().filter(f->"UNSUPPORTED".equals(f.supportStatus())).count());
        result.put("stagingOnly",true); result.put("canonicalWrites",0);
        result.put("datasetControls",a.datasets().stream().collect(java.util.stream.Collectors.toMap(
                LegacyImportAdapter.Dataset::key, LegacyImportAdapter.Dataset::metadata, (left,right)->left, LinkedHashMap::new)));
        return result;
    }

    private LegacyImportAdapter selectAdapter(String filename, byte[] bytes) {
        return adapters.stream().filter(candidate -> candidate.supports(filename, bytes)).findFirst()
                .orElseThrow(() -> badRequest("Only valid Asan RAR archives or manifest-driven ZIP/Excel packages are accepted"));
    }

    private BigDecimal amount(Long group, Long tenant, String field) { BigDecimal value=jdbc.queryForObject("SELECT coalesce(sum(coalesce(nullif(r.normalized_record->>?,'')::numeric,0)),0) FROM legacy_records r JOIN legacy_import_batches b ON b.id=r.import_batch_id AND b.tenant_id=r.tenant_id WHERE r.tenant_id=? AND b.migration_group_id=?",BigDecimal.class,field,tenant,group); return value==null?BigDecimal.ZERO:value; }
    private long datasetRecords(Long group, Long tenant, String type) { Long value=jdbc.queryForObject("SELECT count(r.id) FROM legacy_records r JOIN legacy_datasets d ON d.id=r.dataset_id AND d.tenant_id=r.tenant_id JOIN legacy_import_batches b ON b.id=r.import_batch_id AND b.tenant_id=r.tenant_id WHERE r.tenant_id=? AND b.migration_group_id=? AND d.semantic_type=?",Long.class,tenant,group,type); return value==null?0:value; }
    private void upsertCheck(Long tenant,Long group,String code,String status,Map<String,?> evidence) { jdbc.update("INSERT INTO legacy_migration_acceptance_checks(tenant_id,migration_group_id,check_code,status,evidence) VALUES (?,?,?,?,cast(? as jsonb)) ON CONFLICT(migration_group_id,check_code) DO UPDATE SET status=excluded.status,evidence=excluded.evidence,updated_at=now()",tenant,group,code,status,json(evidence)); }
    private void upsertKnownBankTimingException(Long tenant,Long group) { jdbc.update("INSERT INTO legacy_reconciliation_exceptions(tenant_id,migration_group_id,domain,source_key,classification,legacy_value,difference_value,explanation,source_evidence,approval_status) VALUES (?,?, 'BANK','HANDOFF_BANK_TIMING_699837000','EXPLAINED_DIFFERENCE',cast(? as jsonb),cast(? as jsonb),?,cast(? as jsonb),'EXPLAINED') ON CONFLICT(migration_group_id,domain,source_key) DO NOTHING",tenant,group,json(Map.of("amount",new BigDecimal("699837000"),"currency","IRR")),json(Map.of("amount",new BigDecimal("699837000"),"currency","IRR")),"Documented Asan extraction timing difference: 700,000,000 receipt less 163,000 bank charge.",json(Map.of("source","SAMI_ERP_Accounting_Migration_Handoff.xlsx","rule","اختلاف بانک"))); }
    private void auditGroup(Long tenant,Long group,String action,Map<String,?> detail) { List<Long> batches=jdbc.query("SELECT id FROM legacy_import_batches WHERE tenant_id=? AND migration_group_id=? ORDER BY id LIMIT 1",(rs,n)->rs.getLong(1),tenant,group); if(!batches.isEmpty())audit(tenant,batches.getFirst(),action,detail); }
    private static String evidenceType(LegacyImportAdapter adapter) { return adapter instanceof AsanAccountingExcelAdapter ? "ASAN_EXCEL_REPORT" : "ASAN_BACKUP"; }

    private long count(Long batchId, Long tenant, String predicate) {
        String sql = "SELECT count(*) FROM legacy_records r JOIN legacy_datasets d ON d.id=r.dataset_id AND d.tenant_id=r.tenant_id WHERE r.tenant_id=? AND r.import_batch_id=? AND " + predicate;
        Long value = jdbc.queryForObject(sql, Long.class, tenant, batchId);
        return value == null ? 0 : value;
    }

    private void insertRecords(Long tenant, Long batchId, Long datasetId, List<LegacyImportAdapter.Record> records) {
        String sql = "INSERT INTO legacy_records(tenant_id,import_batch_id,dataset_id,source_record_id,legacy_code,normalized_key,raw_record,normalized_record) VALUES (?,?,?,?,?,?,cast(? as jsonb),cast(? as jsonb))";
        int chunkSize = properties.importChunkSize();
        List<Object[]> chunk = new ArrayList<>(chunkSize);
        for (var record : records) {
            chunk.add(new Object[]{tenant,batchId,datasetId,record.sourceId(),record.legacyCode(),record.normalizedKey(),json(record.raw()),json(record.normalized())});
            if (chunk.size() == chunkSize) { jdbc.batchUpdate(sql, chunk); chunk.clear(); }
        }
        if (!chunk.isEmpty()) jdbc.batchUpdate(sql, chunk);
    }
    private static String extension(String n) { int i=n.lastIndexOf('.'); return i<0?null:n.substring(i+1).toLowerCase(); }
    private static ApiException badRequest(String message) { return new ApiException(ErrorCode.BAD_REQUEST, message); }
    private static String basename(String n) { String clean=n.replace('\\','/'); return clean.substring(clean.lastIndexOf('/')+1); }
    private static String sha256(byte[] bytes) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); } catch(Exception e){throw new IllegalStateException(e);} }
}
