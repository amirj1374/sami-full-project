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
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LegacyImportService {
    private final JdbcTemplate jdbc;
    private final TenantContext tenants;
    private final FileStorage storage;
    private final ObjectMapper json;
    private final AsanLegacyImportAdapter adapter;
    private final LegacyImportProperties properties;

    @Transactional
    public Map<String,Object> upload(MultipartFile file) {
        if (file == null || file.isEmpty()) throw badRequest("A non-empty RAR archive is required");
        if (file.getSize() > properties.maxUploadBytes()) throw badRequest("Archive exceeds the configured upload limit");
        String name = file.getOriginalFilename() == null ? "legacy.rar" : file.getOriginalFilename();
        if (!name.toLowerCase().endsWith(".rar")) throw badRequest("Only .rar archives are accepted");
        try {
            byte[] bytes = file.getBytes();
            try {
                AsanLegacyImportAdapter.requireRarSignature(bytes);
            } catch (IllegalArgumentException ex) {
                throw badRequest("Only valid RAR archives are accepted");
            }
            Long tenant = tenants.requireTenantId();
            String hash = sha256(bytes);
            String key = storage.store("legacy-imports/" + tenant, bytes, "application/vnd.rar");
            try {
                Long id = jdbc.queryForObject("""
                    INSERT INTO legacy_import_batches(tenant_id,source_system,original_filename,storage_key,sha256,parser_version,status,metadata)
                    VALUES (?,?,?,?,?,?,'UPLOADED',cast(? as jsonb)) RETURNING id
                    """, Long.class, tenant, adapter.sourceSystem(), basename(name), key, hash, AsanLegacyImportAdapter.PARSER_VERSION,
                        json(Map.of("uploadBytes", bytes.length)));
                audit(tenant, id, "UPLOAD", Map.of("sha256", hash, "filename", basename(name)));
                return batch(id);
            } catch (DuplicateKeyException ex) {
                storage.delete(key);
                throw badRequest("This archive has already been uploaded for the current tenant");
            }
        } catch (IOException e) { throw badRequest("Archive upload could not be read"); }
    }

    public List<Map<String,Object>> list() {
        return jdbc.queryForList("SELECT id,source_system,original_filename,sha256,status,dataset_count,record_count,warning_count,error_count,created_at,completed_at FROM legacy_import_batches WHERE tenant_id=? ORDER BY created_at DESC", tenants.requireTenantId());
    }

    public Map<String,Object> batch(Long id) {
        return jdbc.queryForMap("SELECT id,source_system,original_filename,sha256,parser_version,status,dataset_count,record_count,warning_count,error_count,metadata,created_at,started_at,completed_at FROM legacy_import_batches WHERE tenant_id=? AND id=?", tenants.requireTenantId(), id);
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
            LegacyImportAdapter.Analysis analysis = adapter.analyze(bytes, false);
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
            LegacyImportAdapter.Analysis analysis = adapter.analyze(bytes, true);
            replaceAnalysis(tenant,id,analysis,true);
            long count = analysis.datasets().stream().mapToLong(d -> d.records().size()).sum();
            int warnings = severity(analysis,"WARNING"), errors = severity(analysis,"ERROR");
            String completed = warnings > 0 ? "COMPLETED_WITH_WARNINGS" : "COMPLETED";
            jdbc.update("UPDATE legacy_import_batches SET status=?,record_count=?,dataset_count=?,warning_count=?,error_count=?,completed_at=now(),updated_at=now() WHERE tenant_id=? AND id=?",completed,count,analysis.datasets().size(),warnings,errors,tenant,id);
            audit(tenant,id,"IMPORT",Map.of("stagedRecords",count,"canonicalWrites",0));
            return batch(id);
        } catch (RuntimeException ex) {
            jdbc.update("UPDATE legacy_import_batches SET status='FAILED',error_count=error_count+1,updated_at=now() WHERE tenant_id=? AND id=?",tenant,id);
            throw ex;
        }
    }

    public List<Map<String,Object>> files(Long id) { batch(id); return jdbc.queryForList("SELECT id,source_path,safe_name,size_bytes,sha256,format,support_status,metadata FROM legacy_import_files WHERE tenant_id=? AND import_batch_id=? ORDER BY id",tenants.requireTenantId(),id); }
    public List<Map<String,Object>> datasets(Long id) { batch(id); return jdbc.queryForList("SELECT id,dataset_key,source_table,semantic_type,support_status,source_encoding,source_record_count,imported_record_count,field_dictionary FROM legacy_datasets WHERE tenant_id=? AND import_batch_id=? ORDER BY id",tenants.requireTenantId(),id); }
    public List<Map<String,Object>> messages(Long id) { batch(id); return jdbc.queryForList("SELECT severity,code,message,source_record_id,context,created_at FROM legacy_import_messages WHERE tenant_id=? AND import_batch_id=? ORDER BY id",tenants.requireTenantId(),id); }
    public List<Map<String,Object>> records(Long id, int limit, int offset) { batch(id); return jdbc.queryForList("SELECT r.id,d.dataset_key,d.semantic_type,r.source_record_id,r.legacy_code,r.raw_record,r.normalized_record FROM legacy_records r JOIN legacy_datasets d ON d.id=r.dataset_id AND d.tenant_id=r.tenant_id WHERE r.tenant_id=? AND r.import_batch_id=? ORDER BY r.id LIMIT ? OFFSET ?",tenants.requireTenantId(),id,Math.min(Math.max(limit,1),200),Math.max(offset,0)); }

    @Transactional
    public Map<String,Object> compare(Long id) {
        Long tenant = tenants.requireTenantId(); batch(id);
        long started = System.nanoTime();
        Long run = jdbc.queryForObject("INSERT INTO legacy_comparison_runs(tenant_id,import_batch_id,status,comparison_version) VALUES (?,?,'RUNNING','asan-1.0') RETURNING id",Long.class,tenant,id);
        long total = jdbc.queryForObject("SELECT count(*) FROM legacy_records WHERE tenant_id=? AND import_batch_id=?",Long.class,tenant,id);
        long customer = jdbc.queryForObject("SELECT count(*) FROM legacy_records r JOIN legacy_datasets d ON d.id=r.dataset_id AND d.tenant_id=r.tenant_id WHERE r.tenant_id=? AND r.import_batch_id=? AND d.semantic_type='CUSTOMER'",Long.class,tenant,id);
        long exactCode = jdbc.queryForObject("SELECT count(*) FROM legacy_records r JOIN legacy_datasets d ON d.id=r.dataset_id AND d.tenant_id=r.tenant_id JOIN customers c ON c.tenant_id=r.tenant_id AND c.customer_code=r.legacy_code WHERE r.tenant_id=? AND r.import_batch_id=? AND d.semantic_type='CUSTOMER'",Long.class,tenant,id);
        Map<String,Object> counts = Map.of("total",total,"eligibleCustomers",customer,"matchedByCustomerCode",exactCode,"unmapped",total-exactCode,"ambiguous",0);
        List<Map<String,Object>> results=jdbc.queryForList("""
            SELECT r.id AS legacy_record_id,d.dataset_key,d.semantic_type,r.source_record_id,r.legacy_code,
                   CASE WHEN c.id IS NOT NULL THEN 'MATCHED' ELSE 'UNMAPPED' END AS classification,
                   c.id AS sami_customer_id,
                   CASE WHEN c.id IS NOT NULL THEN 'EXACT_CUSTOMER_CODE' ELSE 'NO_APPROVED_MATCH_RULE' END AS reason
            FROM legacy_records r JOIN legacy_datasets d ON d.id=r.dataset_id AND d.tenant_id=r.tenant_id
            LEFT JOIN customers c ON d.semantic_type='CUSTOMER' AND c.tenant_id=r.tenant_id AND c.customer_code=r.legacy_code
            WHERE r.tenant_id=? AND r.import_batch_id=? ORDER BY r.id LIMIT 200
            """,tenant,id);
        long duration = (System.nanoTime()-started)/1_000_000;
        jdbc.update("UPDATE legacy_comparison_runs SET status='COMPLETED',counts=cast(? as jsonb),duration_ms=?,completed_at=now(),updated_at=now() WHERE tenant_id=? AND id=?",json(counts),duration,tenant,run);
        audit(tenant,id,"COMPARE",counts);
        Map<String,Object> result = new LinkedHashMap<>(counts); result.put("runId",run); result.put("durationMs",duration); result.put("matchingPolicy","Exact tenant-scoped customer_code only; all other records remain unmapped for review"); result.put("results",results); return result;
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
            Long datasetId=jdbc.queryForObject("INSERT INTO legacy_datasets(tenant_id,import_batch_id,import_file_id,dataset_key,source_table,semantic_type,support_status,source_encoding,source_record_count,imported_record_count,field_dictionary) VALUES (?,?,?,?,?,?,?,?,?,?,cast(? as jsonb)) RETURNING id",Long.class,tenant,batchId,fileIds.get(d.sourcePath()),d.key(),d.sourceTable(),d.semanticType(),d.supportStatus(),d.encoding(),d.sourceCount(),includeRecords?d.records().size():0,json(d.dictionary()));
            if (includeRecords) for (var r:d.records()) jdbc.update("INSERT INTO legacy_records(tenant_id,import_batch_id,dataset_id,source_record_id,legacy_code,normalized_key,raw_record,normalized_record) VALUES (?,?,?,?,?,?,cast(? as jsonb),cast(? as jsonb))",tenant,batchId,datasetId,r.sourceId(),r.legacyCode(),r.normalizedKey(),json(r.raw()),json(r.normalized()));
        }
        for (var m:analysis.messages()) jdbc.update("INSERT INTO legacy_import_messages(tenant_id,import_batch_id,import_file_id,severity,code,message) VALUES (?,?,?,?,?,?)",tenant,batchId,fileIds.get(m.sourcePath()),m.severity(),m.code(),m.message());
    }

    private String storageKey(Long tenant,Long id) { return jdbc.queryForObject("SELECT storage_key FROM legacy_import_batches WHERE tenant_id=? AND id=?",String.class,tenant,id); }
    private void audit(Long tenant,Long batch,String action,Map<String,?> detail) { jdbc.update("INSERT INTO legacy_import_audit_logs(tenant_id,import_batch_id,action,detail) VALUES (?,?,?,cast(? as jsonb))",tenant,batch,action,json(detail)); }
    private String json(Object value) { try { return json.writeValueAsString(value); } catch (JsonProcessingException e) { throw new IllegalStateException(e); } }
    private static int severity(LegacyImportAdapter.Analysis a,String s) { return (int)a.messages().stream().filter(m->s.equals(m.severity())).count(); }
    private static Map<String,Object> summary(LegacyImportAdapter.Analysis a) { return Map.of("fileCount",a.files().size(),"datasetCount",a.datasets().size(),"supportedFiles",a.files().stream().filter(f->"SUPPORTED".equals(f.supportStatus())).count(),"partialFiles",a.files().stream().filter(f->"PARTIAL".equals(f.supportStatus())).count(),"unsupportedFiles",a.files().stream().filter(f->"UNSUPPORTED".equals(f.supportStatus())).count()); }
    private static String extension(String n) { int i=n.lastIndexOf('.'); return i<0?null:n.substring(i+1).toLowerCase(); }
    private static ApiException badRequest(String message) { return new ApiException(ErrorCode.BAD_REQUEST, message); }
    private static String basename(String n) { String clean=n.replace('\\','/'); return clean.substring(clean.lastIndexOf('/')+1); }
    private static String sha256(byte[] bytes) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); } catch(Exception e){throw new IllegalStateException(e);} }
}
