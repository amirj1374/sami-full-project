package com.sami.app.comm.repository;

import com.sami.app.comm.domain.CommConversationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CommConversationStatusRepository extends JpaRepository<CommConversationStatus, Long> {
    Optional<CommConversationStatus> findByCode(String code);
    Optional<CommConversationStatus> findFirstByIsDefaultTrue();
    Optional<CommConversationStatus> findFirstByIsTerminalTrue();
    List<CommConversationStatus> findAllByOrderByDisplayOrderAsc();
}
