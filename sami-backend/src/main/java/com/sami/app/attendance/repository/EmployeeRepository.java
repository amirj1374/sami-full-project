package com.sami.app.attendance.repository;
import com.sami.app.attendance.domain.Employee;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface EmployeeRepository extends JpaRepository<Employee,Long>{
 Optional<Employee> findByIdAndTenantId(Long id,Long tenantId);
 boolean existsByTenantIdAndEmployeeCodeIgnoreCase(Long tenantId,String code);
 @Query("select e from Employee e where e.tenantId=:tenant and (:q is null or lower(concat(e.firstName,' ',e.lastName)) like lower(concat('%',:q,'%')) or lower(e.employeeCode) like lower(concat('%',:q,'%'))) and (:status is null or e.status=:status)")
 Page<Employee> search(@Param("tenant")Long tenant,@Param("q")String q,@Param("status")String status,Pageable pageable);
}
