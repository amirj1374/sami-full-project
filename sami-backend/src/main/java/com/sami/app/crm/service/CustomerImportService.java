package com.sami.app.crm.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.crm.domain.CustomerContact;
import com.sami.app.crm.dto.ContactDto;
import com.sami.app.crm.dto.CustomerRequest;
import com.sami.app.crm.dto.ImportResultResponse;
import com.sami.app.crm.repository.CustomerTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CSV customer import. Header-driven (column order free): recognized columns
 * are {@code displayName, firstName, lastName, phone, email, nationalCode,
 * companyName}; unknown columns are ignored, so future fields extend the
 * format without breaking old files.
 *
 * <p>Each row imports in its OWN transaction: a bad row (missing mandatory
 * data, duplicate per the configured rules) is reported and skipped, never
 * aborting the batch — re-importing the same file simply skips what already
 * exists. Excel import is a future extension that converts to this same
 * row shape.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerImportService {

    private final CustomerService customerService;
    private final CustomerTypeRepository typeRepository;

    public ImportResultResponse importCsv(MultipartFile file) {
        List<String[]> rows = parse(file);
        if (rows.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "The file is empty");
        }

        Map<String, Integer> header = headerIndex(rows.get(0));
        if (!header.containsKey("displayname")) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "The header must contain a displayName column");
        }
        Long defaultTypeId = typeRepository.findByIsDefaultTrue()
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                        "No default customer type is configured"))
                .getId();

        int created = 0;
        List<ImportResultResponse.SkippedRow> skipped = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            int line = i + 1;
            try {
                CustomerRequest request = toRequest(rows.get(i), header, defaultTypeId);
                // Each row commits independently (CustomerService.create opens
                // its own transaction), so one bad row never rolls back others.
                customerService.create(request);
                created++;
            } catch (ApiException ex) {
                skipped.add(new ImportResultResponse.SkippedRow(line, ex.getMessage()));
            } catch (Exception ex) {
                log.warn("Import row {} failed", line, ex);
                skipped.add(new ImportResultResponse.SkippedRow(line, "Unreadable row"));
            }
        }
        return new ImportResultResponse(created, skipped);
    }

    private CustomerRequest toRequest(String[] row, Map<String, Integer> header, Long typeId) {
        String displayName = cell(row, header, "displayname");
        if (displayName == null || displayName.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "displayName is required");
        }

        List<ContactDto> contacts = new ArrayList<>();
        String phone = cell(row, header, "phone");
        if (phone != null && !phone.isBlank()) {
            contacts.add(new ContactDto(null, CustomerContact.Kind.PHONE, phone.trim(), null, true));
        }
        String email = cell(row, header, "email");
        if (email != null && !email.isBlank()) {
            contacts.add(new ContactDto(null, CustomerContact.Kind.EMAIL, email.trim(), null, true));
        }

        return new CustomerRequest(
                cell(row, header, "firstname"),
                cell(row, header, "lastname"),
                displayName.trim(),
                cell(row, header, "nationalcode"),
                null, null, null, null,
                cell(row, header, "companyname"),
                null,
                typeId,
                null, null, null,
                contacts,
                List.of(),
                false,   // duplicates are never silently overridden by imports
                null);
    }

    private String cell(String[] row, Map<String, Integer> header, String column) {
        Integer index = header.get(column);
        if (index == null || index >= row.length) {
            return null;
        }
        String value = row[index].trim();
        return value.isEmpty() ? null : value;
    }

    private Map<String, Integer> headerIndex(String[] headerRow) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < headerRow.length; i++) {
            index.put(headerRow[i].trim().toLowerCase(Locale.ROOT), i);
        }
        return index;
    }

    /** Minimal RFC-4180-ish CSV parser (quoted fields, escaped quotes). */
    private List<String[]> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "A CSV file is required");
        }
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                rows.add(splitCsvLine(line));
            }
        } catch (IOException e) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Could not read the uploaded file");
        }
        return rows;
    }

    private String[] splitCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                cells.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        cells.add(current.toString());
        return cells.toArray(new String[0]);
    }
}
