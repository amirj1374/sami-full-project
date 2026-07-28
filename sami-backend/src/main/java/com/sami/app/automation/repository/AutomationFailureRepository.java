package com.sami.app.automation.repository;

import com.sami.app.automation.domain.AutomationFailure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AutomationFailureRepository extends JpaRepository<AutomationFailure, Long> {

    /** Open failures whose retry is due — drained by the retry scheduler (future phase). */
    List<AutomationFailure> findByResolvedFalseAndNextRetryAtBefore(Instant now);
}
