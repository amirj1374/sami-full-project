package com.sami.app.attendance.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Table(name="employees") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Employee extends BaseEntity {
    @Column(name="tenant_id",nullable=false) private Long tenantId;
    @Column(name="employee_code",nullable=false,length=32) private String employeeCode;
    @Column(name="first_name",nullable=false,length=100) private String firstName;
    @Column(name="last_name",nullable=false,length=100) private String lastName;
    @Column(name="user_id") private Long userId;
    @Column(name="company_id",nullable=false) private Long companyId;
    @Column(name="branch_id",nullable=false) private Long branchId;
    @Column(name="job_title",length=120) private String jobTitle;
    @Column(length=32) private String mobile;
    @Column(name="hire_date") private LocalDate hireDate;
    @Column(nullable=false,length=16) private String status;
}
