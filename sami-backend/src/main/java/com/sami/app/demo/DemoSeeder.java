package com.sami.app.demo;

import com.sami.app.crm.domain.Customer;
import com.sami.app.crm.domain.CustomerAddress;
import com.sami.app.crm.domain.CustomerContact;
import com.sami.app.crm.domain.CustomerEvent;
import com.sami.app.crm.domain.CustomerSource;
import com.sami.app.crm.domain.CustomerStatus;
import com.sami.app.crm.domain.CustomerTag;
import com.sami.app.crm.domain.CustomerType;
import com.sami.app.crm.repository.CustomerEventRepository;
import com.sami.app.crm.repository.CustomerRepository;
import com.sami.app.crm.repository.CustomerSourceRepository;
import com.sami.app.crm.repository.CustomerStatusRepository;
import com.sami.app.crm.repository.CustomerTagRepository;
import com.sami.app.crm.repository.CustomerTypeRepository;
import com.sami.app.demo.DemoDataPools.City;
import com.sami.app.demo.DemoDataPools.ProductTemplate;
import com.sami.app.demo.DemoDataPools.SupplierTemplate;
import com.sami.app.product.domain.Product;
import com.sami.app.product.repository.ProductRepository;
import com.sami.app.purchasing.domain.Purchase;
import com.sami.app.purchasing.domain.PurchaseItem;
import com.sami.app.purchasing.domain.PurStatus;
import com.sami.app.purchasing.domain.PurType;
import com.sami.app.purchasing.domain.PurWarehouse;
import com.sami.app.purchasing.repository.PurStatusRepository;
import com.sami.app.purchasing.repository.PurTypeRepository;
import com.sami.app.purchasing.repository.PurWarehouseRepository;
import com.sami.app.purchasing.repository.PurchaseRepository;
import com.sami.app.supplier.domain.SupAddress;
import com.sami.app.supplier.domain.SupCategory;
import com.sami.app.supplier.domain.SupChannel;
import com.sami.app.supplier.domain.SupContact;
import com.sami.app.supplier.domain.SupPaymentTerm;
import com.sami.app.supplier.domain.SupStatus;
import com.sami.app.supplier.domain.SupTag;
import com.sami.app.supplier.domain.SupType;
import com.sami.app.supplier.domain.Supplier;
import com.sami.app.supplier.repository.SupCategoryRepository;
import com.sami.app.supplier.repository.SupPaymentTermRepository;
import com.sami.app.supplier.repository.SupStatusRepository;
import com.sami.app.supplier.repository.SupTagRepository;
import com.sami.app.supplier.repository.SupTypeRepository;
import com.sami.app.supplier.repository.SupplierRepository;
import com.sami.app.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Generates a realistic, interconnected demo dataset for the modules that exist
 * today (products, suppliers, customers/CRM timeline, purchases) directly on top
 * of the current architecture — same entities, repositories and conventions the
 * app already uses. Each phase is its own transaction so a failure in one does
 * not roll back the others, and {@code created_at} is backdated so dashboards and
 * time-series reflect "a store with a few years of history".
 *
 * <p>Nothing here changes the schema: it only inserts rows through existing
 * repositories, reusing the reference data seeded by the Flyway migrations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemoSeeder {

    private static final int BATCH = 100;

    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final SupTypeRepository supTypeRepository;
    private final SupStatusRepository supStatusRepository;
    private final SupPaymentTermRepository supPaymentTermRepository;
    private final SupCategoryRepository supCategoryRepository;
    private final SupTagRepository supTagRepository;
    private final CustomerRepository customerRepository;
    private final CustomerTypeRepository customerTypeRepository;
    private final CustomerStatusRepository customerStatusRepository;
    private final CustomerSourceRepository customerSourceRepository;
    private final CustomerTagRepository customerTagRepository;
    private final CustomerEventRepository customerEventRepository;
    private final PurchaseRepository purchaseRepository;
    private final PurTypeRepository purTypeRepository;
    private final PurStatusRepository purStatusRepository;
    private final PurWarehouseRepository purWarehouseRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    /** True when business tables already hold data (seeding is skipped). */
    @Transactional(readOnly = true)
    public boolean alreadySeeded() {
        return productRepository.count() > 30
                || customerRepository.count() > 10
                || supplierRepository.count() > 5;
    }

    // =========================================================================
    // Products
    // =========================================================================

    @Transactional
    public int seedProducts(Random rng, int target) {
        List<ProductVariant> space = buildVariantSpace();
        List<Product> batch = new ArrayList<>(BATCH);
        List<long[]> backdates = new ArrayList<>();
        int created = 0;
        for (int i = 0; i < target; i++) {
            ProductVariant v = space.get(i % space.size());
            int serial = i + 1;
            Product p = toProduct(v, serial, rng);
            batch.add(p);
            if (batch.size() == BATCH) {
                created += flushProducts(batch, backdates, rng);
            }
        }
        created += flushProducts(batch, backdates, rng);
        applyBackdate("products", backdates);
        log.info("Demo: seeded {} products", created);
        return created;
    }

    private int flushProducts(List<Product> batch, List<long[]> backdates, Random rng) {
        if (batch.isEmpty()) {
            return 0;
        }
        List<Product> saved = productRepository.saveAll(batch);
        for (Product p : saved) {
            backdates.add(new long[]{p.getId(), randomPastMillis(rng, 1000)});
        }
        int n = batch.size();
        batch.clear();
        return n;
    }

    private Product toProduct(ProductVariant v, int serial, Random rng) {
        ProductTemplate t = v.template();
        StringBuilder name = new StringBuilder(t.brand()).append(' ').append(t.model());
        if (v.storage() != null) {
            name.append(' ').append(v.storage());
        }
        name.append(" - ").append(v.color());
        if (v.used()) {
            name.append(" (کارکرده)");
        }

        long price = variantPrice(t, v, rng);
        String warranty = v.used()
                ? "گارانتی فروشگاه (کارکرده)"
                : DemoDataPools.PRODUCT_WARRANTIES[rng.nextInt(DemoDataPools.PRODUCT_WARRANTIES.length)];

        StringBuilder desc = new StringBuilder();
        desc.append("دسته‌بندی: ").append(t.category()).append(" — برند: ").append(t.brand()).append('\n');
        desc.append(t.specs()).append('\n');
        desc.append("رنگ: ").append(v.color());
        if (v.storage() != null) {
            desc.append(" — حافظه: ").append(v.storage());
        }
        desc.append('\n').append("گارانتی: ").append(warranty);
        if (t.imei()) {
            desc.append(" — دارای ثبت IMEI");
        } else if (t.serial()) {
            desc.append(" — دارای شماره سریال");
        }

        int stock = stockFor(t.category(), v.used(), rng);
        boolean active = rng.nextInt(100) < 93 && !(stock == 0 && rng.nextBoolean());

        return Product.builder()
                .name(name.toString())
                .sku(buildSku(t, serial))
                .description(desc.toString())
                .price(BigDecimal.valueOf(price))
                .stockQuantity(stock)
                .active(active)
                .build();
    }

    /** All realistic (template × storage × colour × condition) combinations. */
    private List<ProductVariant> buildVariantSpace() {
        List<ProductVariant> out = new ArrayList<>();
        for (ProductTemplate t : DemoDataPools.PRODUCTS) {
            String[] storages = t.storages().length == 0 ? new String[]{null} : t.storages();
            boolean tradesUsed = tradesUsed(t.category());
            for (String storage : storages) {
                for (String color : t.colors()) {
                    out.add(new ProductVariant(t, storage, color, false));
                    if (tradesUsed) {
                        out.add(new ProductVariant(t, storage, color, true));
                    }
                }
            }
        }
        return out;
    }

    private boolean tradesUsed(String category) {
        return switch (category) {
            case "گوشی موبایل", "تبلت", "کنسول بازی", "ساعت هوشمند" -> true;
            default -> false;
        };
    }

    private long variantPrice(ProductTemplate t, ProductVariant v, Random rng) {
        double price = t.basePrice();
        // Storage premium: each tier above the first adds ~15%.
        if (v.storage() != null && t.storages().length > 1) {
            int idx = indexOf(t.storages(), v.storage());
            price *= 1 + 0.15 * Math.max(0, idx);
        }
        if (v.used()) {
            price *= 0.72;
        }
        price *= 0.97 + rng.nextDouble() * 0.06; // ±3% jitter
        long rounded = Math.round(price / 100_000d) * 100_000L;
        return Math.max(rounded, 100_000L);
    }

    private int stockFor(String category, boolean used, Random rng) {
        if (used) {
            return rng.nextInt(4); // 0..3 used units
        }
        return switch (category) {
            case "گوشی موبایل", "تبلت" -> rng.nextInt(16);            // 0..15
            case "کنسول بازی" -> rng.nextInt(9);                       // 0..8
            case "گیفت کارت", "لایسنس نرم‌افزار", "اشتراک دیجیتال" -> 20 + rng.nextInt(480);
            default -> 5 + rng.nextInt(120);                           // accessories
        };
    }

    private String buildSku(ProductTemplate t, int serial) {
        String brand = t.brand().replaceAll("[^A-Za-z]", "").toUpperCase();
        String code = brand.isEmpty() ? "DIG" : brand.substring(0, Math.min(4, brand.length()));
        return code + "-" + String.format("%06d", serial);
    }

    // =========================================================================
    // Suppliers
    // =========================================================================

    @Transactional
    public int seedSuppliers(Random rng, int target) {
        SupType type = supTypeRepository.findByIsDefaultTrue()
                .orElseGet(() -> supTypeRepository.findAllByOrderByDisplayOrderAsc().get(0));
        SupStatus active = supStatusRepository.findByIsDefaultTrue()
                .orElseGet(() -> supStatusRepository.findAllByOrderByDisplayOrderAsc().get(0));
        List<SupPaymentTerm> terms = supPaymentTermRepository.findAllByOrderByDisplayOrderAsc();
        List<SupCategory> categories = supCategoryRepository.findAllByOrderByNameAsc();
        List<SupTag> tags = supTagRepository.findAllByOrderByNameAsc();
        List<SupplierTemplate> templates = DemoDataPools.SUPPLIERS;

        List<long[]> backdates = new ArrayList<>();
        int created = 0;
        for (int i = 0; i < target; i++) {
            SupplierTemplate tpl = templates.get(i % templates.size());
            String companyName = i < templates.size() ? tpl.name() : tpl.name() + " " + ((i / templates.size()) + 1);
            City city = DemoDataPools.CITIES.get(rng.nextInt(DemoDataPools.CITIES.size()));
            long code = supplierRepository.nextCodeNumber();

            Supplier s = Supplier.builder()
                    .supplierCode("SUP-" + String.format("%06d", code))
                    .companyName(companyName)
                    .displayName(companyName)
                    .legalName(companyName + " (سهامی خاص)")
                    .nationalId(randomDigits(rng, 11))
                    .economicCode(randomDigits(rng, 12))
                    .taxNumber(randomDigits(rng, 10))
                    .ownerName(fullName(rng))
                    .country("ایران")
                    .province(city.province())
                    .city(city.name())
                    .postalCode(randomDigits(rng, 10))
                    .description("تأمین‌کننده " + tpl.category() + " در " + city.name())
                    .type(type)
                    .status(active)
                    .paymentTerm(terms.isEmpty() ? null : terms.get(rng.nextInt(terms.size())))
                    .creditLimit(BigDecimal.valueOf((1 + rng.nextInt(20)) * 100_000_000L))
                    .ratingAvg(BigDecimal.valueOf(3.5 + rng.nextInt(16) / 10.0).setScale(2, RoundingMode.HALF_UP))
                    .build();

            s.getChannels().add(SupChannel.builder().supplier(s)
                    .kind(SupChannel.Kind.PHONE).value(landline(rng, city.name()))
                    .label("دفتر مرکزی").isDefault(true).build());
            if (rng.nextBoolean()) {
                s.getChannels().add(SupChannel.builder().supplier(s)
                        .kind(SupChannel.Kind.EMAIL).value(companyEmail(companyName, i))
                        .label("پشتیبانی").isDefault(false).build());
            }
            s.getAddresses().add(SupAddress.builder().supplier(s)
                    .label("انبار").line(street(rng) + "، پلاک " + (1 + rng.nextInt(300)))
                    .city(city.name()).province(city.province())
                    .postalCode(randomDigits(rng, 10)).isDefault(true).build());
            s.getContacts().add(SupContact.builder().supplier(s)
                    .fullName(fullName(rng)).position("مدیر فروش").department("فروش")
                    .mobile(mobile(rng)).email(companyEmail(companyName, i * 7 + 1))
                    .preferredMethod(DemoDataPools.PAYMENT_METHOD_PREF[rng.nextInt(DemoDataPools.PAYMENT_METHOD_PREF.length)])
                    .isPrimary(true).build());

            assignRandom(categories, s.getCategories(), 1 + rng.nextInt(2), rng);
            assignRandom(tags, s.getTags(), rng.nextInt(3), rng);

            Supplier persisted = supplierRepository.save(s);
            backdates.add(new long[]{persisted.getId(), randomPastMillis(rng, 1200)});
            created++;
        }
        applyBackdate("suppliers", backdates);
        log.info("Demo: seeded {} suppliers", created);
        return created;
    }

    // =========================================================================
    // Customers (+ 360° timeline)
    // =========================================================================

    @Transactional
    public int seedCustomers(Random rng, int target) {
        CustomerType individual = customerTypeRepository.findByIsDefaultTrue()
                .orElseGet(() -> customerTypeRepository.findAllByOrderByDisplayOrderAsc().get(0));
        List<CustomerType> types = customerTypeRepository.findAllByOrderByDisplayOrderAsc();
        CustomerStatus activeStatus = customerStatusRepository.findByIsDefaultTrue()
                .orElseGet(() -> customerStatusRepository.findAllByOrderByDisplayOrderAsc().get(0));
        List<CustomerSource> sources = customerSourceRepository.findAllByOrderByDisplayOrderAsc();
        List<CustomerTag> tags = customerTagRepository.findAllByOrderByNameAsc();

        List<long[]> backdates = new ArrayList<>();
        List<CustomerEvent> events = new ArrayList<>();
        int created = 0;
        for (int i = 0; i < target; i++) {
            boolean female = rng.nextBoolean();
            String first = female
                    ? pick(DemoDataPools.FIRST_NAMES_FEMALE, rng)
                    : pick(DemoDataPools.FIRST_NAMES_MALE, rng);
            String last = pick(DemoDataPools.LAST_NAMES, rng);
            String display = first + " " + last;
            City city = DemoDataPools.CITIES.get(rng.nextInt(DemoDataPools.CITIES.size()));
            long code = customerRepository.nextCodeNumber();

            // ~15% businesses (VIP/Business types where available), rest individuals.
            CustomerType type = (rng.nextInt(100) < 15 && types.size() > 1)
                    ? types.get(rng.nextInt(types.size()))
                    : individual;

            Map<String, Object> prefs = new HashMap<>();
            if (rng.nextInt(100) < 40) {
                prefs.put("favorite_brand", DemoDataPools.PRODUCTS.get(rng.nextInt(DemoDataPools.PRODUCTS.size())).brand());
            }

            Customer c = Customer.builder()
                    .customerCode("C" + String.format("%06d", code))
                    .firstName(first)
                    .lastName(last)
                    .displayName(display)
                    .nationalCode(randomDigits(rng, 10))
                    .birthDate(LocalDate.of(1965 + rng.nextInt(45), 1 + rng.nextInt(12), 1 + rng.nextInt(28)))
                    .gender(female ? "female" : "male")
                    .occupation(pick(DemoDataPools.OCCUPATIONS, rng))
                    .type(type)
                    .status(activeStatus)
                    .source(sources.isEmpty() ? null : sources.get(rng.nextInt(sources.size())))
                    .preferences(prefs)
                    .build();

            c.getContacts().add(CustomerContact.builder().customer(c)
                    .kind(CustomerContact.Kind.PHONE).value(mobile(rng))
                    .label("موبایل").isDefault(true).build());
            if (rng.nextInt(100) < 45) {
                c.getContacts().add(CustomerContact.builder().customer(c)
                        .kind(CustomerContact.Kind.EMAIL).value(personalEmail(first, last, i))
                        .label("ایمیل").isDefault(false).build());
            }
            c.getAddresses().add(CustomerAddress.builder().customer(c)
                    .label("منزل").line(street(rng) + "، پلاک " + (1 + rng.nextInt(400)))
                    .city(city.name()).province(city.province())
                    .postalCode(randomDigits(rng, 10)).isDefault(true).build());
            assignRandom(tags, c.getTags(), rng.nextInt(3), rng);

            Customer persisted = customerRepository.save(c);
            long createdMillis = randomPastMillis(rng, 1100);
            backdates.add(new long[]{persisted.getId(), createdMillis});
            events.addAll(buildTimeline(persisted.getId(), createdMillis, display, rng));
            created++;

            if (events.size() >= 400) {
                customerEventRepository.saveAll(events);
                events.clear();
            }
        }
        if (!events.isEmpty()) {
            customerEventRepository.saveAll(events);
        }
        applyBackdate("customers", backdates);
        log.info("Demo: seeded {} customers with timeline history", created);
        return created;
    }

    /** A realistic customer timeline: registration + a purchase/repair/payment history. */
    private List<CustomerEvent> buildTimeline(Long customerId, long createdMillis, String display, Random rng) {
        List<CustomerEvent> list = new ArrayList<>();
        Instant createdAt = Instant.ofEpochMilli(createdMillis);
        list.add(CustomerEvent.builder()
                .customerId(customerId).eventType("CREATED").title("مشتری ثبت شد")
                .sourceModule("crm").occurredAt(createdAt).build());

        int purchases = rng.nextInt(6); // 0..5 past purchases
        long cursor = createdMillis;
        for (int i = 0; i < purchases; i++) {
            cursor += (long) (5 + rng.nextInt(180)) * 24 * 3600 * 1000L;
            if (cursor > System.currentTimeMillis()) {
                break;
            }
            ProductTemplate t = DemoDataPools.PRODUCTS.get(rng.nextInt(DemoDataPools.PRODUCTS.size()));
            long amount = t.basePrice();
            Map<String, Object> detail = new HashMap<>();
            detail.put("product", t.brand() + " " + t.model());
            detail.put("amount", amount);
            detail.put("payment", rng.nextBoolean() ? "cash" : "installment");
            list.add(CustomerEvent.builder()
                    .customerId(customerId).eventType("PURCHASE")
                    .title("خرید " + t.brand() + " " + t.model())
                    .detail(detail).sourceModule("sales")
                    .occurredAt(Instant.ofEpochMilli(cursor)).build());

            if (rng.nextInt(100) < 22) { // some come back for repairs
                cursor += (long) (20 + rng.nextInt(200)) * 24 * 3600 * 1000L;
                if (cursor > System.currentTimeMillis()) {
                    break;
                }
                String[] repairs = {"تعویض گلس", "تعویض باتری", "تعمیر برد شارژ", "تعمیر دوربین", "خرابی آبخوردگی"};
                Map<String, Object> rd = new HashMap<>();
                rd.put("issue", repairs[rng.nextInt(repairs.length)]);
                list.add(CustomerEvent.builder()
                        .customerId(customerId).eventType("REPAIR_REQUEST")
                        .title("پذیرش تعمیر: " + rd.get("issue"))
                        .detail(rd).sourceModule("repairs")
                        .occurredAt(Instant.ofEpochMilli(cursor)).build());
            }
        }
        return list;
    }

    // =========================================================================
    // Purchases (link suppliers + products)
    // =========================================================================

    @Transactional
    public int seedPurchases(Random rng, int target) {
        List<Supplier> suppliers = supplierRepository.findAll();
        List<Product> products = productRepository.findAll();
        if (suppliers.isEmpty() || products.isEmpty()) {
            log.warn("Demo: no suppliers/products available; skipping purchases");
            return 0;
        }
        PurType type = purTypeRepository.findByIsDefaultTrue()
                .orElseGet(() -> purTypeRepository.findAllByOrderByDisplayOrderAsc().get(0));
        List<PurWarehouse> warehouses = purWarehouseRepository.findAllByOrderByDisplayOrderAsc();
        PurStatus draft = purStatusRepository.findByIsDraftStateTrue().orElse(null);
        PurStatus pending = purStatusRepository.findByIsPendingStateTrue().orElse(null);
        PurStatus approved = purStatusRepository.findByIsApprovedStateTrue().orElse(null);
        PurStatus completed = purStatusRepository.findByIsCompletedStateTrue().orElse(approved);
        PurStatus cancelled = purStatusRepository.findByIsCancelledStateTrue().orElse(null);

        Long[] actor = resolveActor();
        List<long[]> backdates = new ArrayList<>();
        int created = 0;
        for (int i = 0; i < target; i++) {
            Supplier supplier = suppliers.get(rng.nextInt(suppliers.size()));
            long createdMillis = weightedRecentMillis(rng, 900);
            int year = Instant.ofEpochMilli(createdMillis).atZone(ZoneOffset.UTC).getYear();
            long seq = purchaseRepository.nextNumber();

            PurStatus status = pickPurStatus(rng, draft, pending, approved, completed, cancelled);
            boolean received = status == completed;
            boolean approvedOrLater = status == approved || status == completed;

            Purchase p = Purchase.builder()
                    .purchaseNumber(type.getNumberPrefix() + "-" + year + "-" + String.format("%06d", seq))
                    .type(type)
                    .status(status)
                    .supplier(supplier)
                    .warehouse(warehouses.isEmpty() ? null : warehouses.get(rng.nextInt(warehouses.size())))
                    .notes("سفارش خرید از " + supplier.getDisplayName())
                    .createdBy(actor[0])
                    .createdByEmail(actor[0] == null ? null : "admin@sami.local")
                    .build();

            int lines = 1 + rng.nextInt(5);
            BigDecimal total = BigDecimal.ZERO;
            Set<Long> usedProducts = new LinkedHashSet<>();
            for (int l = 0; l < lines; l++) {
                Product product = products.get(rng.nextInt(products.size()));
                if (!usedProducts.add(product.getId())) {
                    continue;
                }
                BigDecimal qty = BigDecimal.valueOf(1 + rng.nextInt(20));
                // Cost = 60–80% of retail.
                BigDecimal unitCost = product.getPrice()
                        .multiply(BigDecimal.valueOf(0.60 + rng.nextDouble() * 0.20))
                        .setScale(0, RoundingMode.HALF_UP);
                BigDecimal discount = rng.nextInt(100) < 25
                        ? unitCost.multiply(qty).multiply(BigDecimal.valueOf(0.05)).setScale(0, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                PurchaseItem item = PurchaseItem.builder()
                        .purchase(p).product(product)
                        .description(product.getName())
                        .quantity(qty).unit("piece").unitPrice(unitCost).discount(discount)
                        .requiresImei(product.getName().contains("iPhone") || product.getName().contains("Galaxy"))
                        .receivedQuantity(received ? qty : BigDecimal.ZERO)
                        .build();
                p.getItems().add(item);
                total = total.add(item.lineTotal());
            }
            p.setTotalAmount(total);

            Instant ts = Instant.ofEpochMilli(createdMillis);
            if (approvedOrLater || status == pending) {
                p.setSubmittedAt(ts);
            }
            if (approvedOrLater) {
                p.setApprovedAt(ts.plus(1, ChronoUnit.DAYS));
                p.setApprovedBy(actor[0]);
            }
            if (status == cancelled) {
                p.setCancelledAt(ts.plus(1, ChronoUnit.DAYS));
                p.setCancelledBy(actor[0]);
                p.setCancelNote("لغو شده توسط تأمین‌کننده");
            }

            Purchase persisted = purchaseRepository.save(p);
            backdates.add(new long[]{persisted.getId(), createdMillis});
            created++;
        }
        applyBackdate("purchases", backdates);
        log.info("Demo: seeded {} purchase orders", created);
        return created;
    }

    private PurStatus pickPurStatus(Random rng, PurStatus draft, PurStatus pending, PurStatus approved,
                                    PurStatus completed, PurStatus cancelled) {
        int r = rng.nextInt(100);
        if (r < 55 && completed != null) {
            return completed;
        }
        if (r < 72 && approved != null) {
            return approved;
        }
        if (r < 84 && pending != null) {
            return pending;
        }
        if (r < 94 && draft != null) {
            return draft;
        }
        if (cancelled != null) {
            return cancelled;
        }
        return completed != null ? completed : (approved != null ? approved : draft);
    }

    private Long[] resolveActor() {
        return userRepository.findAll().stream().findFirst()
                .map(u -> new Long[]{u.getId()})
                .orElse(new Long[]{null});
    }

    // =========================================================================
    // Shared helpers
    // =========================================================================

    private void applyBackdate(String table, List<long[]> rows) {
        if (rows.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(
                "UPDATE " + table + " SET created_at = ?, updated_at = ? WHERE id = ?",
                rows,
                rows.size(),
                (ps, row) -> {
                    Timestamp ts = new Timestamp(row[1]);
                    ps.setTimestamp(1, ts);
                    ps.setTimestamp(2, ts);
                    ps.setLong(3, row[0]);
                });
    }

    private <T> void assignRandom(List<T> pool, Set<T> target, int count, Random rng) {
        if (pool.isEmpty() || count <= 0) {
            return;
        }
        for (int i = 0; i < count; i++) {
            target.add(pool.get(rng.nextInt(pool.size())));
        }
    }

    private long randomPastMillis(Random rng, int maxDaysAgo) {
        long daysAgo = rng.nextInt(maxDaysAgo);
        long secondsIntoDay = rng.nextInt(86_400);
        return Instant.now()
                .minus(daysAgo, ChronoUnit.DAYS)
                .minusSeconds(secondsIntoDay)
                .toEpochMilli();
    }

    /** Skews towards recent dates so charts trend upward like a growing business. */
    private long weightedRecentMillis(Random rng, int maxDaysAgo) {
        double skew = rng.nextDouble() * rng.nextDouble(); // biased to 0 (recent)
        long daysAgo = (long) (skew * maxDaysAgo);
        long secondsIntoDay = rng.nextInt(86_400);
        return Instant.now()
                .minus(daysAgo, ChronoUnit.DAYS)
                .minusSeconds(secondsIntoDay)
                .toEpochMilli();
    }

    private String fullName(Random rng) {
        boolean female = rng.nextBoolean();
        String first = female
                ? pick(DemoDataPools.FIRST_NAMES_FEMALE, rng)
                : pick(DemoDataPools.FIRST_NAMES_MALE, rng);
        return first + " " + pick(DemoDataPools.LAST_NAMES, rng);
    }

    private String street(Random rng) {
        return pick(DemoDataPools.STREET_NAMES, rng);
    }

    private String mobile(Random rng) {
        String[] prefixes = {"0912", "0919", "0913", "0935", "0938", "0901", "0903", "0990", "0991", "0937"};
        return prefixes[rng.nextInt(prefixes.length)] + randomDigits(rng, 7);
    }

    private String landline(Random rng, String city) {
        return "0" + (rng.nextInt(80) + 11) + randomDigits(rng, 8);
    }

    private String randomDigits(Random rng, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(rng.nextInt(10));
        }
        return sb.toString();
    }

    private String companyEmail(String companyName, int salt) {
        return "info" + (Math.abs(companyName.hashCode()) % 1000) + salt + "@tejarat.example.ir";
    }

    private String personalEmail(String first, String last, int salt) {
        return "user" + salt + Math.abs((first + last).hashCode()) % 10000 + "@gmail.com";
    }

    private static <T> T pick(T[] arr, Random rng) {
        return arr[rng.nextInt(arr.length)];
    }

    private static int indexOf(String[] arr, String value) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(value)) {
                return i;
            }
        }
        return 0;
    }

    private record ProductVariant(ProductTemplate template, String storage, String color, boolean used) {
    }
}
