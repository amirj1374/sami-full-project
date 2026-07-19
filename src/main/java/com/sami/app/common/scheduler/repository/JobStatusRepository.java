package com.sami.app.common.scheduler.repository;

import com.sami.app.common.scheduler.domain.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobStatusRepository extends JpaRepository<JobStatus, Long> {

    Optional<JobStatus> findByCode(String code);

    Optional<JobStatus> findFirstByIsDefaultTrue();

    Optional<JobStatus> findFirstByIsPausedStateTrue();

    Optional<JobStatus> findFirstByIsFailedStateTrue();

    List<JobStatus> findAllByOrderByDisplayOrderAsc();
}
