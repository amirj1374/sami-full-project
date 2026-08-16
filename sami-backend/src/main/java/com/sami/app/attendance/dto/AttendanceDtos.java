package com.sami.app.attendance.dto;
import com.sami.app.attendance.domain.*;
import jakarta.validation.constraints.*;
import java.time.*;
public final class AttendanceDtos { private AttendanceDtos(){}
 public record EmployeeRequest(@NotBlank @Size(max=32)String employeeCode,@NotBlank @Size(max=100)String firstName,@NotBlank @Size(max=100)String lastName,Long userId,@NotNull Long companyId,@NotNull Long branchId,@Size(max=120)String jobTitle,@Size(max=32)String mobile,LocalDate hireDate,@Pattern(regexp="ACTIVE|INACTIVE")String status){}
 public record EmployeeResponse(Long id,String employeeCode,String firstName,String lastName,String fullName,Long userId,Long companyId,Long branchId,String jobTitle,String mobile,LocalDate hireDate,String status,Long version){public static EmployeeResponse from(Employee e){return new EmployeeResponse(e.getId(),e.getEmployeeCode(),e.getFirstName(),e.getLastName(),e.getFirstName()+" "+e.getLastName(),e.getUserId(),e.getCompanyId(),e.getBranchId(),e.getJobTitle(),e.getMobile(),e.getHireDate(),e.getStatus(),e.getVersion());}}
 public record ClockRequest(@NotNull Long employeeId,Instant occurredAt,@Size(max=500)String notes){}
 public record CorrectionRequest(@NotNull Instant clockIn,Instant clockOut,@Pattern(regexp="PRESENT|LATE|ABSENT|LEAVE|MISSION")String status,@Size(max=500)String notes,@NotNull Long expectedVersion){}
 public record RecordResponse(Long id,Long employeeId,String employeeCode,String employeeName,LocalDate workDate,Instant clockIn,Instant clockOut,String status,String source,String notes,Long version){public static RecordResponse from(AttendanceRecord r){var e=r.getEmployee();return new RecordResponse(r.getId(),e.getId(),e.getEmployeeCode(),e.getFirstName()+" "+e.getLastName(),r.getWorkDate(),r.getClockIn(),r.getClockOut(),r.getStatus(),r.getSource(),r.getNotes(),r.getVersion());}}
 public record OptionResponse(Long id,String label,Long parentId){}
 public record ContextResponse(java.util.List<OptionResponse> companies,java.util.List<OptionResponse> branches,java.util.List<OptionResponse> users){}
}
