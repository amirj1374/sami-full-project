package com.sami.app.supplier.dto;

import com.sami.app.supplier.domain.SupBankAccount;
import com.sami.app.supplier.domain.SupChannel;
import com.sami.app.supplier.domain.SupAddress;
import com.sami.app.supplier.domain.SupContact;
import com.sami.app.supplier.domain.SupDocument;
import com.sami.app.supplier.domain.SupLog;
import com.sami.app.supplier.domain.SupRating;
import com.sami.app.supplier.domain.Supplier;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Request/response payloads for suppliers. */
public final class SupplierDtos {

    private SupplierDtos() {
    }

    // ------------------------------------------------------------- responses

    public record ChannelDto(
            Long id,
            @NotNull SupChannel.Kind kind,
            @NotBlank @Size(max = 255) String value,
            @Size(max = 60) String label,
            boolean isDefault
    ) {
        public static ChannelDto from(SupChannel c) {
            return new ChannelDto(c.getId(), c.getKind(), c.getValue(), c.getLabel(), c.isDefault());
        }
    }

    public record AddressDto(
            Long id,
            @Size(max = 60) String label,
            @NotBlank @Size(max = 300) String line,
            @Size(max = 100) String city,
            @Size(max = 100) String province,
            @Size(max = 20) String postalCode,
            boolean isDefault
    ) {
        public static AddressDto from(SupAddress a) {
            return new AddressDto(a.getId(), a.getLabel(), a.getLine(), a.getCity(),
                    a.getProvince(), a.getPostalCode(), a.isDefault());
        }
    }

    public record ContactDto(
            Long id,
            @NotBlank @Size(max = 120) String fullName,
            @Size(max = 100) String position,
            @Size(max = 100) String department,
            @Size(max = 32) String phone,
            @Size(max = 32) String mobile,
            @Size(max = 255) String email,
            @Size(max = 32) String preferredMethod,
            @Size(max = 500) String notes,
            boolean isPrimary
    ) {
        public static ContactDto from(SupContact c) {
            return new ContactDto(c.getId(), c.getFullName(), c.getPosition(), c.getDepartment(),
                    c.getPhone(), c.getMobile(), c.getEmail(), c.getPreferredMethod(),
                    c.getNotes(), c.isPrimary());
        }
    }

    public record BankAccountDto(
            Long id,
            @NotBlank @Size(max = 100) String bankName,
            @Size(max = 64) String accountNumber,
            @Size(max = 34) String iban,
            @Size(max = 20) String cardNumber,
            @Size(max = 120) String accountHolder,
            boolean isDefault,
            Map<String, Object> extra
    ) {
        public static BankAccountDto from(SupBankAccount b) {
            return new BankAccountDto(b.getId(), b.getBankName(), b.getAccountNumber(),
                    b.getIban(), b.getCardNumber(), b.getAccountHolder(), b.isDefault(),
                    b.getExtra());
        }
    }

    public record SupplierRowResponse(
            Long id, String supplierCode, String companyName, String displayName,
            String city, SupLookupDtos.TypeResponse type, SupLookupDtos.StatusResponse status,
            SupLookupDtos.PaymentTermResponse paymentTerm,
            List<SupLookupDtos.TagResponse> tags,
            List<SupLookupDtos.CategoryResponse> categories,
            BigDecimal ratingAvg, BigDecimal creditLimit,
            Instant createdAt, Instant updatedAt, Long version
    ) {
        public static SupplierRowResponse from(Supplier s) {
            return new SupplierRowResponse(
                    s.getId(), s.getSupplierCode(), s.getCompanyName(), s.getDisplayName(),
                    s.getCity(),
                    SupLookupDtos.TypeResponse.from(s.getType()),
                    SupLookupDtos.StatusResponse.from(s.getStatus()),
                    s.getPaymentTerm() != null
                            ? SupLookupDtos.PaymentTermResponse.from(s.getPaymentTerm()) : null,
                    s.getTags().stream()
                            .sorted(Comparator.comparing(t -> t.getName().toLowerCase()))
                            .map(SupLookupDtos.TagResponse::from).toList(),
                    s.getCategories().stream()
                            .sorted(Comparator.comparing(c -> c.getName().toLowerCase()))
                            .map(SupLookupDtos.CategoryResponse::from).toList(),
                    s.getRatingAvg(), s.getCreditLimit(),
                    s.getCreatedAt(), s.getUpdatedAt(), s.getVersion());
        }
    }

