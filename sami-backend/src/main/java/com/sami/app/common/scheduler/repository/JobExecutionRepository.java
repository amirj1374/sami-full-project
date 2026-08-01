package com.sami.app.common.scheduler.repository;

import com.sami.app.common.scheduler.domain.JobExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JobExecutionRepository extends JpaRepository<JobExecution, Long> {

    Page<JobExecution> findAllByJobIdOrderByStartedAtDesc(Long jobId, Pageable pageable);

    Page<JobExecution> findAllByJobIdAndTenantIdOrderByStartedAtDesc(
            Long jobId, Long tenantId, Pageable pageable);

    List<JobExecution> findAllByStatusOrderByStartedAtDesc(String status);

    Page<JobExecution> findAllByOrderByStartedAtDesc(Pageable pageable);

    Page<JobExecution> findAllByTenantIdOrderByStartedAtDesc(Long tenantId, Pageable pageable);

    @Query(value = "SELECT nextval('job_execution_seq')", nativeQuery = true)
    Long nextExecutionSequence();
}
