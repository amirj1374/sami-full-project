package com.sami.app.comm.repository;

import com.sami.app.comm.domain.CommMessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CommMessageStatusRepository extends JpaRepository<CommMessageStatus, Long> {
    Optional<CommMessageStatus> findByCode(String code);
    Optional<CommMessageStatus> findFirstByIsDefaultTrue();
    Optional<CommMessageStatus> findFirstByIsSendingStateTrue();
    Optional<CommMessageStatus> findFirstByIsSentStateTrue();
    Optional<CommMessageStatus> findFirstByIsDeliveredStateTrue();
    Optional<CommMessageStatus> findFirstByIsReadStateTrue();
    Optional<CommMessageStatus> findFirstByIsFailedStateTrueAndAllowsRetryTrue();
    Optional<CommMessageStatus> findFirstByIsFailedStateTrueAndIsTerminalTrue();
    Optional<CommMessageStatus> findFirstByIsExpiredStateTrue();
    Optional<CommMessageStatus> findFirstByIsCancelledStateTrue();
    List<CommMessageStatus> findAllByOrderByDisplayOrderAsc();
}
