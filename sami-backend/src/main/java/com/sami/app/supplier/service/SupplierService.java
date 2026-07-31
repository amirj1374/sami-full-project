package com.sami.app.supplier.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.security.CurrentActor;
import com.sami.app.supplier.SupplierProperties;
import com.sami.app.supplier.domain.SupAddress;
import com.sami.app.supplier.domain.SupBankAccount;
import com.sami.app.supplier.domain.SupChannel;
import com.sami.app.supplier.domain.SupChannel.Kind;
import com.sami.app.supplier.domain.SupContact;
import com.sami.app.supplier.domain.SupStatus;
import com.sami.app.supplier.domain.Supplier;
import com.sami.app.supplier.dto.SupplierDtos.AddressDto;
import com.sami.app.supplier.dto.SupplierDtos.BankAccountDto;
import com.sami.app.supplier.dto.SupplierDtos.ChannelDto;
import com.sami.app.supplier.dto.SupplierDtos.ContactDto;
import com.sami.app.supplier.dto.SupplierDtos.SupplierDetailResponse;
import com.sami.app.supplier.dto.SupplierDtos.SupplierFilter;
import com.sami.app.supplier.dto.SupplierDtos.SupplierRequest;
import com.sami.app.supplier.dto.SupplierDtos.SupplierRowResponse;
import com.sami.app.supplier.repository.SupCategoryRepository;
import com.sami.app.supplier.repository.SupDuplicateRuleRepository;
import com.sami.app.supplier.repository.SupPaymentTermRepository;
import com.sami.app.supplier.repository.SupStatusRepository;
import com.sami.app.supplier.repository.SupTagRepository;
import com.sami.app.supplier.repository.SupTypeRepository;
import com.sami.app.supplier.repository.SupplierRepository;
import com.sami.app.supplier.repository.SupplierSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Supplier lifecycle: CRUD with configurable duplicate-identifier rules,
 * centralized contact/bank validation, archive / soft delete / restore /
 * purge, and CSV export. Every mutation lands in the supplier log and is
 * republished as a business event.
 */
