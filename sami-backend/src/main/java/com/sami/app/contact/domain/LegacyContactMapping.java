package com.sami.app.contact.domain;
import com.sami.app.common.domain.BaseEntity; import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="legacy_contact_mappings") @Getter @Setter @NoArgsConstructor
public class LegacyContactMapping extends BaseEntity { @Column(name="tenant_id",nullable=false,updatable=false) private Long tenantId; @Column(name="legacy_type",nullable=false,length=16) private String legacyType; @Column(name="legacy_id",nullable=false) private Long legacyId; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="contact_id",nullable=false) private Contact contact; @Column(name="match_method",nullable=false,length=32) private String matchMethod; @Column(name="review_required",nullable=false) private boolean reviewRequired; }