    public record SupplierDetailResponse(
            SupplierRowResponse supplier,
            String legalName, String nationalId, String economicCode, String taxNumber,
            String registrationNumber, String ownerName, String website,
            String country, String province, String postalCode, String description,
            List<ChannelDto> channels,
            List<AddressDto> addresses,
            List<ContactDto> contacts,
            List<BankAccountDto> bankAccounts
    ) {
        public static SupplierDetailResponse from(Supplier s) {
            return new SupplierDetailResponse(
                    SupplierRowResponse.from(s),
                    s.getLegalName(), s.getNationalId(), s.getEconomicCode(), s.getTaxNumber(),
                    s.getRegistrationNumber(), s.getOwnerName(), s.getWebsite(),
                    s.getCountry(), s.getProvince(), s.getPostalCode(), s.getDescription(),
                    s.getChannels().stream().map(ChannelDto::from).toList(),
                    s.getAddresses().stream().map(AddressDto::from).toList(),
                    s.getContacts().stream().map(ContactDto::from).toList(),
                    s.getBankAccounts().stream().map(BankAccountDto::from).toList());
        }
    }

    public record RatingResponse(Long criterionId, String criterionName, BigDecimal weight,
                                 BigDecimal score, String note, String ratedByEmail,
                                 Instant updatedAt) {
        public static RatingResponse from(SupRating r) {
            return new RatingResponse(r.getCriterion().getId(), r.getCriterion().getName(),
                    r.getCriterion().getWeight(), r.getScore(), r.getNote(),
                    r.getRatedByEmail(), r.getUpdatedAt());
        }
    }

    public record DocumentResponse(Long id, String docType, String fileName, String contentType,
                                   long fileSize, String uploadedByEmail, Instant createdAt) {
        public static DocumentResponse from(SupDocument d) {
            return new DocumentResponse(d.getId(),
                    d.getDocType() != null ? d.getDocType().getName() : null,
                    d.getFileName(), d.getContentType(), d.getFileSize(),
                    d.getUploadedByEmail(), d.getCreatedAt());
        }
    }

    public record LogResponse(Long id, String action, String title, Map<String, Object> detail,
                              String actorEmail, Instant occurredAt) {
        public static LogResponse from(SupLog l) {
            return new LogResponse(l.getId(), l.getAction(), l.getTitle(), l.getDetail(),
                    l.getActorEmail(), l.getOccurredAt());
        }
    }

    // -------------------------------------------------------------- requests

    /** Create/update payload; child lists are full replacements. */
    public record SupplierRequest(
            @NotBlank @Size(max = 160) String companyName,
            @NotBlank @Size(max = 160) String displayName,
            @Size(max = 160) String legalName,
            @Size(max = 32) String nationalId,
            @Size(max = 32) String economicCode,
            @Size(max = 64) String taxNumber,
            @Size(max = 64) String registrationNumber,
            @Size(max = 120) String ownerName,
            @Size(max = 255) String website,
            @Size(max = 100) String country,
            @Size(max = 100) String province,
            @Size(max = 100) String city,
            @Size(max = 20) String postalCode,
            @Size(max = 2000) String description,
            @NotNull Long typeId,
            Long statusId,
            Long paymentTermId,
            @DecimalMin(value = "0.00") BigDecimal creditLimit,
            List<Long> tagIds,
            List<Long> categoryIds,
            @Valid List<ChannelDto> channels,
            @Valid List<AddressDto> addresses,
            @Valid List<ContactDto> contacts,
            @Valid List<BankAccountDto> bankAccounts,
            boolean ignoreDuplicates,
            Long expectedVersion
    ) {
    }

    public record RateRequest(
            @NotEmpty @Valid List<Score> scores
    ) {
        public record Score(
                @NotNull Long criterionId,
                @NotNull @DecimalMin(value = "0.0") @DecimalMax(value = "5.0") BigDecimal score,
                @Size(max = 255) String note
        ) {
        }
    }

    /** Combinable listing filters. */
    public record SupplierFilter(
            String search,
            String phone,
            String email,
            String contactName,
            String city,
            Long statusId,
            Long typeId,
            List<Long> tagIds,
            List<Long> categoryIds,
            BigDecimal minRating,
            Boolean includeHidden
    ) {
    }
}
