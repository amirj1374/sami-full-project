package com.sami.app.dataquality.repository;

import com.sami.app.dataquality.domain.ValidationRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ValidationRunRepository extends JpaRepository<ValidationRun, Long> {

    Page<ValidationRun> findByModuleCodeAndEntityCodeOrderByStartedAtDesc(
            String moduleCode, String entityCode, Pageable pageable);

    @Query(value = "SELECT nextval('validation_run_seq')", nativeQuery = true)
    long nextNumber();
}
