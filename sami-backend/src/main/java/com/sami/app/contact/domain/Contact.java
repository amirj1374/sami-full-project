package com.sami.app.contact.domain;
import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="contacts") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Contact extends BaseEntity {
 @Column(name="tenant_id",nullable=false,updatable=false) private Long tenantId;
 @Column(name="identity_type",nullable=false,length=16) private String identityType;
 @Column(name="display_name",nullable=false,length=160) private String displayName;
 @Column(name="first_name",length=80) private String firstName;
 @Column(name="last_name",length=80) private String lastName;
 @Column(name="company_name",length=160) private String companyName;
 @Column(name="national_code",length=32) private String nationalCode;
 @Column(name="legal_identifier",length=64) private String legalIdentifier;
 @Column(name="tax_number",length=64) private String taxNumber;
 @Column(length=255) private String email;
 @Column(length=64) private String phone;
 @Column(name="is_active",nullable=false) private boolean active;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="merged_into_id") private Contact mergedInto;
}
