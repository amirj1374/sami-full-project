package com.sami.app.attendance.repository;
import com.sami.app.attendance.domain.AttendanceRecord;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord,Long>{
 @Lock(LockModeType.PESSIMISTIC_WRITE) Optional<AttendanceRecord> findByTenantIdAndEmployeeIdAndClockOutIsNull(Long tenantId,Long employeeId);
 Optional<AttendanceRecord> findByIdAndTenantId(Long id,Long tenantId);
 @Query("select r from AttendanceRecord r join fetch r.employee e where r.tenantId=:tenant and (:employeeId is null or e.id=:employeeId) and (:from is null or r.workDate>=:from) and (:to is null or r.workDate<=:to)")
 Page<AttendanceRecord> search(@Param("tenant")Long tenant,@Param("employeeId")Long employeeId,@Param("from")LocalDate from,@Param("to")LocalDate to,Pageable pageable);
}
