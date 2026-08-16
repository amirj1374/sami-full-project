package com.sami.app.attendance.web;
import com.sami.app.attendance.dto.AttendanceDtos.*;
import com.sami.app.attendance.service.AttendanceService;
import com.sami.app.common.api.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
@RestController @RequestMapping("/api/v1/attendance") @RequiredArgsConstructor
public class AttendanceController { private final AttendanceService service;
 @GetMapping("/context") @PreAuthorize("@authz.has('attendance:manage-employees')") public ApiResponse<ContextResponse> context(){return ApiResponse.ok(service.context());}
 @GetMapping("/employees") @PreAuthorize("@authz.has('attendance:view')") public ApiResponse<PageResponse<EmployeeResponse>> employees(@RequestParam(required=false)String q,@RequestParam(required=false)String status,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return ApiResponse.ok(service.employees(q,status,page,size));}
 @PostMapping("/employees") @PreAuthorize("@authz.has('attendance:manage-employees')") public ApiResponse<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest r){return ApiResponse.ok(service.create(r));}
 @PutMapping("/employees/{id}") @PreAuthorize("@authz.has('attendance:manage-employees')") public ApiResponse<EmployeeResponse> update(@PathVariable Long id,@Valid @RequestBody EmployeeRequest r){return ApiResponse.ok(service.update(id,r));}
 @GetMapping("/records") @PreAuthorize("@authz.has('attendance:view')") public ApiResponse<PageResponse<RecordResponse>> records(@RequestParam(required=false)Long employeeId,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate from,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate to,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return ApiResponse.ok(service.records(employeeId,from,to,page,size));}
 @GetMapping("/report") @PreAuthorize("@authz.has('attendance:report')") public ApiResponse<PageResponse<RecordResponse>> report(@RequestParam(required=false)Long employeeId,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate from,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate to,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="100")int size){return ApiResponse.ok(service.records(employeeId,from,to,page,size));}
 @PostMapping("/clock-in") @PreAuthorize("@authz.has('attendance:clock')") public ApiResponse<RecordResponse> in(@Valid @RequestBody ClockRequest r){return ApiResponse.ok(service.clockIn(r));}
 @PostMapping("/clock-out") @PreAuthorize("@authz.has('attendance:clock')") public ApiResponse<RecordResponse> out(@Valid @RequestBody ClockRequest r){return ApiResponse.ok(service.clockOut(r));}
 @PutMapping("/records/{id}") @PreAuthorize("@authz.has('attendance:correct')") public ApiResponse<RecordResponse> correct(@PathVariable Long id,@Valid @RequestBody CorrectionRequest r){return ApiResponse.ok(service.correct(id,r));}
}