@Service
@RequiredArgsConstructor
public class SupplierService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern IBAN = Pattern.compile("^[A-Z]{2}[0-9A-Z]{13,32}$");
    private static final Pattern CARD = Pattern.compile("^[0-9]{16}$");

    private final SupplierRepository supplierRepository;
    private final SupTypeRepository typeRepository;
    private final SupStatusRepository statusRepository;
    private final SupPaymentTermRepository paymentTermRepository;
    private final SupTagRepository tagRepository;
    private final SupCategoryRepository categoryRepository;
    private final SupDuplicateRuleRepository duplicateRuleRepository;
    private final SupplierProperties properties;
    private final SupLogService logs;

    // ----------------------------------------------------------------- reads

    @Transactional(readOnly = true)
    public Page<SupplierRowResponse> list(SupplierFilter filter, Pageable pageable) {
        return supplierRepository.findAll(toSpecification(filter), pageable)
                .map(SupplierRowResponse::from);
    }

    @Transactional(readOnly = true)
    public SupplierDetailResponse getDetail(Long id) {
        return SupplierDetailResponse.from(findWithDetailsOrThrow(id));
    }

    @Transactional(readOnly = true)
    public String exportCsv(SupplierFilter filter) {
        List<Supplier> suppliers = supplierRepository.findAll(toSpecification(filter),
                Sort.by(Sort.Direction.ASC, "id"));
        StringBuilder csv = new StringBuilder(
                "\uFEFFcode,companyName,displayName,city,type,status,paymentTerm,rating,defaultPhone,defaultEmail,taxNumber,createdAt\n");
        for (Supplier s : suppliers) {
            csv.append(csvRow(
                    s.getSupplierCode(), s.getCompanyName(), s.getDisplayName(), s.getCity(),
                    s.getType().getName(), s.getStatus().getName(),
                    s.getPaymentTerm() != null ? s.getPaymentTerm().getName() : null,
                    s.getRatingAvg() != null ? s.getRatingAvg().toPlainString() : null,
                    defaultChannel(s, Kind.PHONE), defaultChannel(s, Kind.EMAIL),
                    s.getTaxNumber(), s.getCreatedAt().toString()));
        }
        return csv.toString();
    }

    // ----------------------------------------------------------------- write

    @Transactional
    public SupplierDetailResponse create(SupplierRequest request) {
        enforceDuplicatePolicy(request, null);

        SupStatus status = request.statusId() != null
                ? findStatusOrThrow(request.statusId())
                : statusRepository.findByIsDefaultTrue()
                        .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                                "No default supplier status is configured"));

        Supplier supplier = Supplier.builder()
                .supplierCode(nextSupplierCode())
                .companyName(request.companyName().trim())
                .displayName(request.displayName().trim())
                .type(typeRepository.findById(request.typeId())
                        .orElseThrow(() -> ResourceNotFoundException.of("Supplier type",
                                request.typeId())))
                .status(status)
                .build();
        applyScalarFields(supplier, request);
        applyLinks(supplier, request);
        supplierRepository.save(supplier);
        applyChildren(supplier, request);

        logs.record(supplier, SupLogService.CREATED, "Supplier created",
                Map.of("code", supplier.getSupplierCode()));
        return SupplierDetailResponse.from(supplier);
    }

    @Transactional
    public SupplierDetailResponse update(Long id, SupplierRequest request) {
        Supplier supplier = findWithDetailsOrThrow(id);
        if (request.expectedVersion() != null
                && !request.expectedVersion().equals(supplier.getVersion())) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "The supplier was modified by someone else; reload and retry");
        }
        enforceDuplicatePolicy(request, id);

        supplier.setCompanyName(request.companyName().trim());
        supplier.setDisplayName(request.displayName().trim());
        supplier.setType(typeRepository.findById(request.typeId())
                .orElseThrow(() -> ResourceNotFoundException.of("Supplier type", request.typeId())));
        if (request.statusId() != null
                && !request.statusId().equals(supplier.getStatus().getId())) {
            SupStatus target = findStatusOrThrow(request.statusId());
            if (target.isArchivedState() || target.isDeletedState()) {
                throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "Use the archive/delete operations for lifecycle statuses");
            }
            String from = supplier.getStatus().getName();
            supplier.setStatus(target);
            logs.record(supplier, SupLogService.STATUS_CHANGED, "Status changed",
                    Map.of("from", from, "to", target.getName()));
        }
        applyScalarFields(supplier, request);
        applyLinks(supplier, request);
        applyChildren(supplier, request);

        logs.record(supplier, SupLogService.UPDATED, "Supplier updated", null);
        return SupplierDetailResponse.from(supplier);
    }

    // ------------------------------------------------------------- lifecycle

    @Transactional
    public SupplierRowResponse archive(Long id) {
        Supplier supplier = findOrThrow(id);
        if (supplier.getStatus().isArchivedState() || supplier.getStatus().isDeletedState()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "The supplier is already archived or deleted");
        }
        supplier.setStatus(structural(statusRepository.findByIsArchivedStateTrue(), "archived"));
        supplier.setArchivedAt(Instant.now());
        supplier.setArchivedBy(CurrentActor.id());
        logs.record(supplier, SupLogService.ARCHIVED, "Supplier archived", null);
        return SupplierRowResponse.from(supplier);
    }

    @Transactional
    public SupplierRowResponse restore(Long id) {
        Supplier supplier = findOrThrow(id);
        if (!supplier.getStatus().isArchivedState() && !supplier.getStatus().isDeletedState()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Only archived or deleted suppliers can be restored");
        }
        supplier.setStatus(structural(statusRepository.findByIsDefaultTrue(), "default"));
        supplier.setArchivedAt(null);
        supplier.setArchivedBy(null);
        supplier.setDeletedAt(null);
        supplier.setDeletedBy(null);
        logs.record(supplier, SupLogService.RESTORED, "Supplier restored", null);
        return SupplierRowResponse.from(supplier);
    }

    @Transactional
    public SupplierRowResponse softDelete(Long id) {
        Supplier supplier = findOrThrow(id);
        if (supplier.getStatus().isDeletedState()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "The supplier is already deleted");
        }
        supplier.setStatus(structural(statusRepository.findByIsDeletedStateTrue(), "deleted"));
        supplier.setDeletedAt(Instant.now());
        supplier.setDeletedBy(CurrentActor.id());
        logs.record(supplier, SupLogService.DELETED, "Supplier deleted (soft)", null);
        return SupplierRowResponse.from(supplier);
    }

    /** Physical removal: two-step gated; refused while purchases reference it. */
    @Transactional
    public void purge(Long id) {
        Supplier supplier = findWithDetailsOrThrow(id);
        if (!supplier.getStatus().isDeletedState()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Only soft-deleted suppliers can be permanently removed");
        }
        supplierRepository.delete(supplier);
        // A referencing purchase makes the DELETE fail on the FK — surfaced as a
        // conflict by the global handler; records with history stay resolvable.
    }

    // -------------------------------------------------------------- internals

    private void enforceDuplicatePolicy(SupplierRequest request, Long excludeId) {
        if (request.ignoreDuplicates()) {
            return;
        }
        long exclude = excludeId == null ? -1L : excludeId;
        for (var rule : duplicateRuleRepository.findByEnabledTrue()) {
            String value = switch (rule.getIdentifier()) {
                case TAX_NUMBER -> request.taxNumber();
                case NATIONAL_ID -> request.nationalId();
                case ECONOMIC_CODE -> request.economicCode();
                case REGISTRATION_NUMBER -> request.registrationNumber();
            };
            if (value == null || value.isBlank()) {
                continue;
            }
            String attribute = switch (rule.getIdentifier()) {
                case TAX_NUMBER -> "taxNumber";
                case NATIONAL_ID -> "nationalId";
                case ECONOMIC_CODE -> "economicCode";
                case REGISTRATION_NUMBER -> "registrationNumber";
            };
            Specification<Supplier> spec = (root, query, cb) -> cb.and(
                    cb.equal(root.get(attribute), value.trim()),
                    cb.notEqual(root.get("id"), exclude));
            List<Supplier> matches = supplierRepository.findAll(spec);
            if (!matches.isEmpty()) {
                throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                        "A supplier with the same %s already exists: %s. Confirm to save anyway."
                                .formatted(rule.getIdentifier().name().toLowerCase().replace('_', ' '),
                                        matches.get(0).getSupplierCode()));
            }
        }
    }

    private void applyScalarFields(Supplier s, SupplierRequest r) {
        s.setLegalName(blankToNull(r.legalName()));
        s.setNationalId(blankToNull(r.nationalId()));
        s.setEconomicCode(blankToNull(r.economicCode()));
        s.setTaxNumber(blankToNull(r.taxNumber()));
        s.setRegistrationNumber(blankToNull(r.registrationNumber()));
        s.setOwnerName(blankToNull(r.ownerName()));
        s.setWebsite(blankToNull(r.website()));
        s.setCountry(blankToNull(r.country()));
        s.setProvince(blankToNull(r.province()));
        s.setCity(blankToNull(r.city()));
        s.setPostalCode(blankToNull(r.postalCode()));
        s.setDescription(blankToNull(r.description()));
        s.setPaymentTerm(r.paymentTermId() == null ? null
                : paymentTermRepository.findById(r.paymentTermId())
                        .orElseThrow(() -> ResourceNotFoundException.of("Payment term",
                                r.paymentTermId())));
        s.setCreditLimit(r.creditLimit());
    }

    private void applyLinks(Supplier supplier, SupplierRequest request) {
        if (request.tagIds() != null) {
            var tags = tagRepository.findAllById(request.tagIds());
            if (tags.size() != new HashSet<>(request.tagIds()).size()) {
                throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "One or more tags do not exist");
            }
            supplier.getTags().clear();
            supplier.getTags().addAll(tags);
        }
        if (request.categoryIds() != null) {
            var categories = categoryRepository.findAllById(request.categoryIds());
            if (categories.size() != new HashSet<>(request.categoryIds()).size()) {
                throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND,
                        "One or more categories do not exist");
            }
            supplier.getCategories().clear();
            supplier.getCategories().addAll(categories);
        }
    }

    /** Full replacement of channels/addresses/contacts/banks, centrally validated. */
    private void applyChildren(Supplier supplier, SupplierRequest request) {
        if (request.channels() != null) {
            requireSingleDefault(request.channels().stream()
                    .filter(c -> c.kind() == Kind.PHONE).filter(ChannelDto::isDefault).count(), "phone");
            requireSingleDefault(request.channels().stream()
                    .filter(c -> c.kind() == Kind.EMAIL).filter(ChannelDto::isDefault).count(), "email");
            request.channels().stream()
                    .filter(c -> c.kind() == Kind.EMAIL)
                    .forEach(c -> requireValid(EMAIL, c.value().trim(), "email address"));
            replace(supplier, supplier.getChannels(), request.channels(), dto ->
                    SupChannel.builder().supplier(supplier).kind(dto.kind())
                            .value(dto.value().trim()).label(blankToNull(dto.label()))
                            .isDefault(dto.isDefault()).build());
        }
        if (request.addresses() != null) {
            requireSingleDefault(request.addresses().stream()
                    .filter(AddressDto::isDefault).count(), "address");
            replace(supplier, supplier.getAddresses(), request.addresses(), dto ->
                    SupAddress.builder().supplier(supplier).label(blankToNull(dto.label()))
                            .line(dto.line().trim()).city(blankToNull(dto.city()))
                            .province(blankToNull(dto.province()))
                            .postalCode(blankToNull(dto.postalCode()))
                            .isDefault(dto.isDefault()).build());
        }
        if (request.contacts() != null) {
            requireSingleDefault(request.contacts().stream()
                    .filter(ContactDto::isPrimary).count(), "primary contact");
            request.contacts().stream()
                    .map(ContactDto::email)
                    .filter(e -> e != null && !e.isBlank())
                    .forEach(e -> requireValid(EMAIL, e.trim(), "contact email"));
            replace(supplier, supplier.getContacts(), request.contacts(), dto ->
                    SupContact.builder().supplier(supplier).fullName(dto.fullName().trim())
                            .position(blankToNull(dto.position()))
                            .department(blankToNull(dto.department()))
                            .phone(blankToNull(dto.phone())).mobile(blankToNull(dto.mobile()))
                            .email(blankToNull(dto.email()))
                            .preferredMethod(blankToNull(dto.preferredMethod()))
                            .notes(blankToNull(dto.notes()))
                            .isPrimary(dto.isPrimary()).build());
        }
        if (request.bankAccounts() != null) {
            requireSingleDefault(request.bankAccounts().stream()
                    .filter(BankAccountDto::isDefault).count(), "bank account");
            for (BankAccountDto dto : request.bankAccounts()) {
                if (dto.iban() != null && !dto.iban().isBlank()) {
                    requireValid(IBAN, dto.iban().trim().toUpperCase().replace(" ", ""), "IBAN");
                }
                if (dto.cardNumber() != null && !dto.cardNumber().isBlank()) {
                    requireValid(CARD, dto.cardNumber().trim().replace(" ", ""), "card number");
                }
            }
            replace(supplier, supplier.getBankAccounts(), request.bankAccounts(), dto ->
                    SupBankAccount.builder().supplier(supplier).bankName(dto.bankName().trim())
                            .accountNumber(blankToNull(dto.accountNumber()))
                            .iban(dto.iban() == null || dto.iban().isBlank() ? null
                                    : dto.iban().trim().toUpperCase().replace(" ", ""))
                            .cardNumber(dto.cardNumber() == null || dto.cardNumber().isBlank() ? null
                                    : dto.cardNumber().trim().replace(" ", ""))
                            .accountHolder(blankToNull(dto.accountHolder()))
                            .isDefault(dto.isDefault())
                            .extra(dto.extra() == null ? Map.of() : dto.extra())
                            .build());
        }
    }

    /**
     * Clears a child collection, flushes the deletions (partial unique indexes
     * on default flags would otherwise trip on insert-before-delete ordering),
     * then rebuilds it from the DTOs.
     */
    private <D, E> void replace(Supplier supplier, List<E> target, List<D> dtos,
                                Function<D, E> mapper) {
        target.clear();
        if (supplier.getId() != null) {
            supplierRepository.flush();
        }
        dtos.forEach(dto -> target.add(mapper.apply(dto)));
    }

    private void requireValid(Pattern pattern, String value, String label) {
        if (!pattern.matcher(value).matches()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "'%s' is not a valid %s".formatted(value, label));
        }
    }

    private void requireSingleDefault(long count, String label) {
        if (count > 1) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Only one default " + label + " is allowed");
        }
    }

    private Specification<Supplier> toSpecification(SupplierFilter f) {
        boolean explicitStatus = f.statusId() != null;
        boolean includeHidden = Boolean.TRUE.equals(f.includeHidden());
        return Specification.allOf(
                SupplierSpecifications.globalSearch(f.search()),
                SupplierSpecifications.hasChannel(Kind.PHONE, f.phone()),
                SupplierSpecifications.hasChannel(Kind.EMAIL, f.email()),
                SupplierSpecifications.contactNameContains(f.contactName()),
                SupplierSpecifications.cityContains(f.city()),
                SupplierSpecifications.hasStatus(f.statusId()),
                SupplierSpecifications.hasType(f.typeId()),
                SupplierSpecifications.hasAnyTag(f.tagIds()),
                SupplierSpecifications.hasAnyCategory(f.categoryIds()),
                SupplierSpecifications.ratingAtLeast(f.minRating()),
                explicitStatus || includeHidden ? null : SupplierSpecifications.visibleByDefault());
    }

    private String nextSupplierCode() {
        long number = supplierRepository.nextCodeNumber();
        return properties.codePrefix()
                + String.format("%0" + properties.codePadding() + "d", number);
    }

    private String defaultChannel(Supplier s, Kind kind) {
        return s.getChannels().stream()
                .filter(c -> c.getKind() == kind)
                .sorted((a, b) -> Boolean.compare(b.isDefault(), a.isDefault()))
                .map(SupChannel::getValue)
                .findFirst()
                .orElse(null);
    }

    private String csvRow(String... cells) {
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                row.append(',');
            }
            String cell = cells[i] == null ? "" : cells[i];
            if (cell.contains(",") || cell.contains("\"") || cell.contains("\n")) {
                cell = '"' + cell.replace("\"", "\"\"") + '"';
            }
            row.append(cell);
        }
        return row.append('\n').toString();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private SupStatus structural(java.util.Optional<SupStatus> status, String role) {
        return status.orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                "No " + role + " supplier status is configured"));
    }

    private SupStatus findStatusOrThrow(Long id) {
        return statusRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Supplier status", id));
    }

    Supplier findOrThrow(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Supplier", id));
    }

    Supplier findWithDetailsOrThrow(Long id) {
        return supplierRepository.findWithDetailsById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Supplier", id));
    }
}
