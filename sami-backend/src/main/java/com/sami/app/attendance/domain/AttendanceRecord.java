package com.sami.app.attendance.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Entity @Table(name="attendance_records") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AttendanceRecord extends BaseEntity {
    @Column(name="tenant_id",nullable=false) private Long tenantId;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="employee_id",nullable=false) private Employee employee;
    @Column(name="work_date",nullable=false) private LocalDate workDate;
    @Column(name="clock_in",nullable=false) private Instant clockIn;
    @Column(name="clock_out") private Instant clockOut;
    @Column(nullable=false,length=20) private String status;
    @Column(nullable=false,length=16) private String source;
    @Column(length=500) private String notes;
    @Column(name="created_by") private Long createdBy;
    @Column(name="updated_by") private Long updatedBy;
}
