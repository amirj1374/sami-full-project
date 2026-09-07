package com.sami.app.sales.delivery;

import com.sami.app.common.domain.BaseEntity;
import com.sami.app.sales.order.SalesOrder;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.*;

@Entity @Table(name="sales_deliveries") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Delivery extends BaseEntity {
    @Column(name="tenant_id",nullable=false) Long tenantId;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="order_id",nullable=false) SalesOrder order;
    @Column(name="order_id",insertable=false,updatable=false) Long orderId;
    @Column(name="company_id",nullable=false) Long companyId; @Column(name="branch_id",nullable=false) Long branchId;
    @Column(name="customer_id",nullable=false) Long customerId; @Column(name="contact_id",nullable=false) Long contactId;
    @Column(name="delivery_number",nullable=false) String deliveryNumber;
    @Enumerated(EnumType.STRING) @Column(nullable=false) DeliveryStatus status;
    @Column(name="confirmed_at") Instant confirmedAt; @Column(name="cancelled_at") Instant cancelledAt;
    @Column(name="created_by") Long createdBy; @Column(name="created_by_email") String createdByEmail;
    @Column(length=2000) String notes;
    @OneToMany(mappedBy="delivery",cascade=CascadeType.ALL,orphanRemoval=true) @Builder.Default List<DeliveryLine> lines=new ArrayList<>();
}
