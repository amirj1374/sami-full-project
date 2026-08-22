package com.sami.app.siminvestment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SimInvestmentImportService {
    private static final long MAX_BYTES = 20L * 1024 * 1024;
    private static final int MAX_ROWS = 100_000;
    private static final Pattern DATE_IN_NAME = Pattern.compile("(20\\d{2})-(\\d{2})-(\\d{2})");
    private final JdbcTemplate jdbc;
    private final TenantContext tenants;
    private final ObjectMapper json;
    private final SimInvestmentAnalysisService analysis;

    @Transactional
    public Map<String, Object> importFile(MultipartFile file, String sourceCode, LocalDate observedOn, boolean fullSnapshot) {
        validateFile(file);
        long tenant = tenants.requireTenantId();
        String filename = basename(file.getOriginalFilename() == null ? "0912-market.csv" : file.getOriginalFilename());
        byte[] content;
        try { content = file.getBytes(); }
        catch (IOException exception) { throw new ApiException(ErrorCode.BAD_REQUEST, "The selected file could not be read"); }
        String hash = sha256(content);
        if (Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from sim_investment_import_batches where tenant_id=? and sha256=?)", Boolean.class, tenant, hash))) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT, "This market snapshot has already been imported");
        }
        LocalDate snapshotDate = observedOn != null ? observedOn : dateFromFilename(filename);
        String source = normalizeSource(sourceCode);
        if (fullSnapshot) ensureChronological(tenant, source, snapshotDate);
        List<SourceRow> parsed = parse(filename, content);
        if (parsed.isEmpty()) throw new ApiException(ErrorCode.VALIDATION_FAILED, "The file contains no data rows");
        LinkedHashMap<String, SourceRow> unique = new LinkedHashMap<>();
        List<ImportMessage> messages = new ArrayList<>();
        int duplicateCount = 0;
        for (SourceRow row : parsed) {
            try {
                String phone = SimRondiAnalyzer.normalize(row.phone());
                BigDecimal price = money(row.price());
                if (price.signum() == 0) messages.add(new ImportMessage("WARNING", "ZERO_PRICE", row.line(), "Price is zero and will be excluded from valuation"));
                SourceRow normalized = new SourceRow(row.line(), phone, price.toPlainString(), required(row.condition(), "status"), required(row.seller(), "seller_id"));
                SourceRow previous = unique.putIfAbsent(phone, normalized);
                if (previous != null) {
                    duplicateCount++;
                    String code = previous.price().equals(normalized.price()) && previous.seller().equals(normalized.seller()) ? "DUPLICATE_ROW" : "CONFLICTING_DUPLICATE";
                    messages.add(new ImportMessage("WARNING", code, row.line(), "Duplicate 0912 number was ignored"));
                }
            } catch (RuntimeException exception) {
                messages.add(new ImportMessage("ERROR", "INVALID_ROW", row.line(), safe(exception.getMessage())));
            }
        }
        if (unique.isEmpty()) throw new ApiException(ErrorCode.VALIDATION_FAILED, "No valid 0912 rows were found");
        long batchId;
        try {
            batchId = jdbc.queryForObject("""
                    insert into sim_investment_import_batches(tenant_id,source_code,original_filename,sha256,observed_on,full_snapshot,status,
                    source_row_count,requested_by,requested_by_email) values(?,?,?,?,?,?,'PROCESSING',?,?,?) returning id
                    """, Long.class, tenant, source, filename, hash, snapshotDate, fullSnapshot, parsed.size(), CurrentActor.id(), CurrentActor.email());
        } catch (DuplicateKeyException exception) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT, "This market snapshot has already been imported");
        }
        writeRows(tenant, batchId, source, snapshotDate, unique.values());
        if (fullSnapshot) jdbc.update("""
                update sim_investment_listings set removed_on=?,updated_at=now()
                where tenant_id=? and source_code=? and removed_on is null and last_import_batch_id<>?
                """, snapshotDate, tenant, source, batchId);
        writeMessages(tenant, batchId, messages);
        long errors = messages.stream().filter(message -> "ERROR".equals(message.severity())).count();
        long warnings = messages.size() - errors;
        String status = messages.isEmpty() ? "COMPLETED" : "COMPLETED_WITH_WARNINGS";
        jdbc.update("""
                update sim_investment_import_batches set status=?,imported_count=?,duplicate_count=?,warning_count=?,error_count=?,completed_at=now()
                where tenant_id=? and id=?
                """, status, unique.size(), duplicateCount, warnings, errors, tenant, batchId);
        Map<String, Object> analysisResult = analysis.recalculate();
        audit(tenant, batchId, "IMPORT_COMPLETED", Map.of("filename", filename, "rows", parsed.size(), "imported", unique.size(), "duplicates", duplicateCount));
        Map<String, Object> result = new LinkedHashMap<>(batch(batchId));
        result.put("analysis", analysisResult);
        return result;
    }

    public List<Map<String, Object>> imports() {
        return jdbc.queryForList("""
                select id,source_code,original_filename,observed_on,full_snapshot,status,source_row_count,imported_count,duplicate_count,
                warning_count,error_count,started_at,completed_at from sim_investment_import_batches where tenant_id=? order by observed_on desc,id desc limit 100
                """, tenants.requireTenantId());
    }

    public List<Map<String, Object>> messages(long batchId) {
        long tenant = tenants.requireTenantId(); batch(batchId);
        return jdbc.queryForList("select severity,code,source_row_number,message from sim_investment_import_messages where tenant_id=? and import_batch_id=? order by id limit 500", tenant, batchId);
    }

    private Map<String, Object> batch(long id) {
        List<Map<String, Object>> rows = jdbc.queryForList("select * from sim_investment_import_batches where tenant_id=? and id=?", tenants.requireTenantId(), id);
        if (rows.isEmpty()) throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Import batch not found");
        return rows.getFirst();
    }

    private void writeRows(long tenant, long batchId, String source, LocalDate observed, Iterable<SourceRow> rows) {
        List<Object[]> history = new ArrayList<>(), listings = new ArrayList<>();
        for (SourceRow row : rows) {
            BigDecimal price = new BigDecimal(row.price());
            history.add(new Object[]{tenant,batchId,row.phone(),price,row.condition(),row.seller(),row.line(),observed});
            listings.add(new Object[]{tenant,source,row.phone(),price,row.condition(),row.seller(),observed,observed,batchId});
            if (history.size() == 1000) { flushRows(history, listings); history.clear(); listings.clear(); }
        }
        if (!history.isEmpty()) flushRows(history, listings);
    }

    private void flushRows(List<Object[]> history, List<Object[]> listings) {
        jdbc.batchUpdate("""
                insert into sim_investment_listing_history(tenant_id,import_batch_id,normalized_phone,price_toman,condition_code,seller_external_id,source_row_number,observed_on)
                values(?,?,?,?,?,?,?,?)
                """, history);
        jdbc.batchUpdate("""
                insert into sim_investment_listings(tenant_id,source_code,normalized_phone,price_toman,condition_code,seller_external_id,first_seen_on,last_seen_on,last_import_batch_id)
                values(?,?,?,?,?,?,?,?,?) on conflict(tenant_id,source_code,normalized_phone) do update set
                price_toman=case when excluded.last_seen_on>=sim_investment_listings.last_seen_on then excluded.price_toman else sim_investment_listings.price_toman end,
                condition_code=case when excluded.last_seen_on>=sim_investment_listings.last_seen_on then excluded.condition_code else sim_investment_listings.condition_code end,
                seller_external_id=case when excluded.last_seen_on>=sim_investment_listings.last_seen_on then excluded.seller_external_id else sim_investment_listings.seller_external_id end,
                first_seen_on=least(sim_investment_listings.first_seen_on,excluded.first_seen_on),
                last_seen_on=greatest(sim_investment_listings.last_seen_on,excluded.last_seen_on),
                removed_on=case when excluded.last_seen_on>=sim_investment_listings.last_seen_on then null else sim_investment_listings.removed_on end,
                last_import_batch_id=case when excluded.last_seen_on>=sim_investment_listings.last_seen_on then excluded.last_import_batch_id else sim_investment_listings.last_import_batch_id end,
                updated_at=now()
                """, listings);
    }

    private void writeMessages(long tenant, long batchId, List<ImportMessage> messages) {
        List<Object[]> batch = messages.stream().map(message -> new Object[]{tenant,batchId,message.severity(),message.code(),message.line(),message.message()}).toList();
        for (int start = 0; start < batch.size(); start += 1000) {
            jdbc.batchUpdate("insert into sim_investment_import_messages(tenant_id,import_batch_id,severity,code,source_row_number,message) values(?,?,?,?,?,?)",
                    batch.subList(start, Math.min(batch.size(), start + 1000)));
        }
    }

    private List<SourceRow> parse(String filename, byte[] content) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".csv")) return parseCsv(content);
        if (lower.endsWith(".xlsx")) return parseXlsx(content);
        throw new ApiException(ErrorCode.VALIDATION_FAILED, "Only CSV and XLSX market files are accepted");
    }

    private List<SourceRow> parseCsv(byte[] content) {
        List<SourceRow> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) return result;
            Map<String, Integer> header = header(splitCsv(headerLine.replace("\uFEFF", "")));
            String line; int number = 1;
            while ((line = reader.readLine()) != null) {
                number++; if (line.isBlank()) continue; if (number > MAX_ROWS + 1) throw tooManyRows();
                String[] cells = splitCsv(line);
                result.add(new SourceRow(number, cell(cells, header, "phone"), cell(cells, header, "price"), cell(cells, header, "status"), cell(cells, header, "seller_id")));
            }
        } catch (IOException exception) { throw new ApiException(ErrorCode.BAD_REQUEST, "CSV file could not be read"); }
        return result;
    }

    private List<SourceRow> parseXlsx(byte[] content) {
        List<SourceRow> result = new ArrayList<>(); DataFormatter formatter = new DataFormatter(Locale.ROOT);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            if (workbook.getNumberOfSheets() == 0) return result;
            Sheet sheet = workbook.getSheetAt(0); Row first = sheet.getRow(sheet.getFirstRowNum());
            if (first == null) return result;
            String[] headers = new String[first.getLastCellNum()];
            for (int index = 0; index < headers.length; index++) headers[index] = formatter.formatCellValue(first.getCell(index));
            Map<String, Integer> header = header(headers);
            for (int index = first.getRowNum() + 1; index <= sheet.getLastRowNum(); index++) {
                if (result.size() >= MAX_ROWS) throw tooManyRows(); Row row = sheet.getRow(index); if (row == null) continue;
                String phone=xlsxCell(row,header,"phone",formatter), price=xlsxCell(row,header,"price",formatter), status=xlsxCell(row,header,"status",formatter), seller=xlsxCell(row,header,"seller_id",formatter);
                if (phone.isBlank() && price.isBlank() && status.isBlank() && seller.isBlank()) continue;
                result.add(new SourceRow(index + 1, phone, price, status, seller));
            }
        } catch (ApiException exception) { throw exception; }
        catch (Exception exception) { throw new ApiException(ErrorCode.BAD_REQUEST, "XLSX file could not be read"); }
        return result;
    }

    private String xlsxCell(Row row, Map<String,Integer> header, String name, DataFormatter formatter) {
        Cell cell=row.getCell(header.get(name));
        if (cell == null) return "";
        if (("phone".equals(name) || "price".equals(name)) && cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue()).toBigInteger().toString();
        }
        return formatter.formatCellValue(cell).trim();
    }

    private Map<String,Integer> header(String[] cells) {
        Map<String,Integer> result=new HashMap<>();
        for(int i=0;i<cells.length;i++) result.put(cells[i].trim().toLowerCase(Locale.ROOT),i);
        for(String required:List.of("phone","price","status","seller_id")) if(!result.containsKey(required))
            throw new ApiException(ErrorCode.VALIDATION_FAILED,"Required column is missing: "+required);
        return result;
    }

    private String cell(String[] cells, Map<String,Integer> header, String name) { int index=header.get(name); return index<cells.length?cells[index].trim():""; }
    private String[] splitCsv(String line) {
        List<String> cells=new ArrayList<>();StringBuilder current=new StringBuilder();boolean quoted=false;
        for(int i=0;i<line.length();i++){char c=line.charAt(i);if(quoted){if(c=='"'&&i+1<line.length()&&line.charAt(i+1)=='"'){current.append('"');i++;}else if(c=='"')quoted=false;else current.append(c);}else if(c=='"')quoted=true;else if(c==','){cells.add(current.toString());current.setLength(0);}else current.append(c);}
        cells.add(current.toString());return cells.toArray(String[]::new);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ApiException(ErrorCode.VALIDATION_FAILED, "Select a non-empty CSV or XLSX file");
        if (file.getSize() > MAX_BYTES) throw new ApiException(ErrorCode.UPLOAD_TOO_LARGE);
    }
    private void ensureChronological(long tenant,String source,LocalDate observed) {
        LocalDate latest=jdbc.queryForObject("select max(last_seen_on) from sim_investment_listings where tenant_id=? and source_code=?",LocalDate.class,tenant,source);
        if(latest!=null&&observed.isBefore(latest))throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,"A full snapshot cannot be older than the current source snapshot");
    }
    private BigDecimal money(String value) { try { BigDecimal result=new BigDecimal(value.replace(",","").trim());if(result.signum()<0)throw new NumberFormatException();return result.setScale(2,java.math.RoundingMode.HALF_UP); } catch(Exception exception){throw new ApiException(ErrorCode.VALIDATION_FAILED,"price must be a non-negative Toman amount");} }
    private String required(String value,String field){if(value==null||value.isBlank())throw new ApiException(ErrorCode.VALIDATION_FAILED,field+" is required");return value.trim();}
    private String normalizeSource(String value){String source=value==null||value.isBlank()?"0912_MANUAL":value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_-]","_");return source.substring(0,Math.min(80,source.length()));}
    private LocalDate dateFromFilename(String filename){Matcher matcher=DATE_IN_NAME.matcher(filename);return matcher.find()?LocalDate.parse(matcher.group()):LocalDate.now();}
    private ApiException tooManyRows(){return new ApiException(ErrorCode.VALIDATION_FAILED,"The file must not contain more than "+MAX_ROWS+" rows");}
    private String basename(String value){String clean=value.replace('\\','/');return clean.substring(clean.lastIndexOf('/')+1);}
    private String safe(String value){String result=value==null?"Unreadable row":value;return result.substring(0,Math.min(500,result.length()));}
    private String sha256(byte[] value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));}catch(Exception exception){throw new IllegalStateException(exception);}}
    private void audit(long tenant,long batch,String action,Map<String,?> detail){jdbc.update("insert into sim_investment_audit_logs(tenant_id,import_batch_id,action,actor_id,actor_email,detail) values(?,?,?,?,?,cast(? as jsonb))",tenant,batch,action,CurrentActor.id(),CurrentActor.email(),json(detail));}
    private String json(Object value){try{return json.writeValueAsString(value);}catch(JsonProcessingException exception){throw new IllegalStateException(exception);}}
    private record SourceRow(int line,String phone,String price,String condition,String seller){}
    private record ImportMessage(String severity,String code,Integer line,String message){}
}
