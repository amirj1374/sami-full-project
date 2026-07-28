package com.sami.app.crm.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.storage.FileStorage;
import com.sami.app.common.storage.ImageUploads;
import com.sami.app.common.storage.StorageProperties;
import com.sami.app.crm.CrmProperties;
import com.sami.app.crm.domain.Customer;
import com.sami.app.crm.domain.CustomerAddress;
import com.sami.app.crm.domain.CustomerContact;
import com.sami.app.crm.domain.CustomerStatus;
import com.sami.app.crm.domain.CustomerTag;
import com.sami.app.crm.dto.AddressDto;
import com.sami.app.crm.dto.ContactDto;
import com.sami.app.crm.dto.CustomerDetailResponse;
import com.sami.app.crm.dto.CustomerFilter;
import com.sami.app.crm.dto.CustomerRequest;
import com.sami.app.crm.dto.CustomerResponse;
import com.sami.app.crm.dto.DuplicateMatchResponse;
import com.sami.app.crm.repository.CustomerRepository;
import com.sami.app.crm.repository.CustomerSourceRepository;
import com.sami.app.crm.repository.CustomerSpecifications;
import com.sami.app.crm.repository.CustomerStatusRepository;
import com.sami.app.crm.repository.CustomerTagRepository;
import com.sami.app.crm.repository.CustomerTypeRepository;
import com.sami.app.crm.domain.CustomerContact.Kind;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Customer lifecycle: CRUD with configurable duplicate prevention, status
 * transitions, archive / soft delete / restore / purge, avatar, CSV export.
 *
 * <p>Every mutation appends timeline events in the same transaction, so the
 * 360° history is complete by construction. Merged-away records are excluded
 * from every listing and check but are never physically touched.
 */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerTypeRepository typeRepository;
    private final CustomerStatusRepository statusRepository;
    private final CustomerSourceRepository sourceRepository;
    private final CustomerTagRepository tagRepository;
    private final DuplicateDetectionService duplicateDetection;
    private final CustomerEventService events;
    private final FileStorage fileStorage;
    private final StorageProperties storageProperties;
    private final CrmProperties crmProperties;

    // ----------------------------------------------------------------- reads

    @Transactional(readOnly = true)
    public Page<CustomerResponse> list(CustomerFilter filter, Pageable pageable) {
        return customerRepository.findAll(toSpecification(filter), pageable)
                .map(CustomerResponse::from);
    }

    @Transactional(readOnly = true)
    public CustomerDetailResponse getDetail(Long id) {
        return CustomerDetailResponse.from(findWithDetailsOrThrow(id));
    }

    @Transactional(readOnly = true)
    public String exportCsv(CustomerFilter filter) {
        List<Customer> customers = customerRepository.findAll(toSpecification(filter),
                Sort.by(Sort.Direction.ASC, "id"));
        StringBuilder csv = new StringBuilder(
                "code,displayName,firstName,lastName,company,nationalCode,defaultPhone,defaultEmail,type,status,source,createdAt\n");
        for (Customer c : customers) {
            csv.append(csvRow(
                    c.getCustomerCode(), c.getDisplayName(), c.getFirstName(), c.getLastName(),
                    c.getCompanyName(), c.getNationalCode(),
                    defaultContact(c, Kind.PHONE), defaultContact(c, Kind.EMAIL),
                    c.getType().getName(), c.getStatus().getName(),
                    c.getSource() != null ? c.getSource().getName() : null,
                    c.getCreatedAt().toString()));
        }
        return csv.toString();
    }

    // ---------------------------------------------------------------- create

    @Transactional
    public CustomerDetailResponse create(CustomerRequest request) {
        enforceDuplicatePolicy(request, null);

        CustomerStatus status = request.statusId() != null
                ? findStatusOrThrow(request.statusId())
                : statusRepository.findByIsDefaultTrue()
                        .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                                "No default customer status is configured"));

        Customer customer = Customer.builder()
                .customerCode(nextCustomerCode())
                .displayName(request.displayName())
                .type(findTypeOrThrow(request.typeId()))
                .status(status)
                .build();
        applyScalarFields(customer, request);
        applySource(customer, request.sourceId());
        applyTags(customer, request.tagIds());
        customerRepository.save(customer);
        applyContacts(customer, request.contacts());
        applyAddresses(customer, request.addresses());

        events.record(customer.getId(), CustomerEventService.CREATED,
                "Customer created", Map.of("code", customer.getCustomerCode()));
        return CustomerDetailResponse.from(customer);
    }

    // ---------------------------------------------------------------- update

    @Transactional
    public CustomerDetailResponse update(Long id, CustomerRequest request) {
        Customer customer = findWithDetailsOrThrow(id);
        requireNotMerged(customer);
        if (request.expectedVersion() != null
                && !request.expectedVersion().equals(customer.getVersion())) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "The customer was modified by someone else; reload and retry");
        }
        enforceDuplicatePolicy(request, id);

        String oldStatus = customer.getStatus().getName();
        Map<String, Object> before = scalarSnapshot(customer);
        Set<String> oldContacts = contactSnapshot(customer);
        Set<String> oldAddresses = addressSnapshot(customer);
        Set<String> oldTags = tagSnapshot(customer);

        customer.setDisplayName(request.displayName());
        customer.setType(findTypeOrThrow(request.typeId()));
        if (request.statusId() != null
                && !request.statusId().equals(customer.getStatus().getId())) {
            CustomerStatus target = findStatusOrThrow(request.statusId());
            if (target.isArchivedState() || target.isDeletedState()) {
                throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "Use the archive/delete operations for lifecycle statuses");
            }
            customer.setStatus(target);
            events.record(id, CustomerEventService.STATUS_CHANGED, "Status changed",
                    Map.of("from", oldStatus, "to", target.getName()));
        }
        applyScalarFields(customer, request);
        applySource(customer, request.sourceId());
        applyTags(customer, request.tagIds());
        applyContacts(customer, request.contacts());
        applyAddresses(customer, request.addresses());

        // Contact/address/tag modification history, preserved as timeline facts.
        recordSetChange(id, CustomerEventService.CONTACT_CHANGED, "Contact information changed",
                oldContacts, contactSnapshot(customer));
        recordSetChange(id, CustomerEventService.ADDRESS_CHANGED, "Addresses changed",
                oldAddresses, addressSnapshot(customer));
        recordSetChange(id, CustomerEventService.TAGS_CHANGED, "Tags changed",
                oldTags, tagSnapshot(customer));

        // Audit: the UPDATED event carries old/new values of every changed field.
        Map<String, Object> after = scalarSnapshot(customer);
        Map<String, Object> oldValues = new LinkedHashMap<>();
        Map<String, Object> newValues = new LinkedHashMap<>();
        after.forEach((key, value) -> {
            if (!Objects.equals(before.get(key), value)) {
                oldValues.put(key, before.get(key));
                newValues.put(key, value);
            }
        });
        events.record(id, CustomerEventService.UPDATED, "Profile updated",
                oldValues.isEmpty() ? null : Map.of("old", oldValues, "new", newValues));

        return CustomerDetailResponse.from(customer);
    }

    // ------------------------------------------------------------- lifecycle

    @Transactional
    public CustomerResponse archive(Long id) {
        Customer customer = findOrThrow(id);
        requireNotMerged(customer);
        if (customer.getStatus().isArchivedState() || customer.getStatus().isDeletedState()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "The customer is already archived or deleted");
        }
        String from = customer.getStatus().getName();
        CustomerStatus archived = statusRepository.findByIsArchivedStateTrue()
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                        "No archived status is configured"));
        customer.setStatus(archived);
        customer.setArchivedAt(Instant.now());
        customer.setArchivedBy(CurrentActor.id());
        events.record(id, CustomerEventService.ARCHIVED, "Customer archived",
                Map.of("from", from));
        return CustomerResponse.from(customer);
    }

    @Transactional
    public CustomerResponse restore(Long id) {
        Customer customer = findOrThrow(id);
        requireNotMerged(customer);
        if (!customer.getStatus().isArchivedState() && !customer.getStatus().isDeletedState()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Only archived or deleted customers can be restored");
        }
        String from = customer.getStatus().getName();
        CustomerStatus target = statusRepository.findByIsDefaultTrue()
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                        "No default customer status is configured"));
        customer.setStatus(target);
        customer.setArchivedAt(null);
        customer.setArchivedBy(null);
        customer.setDeletedAt(null);
        customer.setDeletedBy(null);
        events.record(id, CustomerEventService.RESTORED, "Customer restored",
                Map.of("from", from));
        return CustomerResponse.from(customer);
    }

    @Transactional
    public CustomerResponse softDelete(Long id) {
        Customer customer = findOrThrow(id);
        requireNotMerged(customer);
        if (customer.getStatus().isDeletedState()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "The customer is already deleted");
        }
        String from = customer.getStatus().getName();
        CustomerStatus deleted = statusRepository.findByIsDeletedStateTrue()
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                        "No deleted status is configured"));
        customer.setStatus(deleted);
        customer.setDeletedAt(Instant.now());
        customer.setDeletedBy(CurrentActor.id());
        events.record(id, CustomerEventService.DELETED, "Customer deleted (soft)",
                Map.of("from", from));
        return CustomerResponse.from(customer);
    }

    /**
     * Physical removal. Two-step gated (must be soft-deleted first) and refused
     * for customers that were a merge target or source — those records anchor
     * history and references and must survive.
     */
    @Transactional
    public void purge(Long id) {
        Customer customer = findWithDetailsOrThrow(id);
        if (!customer.getStatus().isDeletedState()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Only soft-deleted customers can be permanently removed");
        }
        if (customer.getMergedInto() != null) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Merged customer records are kept for reference integrity");
        }
        if (customer.getAvatarKey() != null) {
            fileStorage.delete(customer.getAvatarKey());
        }
        customerRepository.delete(customer);
    }

    // ---------------------------------------------------------------- avatar

    @Transactional
    public void uploadAvatar(Long id, MultipartFile file) {
        byte[] content = ImageUploads.validated(file, storageProperties);
        Customer customer = findOrThrow(id);
        String oldKey = customer.getAvatarKey();
        customer.setAvatarKey(fileStorage.store("customer-avatars/" + id, content,
                file.getContentType()));
        if (oldKey != null) {
            fileStorage.delete(oldKey);
        }
        events.record(id, CustomerEventService.AVATAR_UPDATED, "Profile image updated", null);
    }

    @Transactional(readOnly = true)
    public Optional<FileStorage.StoredFile> loadAvatar(Long id) {
        return customerRepository.findById(id)
                .map(Customer::getAvatarKey)
                .flatMap(fileStorage::load);
    }

    // ------------------------------------------------------------- duplicates

    @Transactional(readOnly = true)
    public List<DuplicateMatchResponse> checkDuplicates(CustomerRequest candidate, Long excludeId) {
        return duplicateDetection.findMatches(candidate, excludeId);
    }

    // -------------------------------------------------------------- internals

    private void enforceDuplicatePolicy(CustomerRequest request, Long excludeId) {
        if (request.ignoreDuplicates()) {
            return;
        }
        List<DuplicateMatchResponse> matches = duplicateDetection.findMatches(request, excludeId);
        if (!matches.isEmpty()) {
            String summary = matches.stream()
                    .map(m -> "%s (%s)".formatted(m.customerCode(), String.join(", ", m.matchedOn())))
                    .collect(Collectors.joining("; "));
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "Possible duplicate customers: " + summary
                            + ". Confirm to save anyway or merge the existing record.");
        }
    }

    private void applyScalarFields(Customer c, CustomerRequest r) {
        c.setFirstName(blankToNull(r.firstName()));
        c.setLastName(blankToNull(r.lastName()));
        c.setNationalCode(blankToNull(r.nationalCode()));
        c.setPassportNumber(blankToNull(r.passportNumber()));
        c.setBirthDate(r.birthDate());
        c.setGender(blankToNull(r.gender()));
        c.setOccupation(blankToNull(r.occupation()));
        c.setCompanyName(blankToNull(r.companyName()));
        c.setTaxNumber(blankToNull(r.taxNumber()));
    }

    private void applySource(Customer customer, Long sourceId) {
        customer.setSource(sourceId == null ? null : sourceRepository.findById(sourceId)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer source", sourceId)));
    }

    private void applyTags(Customer customer, List<Long> tagIds) {
        if (tagIds == null) {
            return;
        }
        List<CustomerTag> tags = tagRepository.findAllById(tagIds);
        if (tags.size() != new HashSet<>(tagIds).size()) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "One or more tags do not exist");
        }
        customer.getTags().clear();
        customer.getTags().addAll(tags);
    }

    /** Full replacement; enforces at most one default per kind. */
    private void applyContacts(Customer customer, List<ContactDto> contacts) {
        if (contacts == null) {
            return;
        }
        requireSingleDefault(contacts.stream().filter(c -> c.kind() == Kind.PHONE)
                .filter(ContactDto::isDefault).count(), "phone");
        requireSingleDefault(contacts.stream().filter(c -> c.kind() == Kind.EMAIL)
                .filter(ContactDto::isDefault).count(), "email");

        customer.getContacts().clear();
        // Flush the deletions before inserting replacements: Hibernate orders
        // inserts before collection removals, which would otherwise trip the
        // partial unique index on the default flag when a default is kept.
        flushIfPersisted(customer);
        contacts.forEach(dto -> customer.getContacts().add(CustomerContact.builder()
                .customer(customer)
                .kind(dto.kind())
                .value(dto.value().trim())
                .label(blankToNull(dto.label()))
                .isDefault(dto.isDefault())
                .build()));
    }

    private void applyAddresses(Customer customer, List<AddressDto> addresses) {
        if (addresses == null) {
            return;
        }
        requireSingleDefault(addresses.stream().filter(AddressDto::isDefault).count(), "address");

        customer.getAddresses().clear();
        flushIfPersisted(customer);
        addresses.forEach(dto -> customer.getAddresses().add(CustomerAddress.builder()
                .customer(customer)
                .label(blankToNull(dto.label()))
                .line(dto.line().trim())
                .city(blankToNull(dto.city()))
                .province(blankToNull(dto.province()))
                .postalCode(blankToNull(dto.postalCode()))
                .isDefault(dto.isDefault())
                .build()));
    }

    /** Flushes pending child deletions for an already-persisted customer. */
    private void flushIfPersisted(Customer customer) {
        if (customer.getId() != null) {
            customerRepository.flush();
        }
    }

    private void requireSingleDefault(long count, String label) {
        if (count > 1) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Only one default " + label + " is allowed");
        }
    }

    private Specification<Customer> toSpecification(CustomerFilter f) {
        boolean explicitStatus = f.statusId() != null;
        boolean includeHidden = Boolean.TRUE.equals(f.includeHidden());
        return Specification.allOf(
                CustomerSpecifications.notMerged(),
                CustomerSpecifications.globalSearch(f.search()),
                CustomerSpecifications.hasContact(Kind.PHONE, f.phone()),
                CustomerSpecifications.hasContact(Kind.EMAIL, f.email()),
                CustomerSpecifications.nationalCodeEquals(f.nationalCode()),
                CustomerSpecifications.cityContains(f.city()),
                CustomerSpecifications.hasStatus(f.statusId()),
                CustomerSpecifications.hasType(f.typeId()),
                CustomerSpecifications.hasSource(f.sourceId()),
                CustomerSpecifications.hasAnyTag(f.tagIds()),
                CustomerSpecifications.hasEventOfType(f.eventType(), f.eventSinceDays()),
                CustomerSpecifications.noEventSince(f.noEventSinceDays()),
                explicitStatus || includeHidden ? null : CustomerSpecifications.visibleByDefault());
    }

    private String nextCustomerCode() {
        long number = customerRepository.nextCodeNumber();
        return crmProperties.customerCodePrefix()
                + String.format("%0" + crmProperties.customerCodePadding() + "d", number);
    }

    private void recordSetChange(Long id, String eventType, String title,
                                 Set<String> before, Set<String> after) {
        if (Objects.equals(before, after)) {
            return;
        }
        Set<String> added = new HashSet<>(after);
        added.removeAll(before);
        Set<String> removed = new HashSet<>(before);
        removed.removeAll(after);
        events.record(id, eventType, title,
                Map.of("added", List.copyOf(added), "removed", List.copyOf(removed)));
    }

    /** Flat snapshot of auditable scalar fields. */
    private Map<String, Object> scalarSnapshot(Customer c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("displayName", c.getDisplayName());
        map.put("firstName", c.getFirstName());
        map.put("lastName", c.getLastName());
        map.put("nationalCode", c.getNationalCode());
        map.put("passportNumber", c.getPassportNumber());
        map.put("birthDate", c.getBirthDate() != null ? c.getBirthDate().toString() : null);
        map.put("gender", c.getGender());
        map.put("occupation", c.getOccupation());
        map.put("companyName", c.getCompanyName());
        map.put("taxNumber", c.getTaxNumber());
        map.put("type", c.getType().getName());
        map.put("source", c.getSource() != null ? c.getSource().getName() : null);
        return map;
    }

    private Set<String> contactSnapshot(Customer c) {
        return c.getContacts().stream()
                .map(x -> x.getKind() + ":" + x.getValue() + (x.isDefault() ? "*" : ""))
                .collect(Collectors.toSet());
    }

    private Set<String> addressSnapshot(Customer c) {
        return c.getAddresses().stream()
                .map(a -> a.getLine() + "," + (a.getCity() == null ? "" : a.getCity())
                        + (a.isDefault() ? "*" : ""))
                .collect(Collectors.toSet());
    }

    private Set<String> tagSnapshot(Customer c) {
        return c.getTags().stream().map(CustomerTag::getName).collect(Collectors.toSet());
    }

    private String defaultContact(Customer c, Kind kind) {
        return c.getContacts().stream()
                .filter(x -> x.getKind() == kind)
                .sorted((a, b) -> Boolean.compare(b.isDefault(), a.isDefault()))
                .map(CustomerContact::getValue)
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

    private void requireNotMerged(Customer customer) {
        if (customer.getMergedInto() != null) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "This record was merged into " + customer.getMergedInto().getCustomerCode());
        }
    }

    private Customer findOrThrow(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", id));
    }

    private Customer findWithDetailsOrThrow(Long id) {
        return customerRepository.findWithDetailsById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", id));
    }

    private com.sami.app.crm.domain.CustomerType findTypeOrThrow(Long id) {
        return typeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer type", id));
    }

    private CustomerStatus findStatusOrThrow(Long id) {
        return statusRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer status", id));
    }
}
