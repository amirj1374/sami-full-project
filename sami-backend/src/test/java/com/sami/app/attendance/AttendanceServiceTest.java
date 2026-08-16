package com.sami.app.attendance;

import com.sami.app.attendance.domain.AttendanceRecord;
import com.sami.app.attendance.domain.Employee;
import com.sami.app.attendance.dto.AttendanceDtos.ClockRequest;
import com.sami.app.attendance.repository.AttendanceRecordRepository;
import com.sami.app.attendance.repository.EmployeeRepository;
import com.sami.app.attendance.service.AttendanceService;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.tenancy.TenantContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock EmployeeRepository employees;
    @Mock AttendanceRecordRepository records;
    @Mock TenantContext tenantContext;
    @Mock JdbcTemplate jdbc;
    @InjectMocks AttendanceService service;

    @Test
    void clockInUsesTrustedTenantAndPersistsOneOpenRecord() {
        Employee employee = employee(41L, "ACTIVE");
        when(tenantContext.requireTenantId()).thenReturn(41L);
        when(employees.findByIdAndTenantId(7L, 41L)).thenReturn(Optional.of(employee));
        when(records.findByTenantIdAndEmployeeIdAndClockOutIsNull(41L, 7L))
                .thenReturn(Optional.empty());
        when(jdbc.queryForObject(any(String.class), eq(String.class), eq(3L), eq(41L)))
                .thenReturn("Asia/Tehran");
        when(records.save(any(AttendanceRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.clockIn(new ClockRequest(7L, Instant.parse("2026-08-16T05:00:00Z"), null));

        verify(records).save(any(AttendanceRecord.class));
        verify(employees).findByIdAndTenantId(7L, 41L);
    }

    @Test
    void inactiveEmployeeCannotClockIn() {
        when(tenantContext.requireTenantId()).thenReturn(41L);
        when(employees.findByIdAndTenantId(7L, 41L))
                .thenReturn(Optional.of(employee(41L, "INACTIVE")));

        assertThatThrownBy(() -> service.clockIn(new ClockRequest(7L, null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Inactive employee");

        verifyNoInteractions(records);
    }

    @Test
    void duplicateOpenAttendanceIsRejectedBeforeInsert() {
        Employee employee = employee(41L, "ACTIVE");
        when(tenantContext.requireTenantId()).thenReturn(41L);
        when(employees.findByIdAndTenantId(7L, 41L)).thenReturn(Optional.of(employee));
        when(records.findByTenantIdAndEmployeeIdAndClockOutIsNull(41L, 7L))
                .thenReturn(Optional.of(AttendanceRecord.builder().employee(employee).build()));

        assertThatThrownBy(() -> service.clockIn(new ClockRequest(7L, null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("open attendance record");

        verify(records, never()).save(any());
    }

    @Test
    void tenantBoundaryIsAppliedToEmployeeLookup() {
        when(tenantContext.requireTenantId()).thenReturn(52L);
        when(employees.findByIdAndTenantId(7L, 52L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.clockIn(new ClockRequest(7L, null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Employee not found");

        verify(employees).findByIdAndTenantId(7L, 52L);
        verify(records, never()).findByTenantIdAndEmployeeIdAndClockOutIsNull(anyLong(), anyLong());
    }

    @Test
    void clockOutCannotPrecedeClockIn() {
        Employee employee = employee(41L, "ACTIVE");
        AttendanceRecord record = AttendanceRecord.builder()
                .tenantId(41L)
                .employee(employee)
                .clockIn(Instant.parse("2026-08-16T05:00:00Z"))
                .build();
        when(tenantContext.requireTenantId()).thenReturn(41L);
        when(employees.findByIdAndTenantId(7L, 41L)).thenReturn(Optional.of(employee));
        when(records.findByTenantIdAndEmployeeIdAndClockOutIsNull(41L, 7L))
                .thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.clockOut(new ClockRequest(
                7L, Instant.parse("2026-08-16T04:59:59Z"), null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("before clock-in");

        verify(records, never()).save(any());
    }

    private Employee employee(Long tenantId, String status) {
        Employee employee = Employee.builder()
                .tenantId(tenantId)
                .employeeCode("EMP-7")
                .firstName("Ali")
                .lastName("Ahmadi")
                .companyId(2L)
                .branchId(3L)
                .status(status)
                .build();
        ReflectionTestUtils.setField(employee, "id", 7L);
        return employee;
    }
}
