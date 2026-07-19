package com.sami.app.knowledge.repository;

import com.sami.app.knowledge.domain.KbStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KbStatusRepository extends JpaRepository<KbStatus, Long> {
    Optional<KbStatus> findByCode(String code);
    Optional<KbStatus> findFirstByIsDefaultTrue();
    Optional<KbStatus> findFirstByIsPublishedStateTrue();
    Optional<KbStatus> findFirstByIsReviewStateTrue();
    Optional<KbStatus> findFirstByIsApprovedStateTrue();
    Optional<KbStatus> findFirstByIsArchivedStateTrue();
    Optional<KbStatus> findFirstByIsDeprecatedStateTrue();
    List<KbStatus> findAllByOrderByDisplayOrderAsc();
}
