package com.sami.app.purchasing.service;

import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.purchasing.domain.Purchase;
import com.sami.app.purchasing.dto.PurchaseDtos.DashboardResponse;
import com.sami.app.purchasing.dto.PurchaseDtos.OverdueLineResponse;
import com.sami.app.purchasing.dto.PurchaseDtos.PurchaseFilter;
import com.sami.app.purchasing.dto.PurchaseDtos.ReportGroup;
import com.sami.app.purchasing.dto.PurchaseDtos.ReportResponse;
import com.sami.app.purchasing.repository.PurchaseRepository;
import com.sami.app.purchasing.repository.PurchaseSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Purchasing dashboard, operational reports and Excel-compatible CSV export. */
@Service
@RequiredArgsConstructor
public class PurchaseReportingService {

    private final PurchaseRepository purchaseRepository;
    private final TenantContext tenantContext;

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        List<Purchase> purchases = purchases(new PurchaseFilter(
                null, null, null, null, null, null, null, null, null));
        long drafts = purchases.stream().filter(p -> p.getStatus().isDraftState()).count();
        long pending = purchases.stream().filter(p -> p.getStatus().isPendingState()).count();
        long open = purchases.stream().filter(p -> p.getStatus().isAllowsReceiving()).count();
        long completed = purchases.stream().filter(p -> p.getStatus().isCompletedState()).count();
        BigDecimal committed = purchases.stream().map(Purchase::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal received = purchases.stream().flatMap(p -> p.getItems().stream())
                .map(item -> item.getReceivedQuantity().multiply(item.getUnitPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DashboardResponse(purchases.size(), drafts, pending, open, completed,
                committed, received, overdue(purchases).size());
    }

    @Transactional(readOnly = true)
    public ReportResponse report(PurchaseFilter filter) {
        List<Purchase> purchases = purchases(filter);
        return new ReportResponse(
                group(purchases, p -> p.getStatus().getCode(), p -> p.getStatus().getName()),
                group(purchases, p -> p.getType().getCode(), p -> p.getType().getName()),
                group(purchases, this::sellerCode, this::sellerName),
                overdue(purchases));
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(PurchaseFilter filter) {
        StringBuilder csv = new StringBuilder();
        csv.append("Purchase Number,Type,Status,Seller Type,Seller Code,Seller,Warehouse,Total Amount,Created By,Created At\r\n");
        for (Purchase purchase : purchases(filter)) {
            csv.append(cell(purchase.getPurchaseNumber())).append(',')
                    .append(cell(purchase.getType().getName())).append(',')
                    .append(cell(purchase.getStatus().getName())).append(',')
                    .append(cell(purchase.getSellerType())).append(',')
                    .append(cell(sellerCode(purchase))).append(',')
                    .append(cell(sellerName(purchase))).append(',')
                    .append(cell(purchase.getWarehouse() == null ? ""
                            : purchase.getWarehouse().getName())).append(',')
                    .append(purchase.getTotalAmount()).append(',')
                    .append(cell(purchase.getCreatedByEmail())).append(',')
                    .append(cell(purchase.getCreatedAt())).append("\r\n");
        }
        byte[] content = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[content.length + 3];
        withBom[0] = (byte) 0xEF;
        withBom[1] = (byte) 0xBB;
        withBom[2] = (byte) 0xBF;
        System.arraycopy(content, 0, withBom, 3, content.length);
        return withBom;
    }

    private List<Purchase> purchases(PurchaseFilter filter) {
        return purchaseRepository.findAll(spec(filter), Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private Specification<Purchase> spec(PurchaseFilter filter) {
        return Specification.allOf(
                PurchaseSpecifications.hasTenant(tenantContext.requireTenantId()),
                PurchaseSpecifications.globalSearch(filter.search()),
                PurchaseSpecifications.hasSupplier(filter.supplierId()),
                PurchaseSpecifications.hasStatus(filter.statusId()),
                PurchaseSpecifications.hasType(filter.typeId()),
                PurchaseSpecifications.hasWarehouse(filter.warehouseId()),
                PurchaseSpecifications.containsProduct(filter.productId()),
                PurchaseSpecifications.createdBy(filter.createdBy()),
                PurchaseSpecifications.createdOnOrAfter(filter.createdFrom()),
                PurchaseSpecifications.createdBefore(filter.createdTo()));
    }

    private List<ReportGroup> group(List<Purchase> purchases,
                                    java.util.function.Function<Purchase, String> key,
                                    java.util.function.Function<Purchase, String> label) {
        Map<String, MutableGroup> groups = new LinkedHashMap<>();
        purchases.forEach(purchase -> groups
                .computeIfAbsent(key.apply(purchase), ignored -> new MutableGroup(label.apply(purchase)))
                .add(purchase.getTotalAmount()));
        return groups.entrySet().stream()
                .map(entry -> new ReportGroup(entry.getKey(), entry.getValue().label,
                        entry.getValue().count, entry.getValue().amount)).toList();
    }

    private List<OverdueLineResponse> overdue(List<Purchase> purchases) {
        LocalDate today = LocalDate.now();
        List<OverdueLineResponse> result = new ArrayList<>();
        purchases.stream()
                .filter(p -> !p.getStatus().isTerminal())
                .forEach(purchase -> purchase.getItems().stream()
                        .filter(item -> item.getExpectedDelivery() != null
                                && item.getExpectedDelivery().isBefore(today)
                                && item.remainingQuantity().compareTo(BigDecimal.ZERO) > 0)
                        .forEach(item -> result.add(new OverdueLineResponse(
                                purchase.getId(), purchase.getPurchaseNumber(),
                                sellerName(purchase), item.getId(),
                                item.getProduct().getName(), item.getExpectedDelivery(),
                                item.getQuantity(), item.remainingQuantity()))));
        return result;
    }

    private String cell(Object value) {
        String text = value == null ? "" : value.toString();
        return '"' + text.replace("\"", "\"\"") + '"';
    }

    private String sellerCode(Purchase purchase) {
        return purchase.getSupplier() != null ? purchase.getSupplier().getSupplierCode()
                : purchase.getSellerCustomer().getCustomerCode();
    }

    private String sellerName(Purchase purchase) {
        return purchase.getSupplier() != null ? purchase.getSupplier().getDisplayName()
                : purchase.getSellerCustomer().getDisplayName();
    }

    private static final class MutableGroup {
        private final String label;
        private long count;
        private BigDecimal amount = BigDecimal.ZERO;

        private MutableGroup(String label) {
            this.label = label;
        }

        private void add(BigDecimal value) {
            count++;
            amount = amount.add(value);
        }
    }
}
