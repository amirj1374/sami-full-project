package com.sami.app.purchasing.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.inventory.domain.InventoryWarehouse;
import com.sami.app.inventory.repository.InventoryWarehouseRepository;
import com.sami.app.product.domain.Product;
import com.sami.app.product.repository.ProductRepository;
import com.sami.app.purchasing.domain.PurType;
import com.sami.app.purchasing.dto.PurchaseDtos.ImportResponse;
import com.sami.app.purchasing.dto.PurchaseDtos.ItemRequest;
import com.sami.app.purchasing.dto.PurchaseDtos.PurchaseDetailResponse;
import com.sami.app.purchasing.dto.PurchaseDtos.PurchaseRequest;
import com.sami.app.purchasing.repository.PurchaseRepository;
import com.sami.app.supplier.domain.Supplier;
import com.sami.app.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Validated, idempotent CSV import for purchase drafts. */
@Service
@RequiredArgsConstructor
public class PurchaseImportService {

    private static final long MAX_BYTES = 5L * 1024L * 1024L;
    private static final List<String> HEADER = List.of(
            "purchaseKey", "typeCode", "supplierCode", "warehouseCode", "productSku",
            "quantity", "unit", "unitPrice", "discount", "expectedDelivery", "notes");

    private final TenantContext tenantContext;
    private final PurchaseRepository purchaseRepository;
    private final PurchaseService purchaseService;
    private final PurchasingConfigService config;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final InventoryWarehouseRepository warehouseRepository;

    @Transactional
    public ImportResponse importCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw invalid("A CSV file is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw invalid("CSV file exceeds the 5 MB limit");
        }

        List<List<String>> rows = parse(read(file));
        if (rows.isEmpty() || !HEADER.equals(rows.get(0))) {
            throw invalid("CSV header must be: " + String.join(",", HEADER));
        }

        Map<String, List<List<String>>> groups = new LinkedHashMap<>();
        for (int index = 1; index < rows.size(); index++) {
            List<String> row = rows.get(index);
            if (row.stream().allMatch(String::isBlank)) continue;
            if (row.size() != HEADER.size()) {
                throw invalid("CSV row " + (index + 1) + " has " + row.size()
                        + " columns; expected " + HEADER.size());
            }
            require(row.get(0), "purchaseKey", index + 1);
            groups.computeIfAbsent(row.get(0).trim(), ignored -> new ArrayList<>()).add(row);
        }
        if (groups.isEmpty()) {
            throw invalid("CSV contains no purchase rows");
        }

