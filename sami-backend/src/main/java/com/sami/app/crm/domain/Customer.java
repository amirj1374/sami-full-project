package com.sami.app.crm.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The single source of truth for one customer.
 *
 * <p>Contacts, addresses, tags, notes and timeline events hang off this record.
 * A customer absorbed by a merge keeps its row forever with
 * {@code mergedInto} set — every reference from any module stays resolvable —
 * but disappears from listings and duplicate checks.
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(name = "customer_code", nullable = false, length = 32)
    private String customerCode;

    @Column(name = "first_name", length = 80)
    private String firstName;

    @Column(name = "last_name", length = 80)
    private String lastName;

    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @Column(name = "national_code", length = 32)
    private String nationalCode;

    @Column(name = "passport_number", length = 32)
    private String passportNumber;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(length = 16)
    private String gender;

    @Column(length = 120)
    private String occupation;

    @Column(name = "company_name", length = 160)
    private String companyName;

    @Column(name = "tax_number", length = 64)
    private String taxNumber;

    /** Opaque key into the {@code FileStorage} port. */
    @Column(name = "avatar_key", length = 255)
    private String avatarKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "type_id", nullable = false)
    private CustomerType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private CustomerStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private CustomerSource source;

    /** Set when this record was absorbed by a merge; the survivor. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merged_into_id")
    private Customer mergedInto;

    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("isDefault DESC, id ASC")
    @Builder.Default
    private List<CustomerContact> contacts = new ArrayList<>();

    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("isDefault DESC, id ASC")
    @Builder.Default
    private List<CustomerAddress> addresses = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "customer_tag_links",
            joinColumns = @JoinColumn(name = "customer_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @Builder.Default
    private Set<CustomerTag> tags = new HashSet<>();

    /** Configurable preference values, keyed by {@code PreferenceDefinition.prefKey}. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferences", nullable = false)
    @Builder.Default
    private Map<String, Object> preferences = new HashMap<>();

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "archived_by")
    private Long archivedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private Long deletedBy;
}
