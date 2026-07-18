package com.sami.app.user.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * The user's profile: standard fields as real columns (constrainable and
 * indexable — phone, employee code and national code carry partial unique
 * indexes), plus a JSONB {@code customFields} map for runtime-defined fields
 * described by {@link ProfileFieldDefinition} — future fields never require a
 * schema change.
 *
 * <p>{@code employeeCode} doubles as the loose link to a future Employees
 * module: it can join on the code (or add its own FK later) without this module
 * knowing anything about it.
 */
@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "first_name", length = 80)
    private String firstName;

    @Column(name = "last_name", length = 80)
    private String lastName;

    @Column(name = "display_name", length = 160)
    private String displayName;

    @Column(name = "national_code", length = 32)
    private String nationalCode;

    @Column(name = "employee_code", length = 32)
    private String employeeCode;

    @Column(name = "phone_number", length = 32)
    private String phoneNumber;

    @Column(length = 16)
    private String gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    /** Opaque key into the {@code FileStorage} port; never a filesystem path. */
    @Column(name = "avatar_key", length = 255)
    private String avatarKey;

    @Column(length = 500)
    private String address;

    @Column(length = 2000)
    private String notes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_fields", nullable = false)
    @Builder.Default
    private Map<String, Object> customFields = new HashMap<>();
}
