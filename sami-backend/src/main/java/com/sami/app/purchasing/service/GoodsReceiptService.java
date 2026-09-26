package com.sami.app.purchasing.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.inventory.publicapi.InventoryStockOperations;
import com.sami.app.inventory.publicapi.InventoryStockOperations.SerialIdentity;
import com.sami.app.organization.service.FoundationAuditService;
import com.sami.app.organization.service.OrganizationScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Purchasing-owned receipt boundary. Inventory remains the sole stock writer. */
@Service
@RequiredArgsConstructor
public class GoodsReceiptService {
    private final JdbcTemplate jdbc;
    private final TenantContext tenants;
    private final OrganizationScopeService scope;
    private final InventoryStockOperations inventory;
    private final FoundationAuditService audit;

    @Transactional
    public Map<String, Object> create(Long company, Long branch, Long purchaseOrderId, Long warehouse,
                                      List<Map<String, Object>> lines, String notes) {
        Long tenant = tenants.requireTenantId();
        scope.requireScope(company, branch);
        Map<String, Object> purchaseOrder = loadPoForUpdate(tenant, purchaseOrderId);
        if (!Objects.equals(company, ((Number) purchaseOrder.get("company_id")).longValue())
                || !Objects.equals(branch, ((Number) purchaseOrder.get("branch_id")).longValue())) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Purchase order belongs to a different organization scope");
        }
        if (!"APPROVED".equals(purchaseOrder.get("status"))) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "Purchase order must be approved");
        }
        if (lines == null || lines.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Receipt requires items");
        }

        String receiptNumber = "GR-" + jdbc.queryForObject("select nextval('goods_receipt_number_seq')", Long.class);
        Long receiptId = jdbc.queryForObject("""
                insert into goods_receipts(tenant_id,company_id,branch_id,purchase_order_id,supplier_id,receipt_number,warehouse_id,notes)
                values(?,?,?,?,?,?,?,?) returning id
                """, Long.class, tenant, company, branch, purchaseOrderId, purchaseOrder.get("supplier_id"),
                receiptNumber, warehouse, notes);

        List<InventoryStockOperations.ReceiptLine> productLines = new ArrayList<>();
        List<InventoryStockOperations.VariantPurchaseReceiptCommand> variantLines = new ArrayList<>();
        for (Map<String, Object> line : lines) {
            Long purchaseOrderLineId = requiredId(line.get("purchaseOrderLineId"), "Purchase order line");
            BigDecimal received = requiredDecimal(line.get("receivedQuantity"), "Received quantity");
            BigDecimal unitCost = requiredDecimal(line.get("unitCost"), "Unit cost");
            Map<String, Object> source = jdbc.queryForMap("""
                    select product_id, variant_id, quantity from purchase_order_lines
                    where tenant_id=? and id=? and purchase_order_id=? for update
                    """, tenant, purchaseOrderLineId, purchaseOrderId);
            if (received.signum() <= 0 || unitCost.signum() < 0) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "Receipt quantity and cost are invalid");
            }
            BigDecimal alreadyReceived = jdbc.queryForObject("""
                    select coalesce(sum(l.received_quantity),0)
                    from goods_receipt_lines l join goods_receipts r on r.id=l.receipt_id
                    where l.tenant_id=? and l.purchase_order_line_id=? and r.status='CONFIRMED'
                    """, BigDecimal.class, tenant, purchaseOrderLineId);
            if (alreadyReceived.add(received).compareTo((BigDecimal) source.get("quantity")) > 0) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "Received quantity exceeds remaining ordered quantity");
            }

            List<SerialIdentity> serials = parseSerials(line.get("serials"));
            if (!serials.isEmpty() && received.compareTo(BigDecimal.valueOf(serials.size())) != 0) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "Serialized receipt quantity must equal serial count");
            }
            Long productId = ((Number) source.get("product_id")).longValue();
            Long variantId = source.get("variant_id") == null ? null : ((Number) source.get("variant_id")).longValue();
            Long receiptLineId = jdbc.queryForObject("""
                    insert into goods_receipt_lines(tenant_id,receipt_id,purchase_order_line_id,product_id,variant_id,received_quantity,unit_cost)
                    values(?,?,?,?,?,?,?) returning id
                    """, Long.class, tenant, receiptId, purchaseOrderLineId, productId, variantId, received, unitCost);
            persistIdentifiers(tenant, receiptLineId, serials);
            if (variantId == null) {
                productLines.add(new InventoryStockOperations.ReceiptLine(purchaseOrderLineId, productId, received, unitCost, serials));
            } else {
                variantLines.add(new InventoryStockOperations.VariantPurchaseReceiptCommand(
                        warehouse, purchaseOrderId, receiptId, receiptLineId, productId, variantId, received,
                        null, null, BigDecimal.ONE, unitCost, serials));
            }
        }
        if (!productLines.isEmpty()) {
            inventory.receivePurchase(new InventoryStockOperations.PurchaseReceiptCommand(warehouse, purchaseOrderId, receiptId, productLines));
        }
        variantLines.forEach(inventory::receiveVariantPurchase);
        jdbc.update("update goods_receipts set status='CONFIRMED',updated_at=now() where id=? and tenant_id=?", receiptId, tenant);
        audit.record(tenant, company, branch, "GOODS_RECEIPT", receiptId, "CONFIRMED", "Goods receipt confirmed",
                Map.of("purchaseOrderId", purchaseOrderId));
        return get(receiptId);
    }

    private void persistIdentifiers(Long tenant, Long receiptLineId, List<SerialIdentity> serials) {
        for (SerialIdentity serial : serials) {
            jdbc.update("""
                    insert into goods_receipt_unit_identifiers(tenant_id,goods_receipt_line_id,serial_number,imei,hamta_activation_code)
                    values(?,?,?,?,?)
                    """, tenant, receiptLineId, blank(serial.serialNumber()), blank(serial.imei()), blank(serial.hamtaActivationCode()));
        }
    }

    private List<SerialIdentity> parseSerials(Object raw) {
        if (raw == null) return List.of();
        if (!(raw instanceof List<?> values)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Serial identifiers must be a list");
        }
        List<SerialIdentity> serials = new ArrayList<>();
        for (Object value : values) {
            if (!(value instanceof Map<?, ?> identifier)) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "Serial identifier is invalid");
            }
            String serial = blank(identifier.get("serialNumber"));
            String imei = blank(identifier.get("imei"));
            if (serial == null && imei == null) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "Serial number or IMEI is required");
            }
            serials.add(new SerialIdentity(serial, imei, blank(identifier.get("hamtaActivationCode"))));
        }
        return serials;
    }

    private Map<String, Object> loadPoForUpdate(Long tenant, Long id) {
        try { return jdbc.queryForMap("select * from purchase_orders where tenant_id=? and id=? for update", tenant, id); }
        catch (Exception e) { throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Purchase order not found"); }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(Long id) {
        Long tenant = tenants.requireTenantId();
        Map<String, Object> receipt;
        try { receipt = jdbc.queryForMap("select * from goods_receipts where tenant_id=? and id=?", tenant, id); }
        catch (Exception e) { throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Goods receipt not found"); }
        scope.requireScope(((Number) receipt.get("company_id")).longValue(), ((Number) receipt.get("branch_id")).longValue());
        List<Map<String, Object>> receiptLines = jdbc.queryForList("select * from goods_receipt_lines where tenant_id=? and receipt_id=?", tenant, id);
        receiptLines.forEach(line -> line.put("serials", jdbc.queryForList("""
                select serial_number,imei,hamta_activation_code from goods_receipt_unit_identifiers
                where tenant_id=? and goods_receipt_line_id=? order by id
                """, tenant, ((Number) line.get("id")).longValue())));
        receipt.put("lines", receiptLines);
        return receipt;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        Long tenant = tenants.requireTenantId();
        var current = scope.current();
        if (current.companyId() == null) return List.of();
        return current.branchId() == null
                ? jdbc.queryForList("select * from goods_receipts where tenant_id=? and company_id=? order by received_at desc", tenant, current.companyId())
                : jdbc.queryForList("select * from goods_receipts where tenant_id=? and company_id=? and branch_id=? order by received_at desc", tenant, current.companyId(), current.branchId());
    }

    private Long requiredId(Object value, String name) {
        try { return Long.valueOf(String.valueOf(value)); }
        catch (Exception e) { throw new ApiException(ErrorCode.VALIDATION_FAILED, name + " is required"); }
    }
    private BigDecimal requiredDecimal(Object value, String name) {
        try { return new BigDecimal(String.valueOf(value)); }
        catch (Exception e) { throw new ApiException(ErrorCode.VALIDATION_FAILED, name + " is invalid"); }
    }
    private String blank(Object value) {
        if (value == null) return null;
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }
}
