package com.sami.app.comm.repository;

import com.sami.app.comm.domain.CommConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommConversationParticipantRepository extends JpaRepository<CommConversationParticipant, Long> {
    List<CommConversationParticipant> findByConversationId(Long conversationId);
}
