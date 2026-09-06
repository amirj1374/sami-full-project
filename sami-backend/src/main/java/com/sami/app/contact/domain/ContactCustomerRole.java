package com.sami.app.contact.domain;
import com.sami.app.common.domain.BaseEntity; import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="contact_customer_roles") @Getter @Setter @NoArgsConstructor
public class ContactCustomerRole extends BaseEntity { @Column(name="tenant_id",nullable=false,updatable=false) private Long tenantId; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="contact_id",nullable=false) private Contact contact; @Column(name="customer_id",nullable=false) private Long customerId; }