        int created = 0;
        int skipped = 0;
        List<String> numbers = new ArrayList<>();
        for (Map.Entry<String, List<List<String>>> entry : groups.entrySet()) {
            String importKey = "csv:" + sha256(entry.getKey());
            if (purchaseRepository.existsByTenantIdAndImportKey(tenantId(), importKey)) {
                skipped++;
                continue;
            }
            PurchaseDetailResponse response = purchaseService.createImported(
                    toRequest(entry.getKey(), entry.getValue()), importKey);
            numbers.add(response.purchase().purchaseNumber());
            created++;
        }
        return new ImportResponse(created, skipped, numbers);
    }

    private PurchaseRequest toRequest(String key, List<List<String>> rows) {
        List<String> first = rows.get(0);
        String typeCode = require(first.get(1), "typeCode", 0);
        String supplierCode = require(first.get(2), "supplierCode", 0);
        String warehouseCode = first.get(3).trim();
        String notes = blankToNull(first.get(10));
        for (List<String> row : rows) {
            if (!typeCode.equalsIgnoreCase(row.get(1).trim())
                    || !supplierCode.equalsIgnoreCase(row.get(2).trim())
                    || !warehouseCode.equalsIgnoreCase(row.get(3).trim())) {
                throw invalid("Purchase '" + key
                        + "' must use one type, supplier and warehouse");
            }
        }

        PurType type = config.requireTypeByCode(typeCode);
        Supplier supplier = supplierRepository
                .findByTenantIdAndSupplierCodeIgnoreCase(tenantId(), supplierCode)
                .orElseThrow(() -> invalid("Unknown supplier code '" + supplierCode + "'"));
        InventoryWarehouse warehouse = warehouseCode.isBlank() ? null : warehouseRepository
                .findByTenantIdAndCodeIgnoreCase(tenantId(), warehouseCode)
                .filter(InventoryWarehouse::isActive)
                .orElseThrow(() -> invalid("Unknown or inactive warehouse '"
                        + warehouseCode + "'"));

        List<ItemRequest> items = new ArrayList<>();
        for (List<String> row : rows) {
            String sku = require(row.get(4), "productSku", 0);
            Product product = productRepository.findByTenantIdAndSkuIgnoreCase(tenantId(), sku)
                    .orElseThrow(() -> invalid("Unknown product SKU '" + sku + "'"));
            BigDecimal quantity = positive(row.get(5), "quantity");
            String unit = require(row.get(6), "unit", 0);
            BigDecimal price = nonNegative(row.get(7), "unitPrice");
            BigDecimal discount = row.get(8).isBlank()
                    ? BigDecimal.ZERO : nonNegative(row.get(8), "discount");
            LocalDate expected = date(row.get(9));
            items.add(new ItemRequest(product.getId(), null, quantity, unit, price,
                    discount, expected, false, false));
        }
        return new PurchaseRequest(type.getId(),
                com.sami.app.purchasing.domain.PurchaseSellerType.SUPPLIER,
                supplier.getId(), null, null, null, null,
                com.sami.app.purchasing.domain.PurchaseItemCondition.OTHER,
                null, false, null, null,
                com.sami.app.purchasing.domain.PurchaseSettlementStatus.PENDING,
                null, null, null,
                warehouse == null ? null : warehouse.getId(), notes, items, null);
    }

    private String read(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            int offset = bytes.length >= 3 && bytes[0] == (byte) 0xEF
                    && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF ? 3 : 0;
            return new String(bytes, offset, bytes.length - offset, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw invalid("Could not read the CSV file");
        }
    }

    private List<List<String>> parse(String text) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (current == '"') {
                if (quoted && index + 1 < text.length() && text.charAt(index + 1) == '"') {
                    field.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                row.add(field.toString().trim());
                field.setLength(0);
            } else if ((current == '\n' || current == '\r') && !quoted) {
                if (current == '\r' && index + 1 < text.length() && text.charAt(index + 1) == '\n') {
                    index++;
                }
                row.add(field.toString().trim());
                field.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            } else {
                field.append(current);
            }
        }
        if (quoted) throw invalid("CSV contains an unterminated quoted field");
        if (field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString().trim());
            rows.add(row);
        }
        return rows;
    }

    private BigDecimal positive(String value, String name) {
        BigDecimal parsed = number(value, name);
        if (parsed.compareTo(BigDecimal.ZERO) <= 0) throw invalid(name + " must be positive");
        return parsed;
    }

    private BigDecimal nonNegative(String value, String name) {
        BigDecimal parsed = number(value, name);
        if (parsed.compareTo(BigDecimal.ZERO) < 0) throw invalid(name + " cannot be negative");
        return parsed;
    }

    private BigDecimal number(String value, String name) {
        try {
            return new BigDecimal(require(value, name, 0));
        } catch (NumberFormatException exception) {
            throw invalid(name + " must be a valid number");
        }
    }

    private LocalDate date(String value) {
        if (value.isBlank()) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw invalid("expectedDelivery must use YYYY-MM-DD");
        }
    }

    private String require(String value, String name, int row) {
        if (value == null || value.isBlank()) {
            throw invalid(name + " is required" + (row > 0 ? " on row " + row : ""));
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private ApiException invalid(String message) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, message);
    }

    private Long tenantId() {
        return tenantContext.requireTenantId();
    }
}
