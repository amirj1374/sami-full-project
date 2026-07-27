package com.sami.app.comm.repository;

import com.sami.app.comm.domain.CommConversation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;
import java.util.Optional;

public interface CommConversationRepository extends JpaRepository<CommConversation, Long>,
        JpaSpecificationExecutor<CommConversation> {

    @EntityGraph(attributePaths = {"channel", "channel.channelType", "status"})
    Optional<CommConversation> findByConversationNumber(String conversationNumber);

    @EntityGraph(attributePaths = {"channel", "channel.channelType", "status"})
    Optional<CommConversation> findWithDetailsById(Long id);

    // Specification queries do NOT inherit @EntityGraph from other methods —
    // the V18 lesson; declared explicitly.
    @Override
    @EntityGraph(attributePaths = {"channel", "channel.channelType", "status"})
    Page<CommConversation> findAll(Specification<CommConversation> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"channel", "channel.channelType", "status"})
    List<CommConversation> findByCustomerIdOrderByLastActivityAtDesc(Long customerId);

    @org.springframework.data.jpa.repository.Query(value = "SELECT nextval('comm_conversation_number_seq')", nativeQuery = true)
    Long nextConversationSequence();
}
