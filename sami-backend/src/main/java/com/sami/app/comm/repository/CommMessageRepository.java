package com.sami.app.comm.repository;

import com.sami.app.comm.domain.CommMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CommMessageRepository extends JpaRepository<CommMessage, Long>,
        JpaSpecificationExecutor<CommMessage> {

    @EntityGraph(attributePaths = {"channel", "channel.channelType", "channel.provider",
            "channel.status", "channel.deliveryPolicy", "status", "template"})
    Optional<CommMessage> findByMessageNumber(String messageNumber);

    @EntityGraph(attributePaths = {"channel", "channel.channelType", "channel.provider",
            "channel.status", "channel.deliveryPolicy", "status", "template"})
    Optional<CommMessage> findWithDetailsById(Long id);

    Optional<CommMessage> findByTenantIdAndIdempotencyKey(Long tenantId, String idempotencyKey);

    @Override
    @EntityGraph(attributePaths = {"channel", "channel.channelType", "status"})
    Page<CommMessage> findAll(Specification<CommMessage> spec, Pageable pageable);

    /**
     * The sweep's working set: retryable messages whose next attempt is due.
     * Status flags — not codes — decide retryability.
     */
    @EntityGraph(attributePaths = {"channel", "channel.channelType", "channel.provider",
            "channel.status", "channel.deliveryPolicy", "status"})
    @Query("""
            SELECT m FROM CommMessage m
            WHERE m.status.allowsRetry = TRUE
              AND m.nextAttemptAt IS NOT NULL
              AND m.nextAttemptAt <= :now
            ORDER BY m.priority DESC, m.nextAttemptAt ASC
            """)
    List<CommMessage> findDue(@Param("now") Instant now, Pageable pageable);

    @EntityGraph(attributePaths = {"channel", "channel.channelType", "status"})
    List<CommMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    /** Success-rate and volume aggregates for the reports endpoint. */
    @Query("""
            SELECT m.channel.id, count(m),
                   sum(CASE WHEN m.status.countsAsSuccess = TRUE THEN 1 ELSE 0 END),
                   sum(CASE WHEN m.status.isFailedState = TRUE THEN 1 ELSE 0 END)
            FROM CommMessage m
            WHERE m.createdAt >= :from AND m.createdAt < :to
            GROUP BY m.channel.id
            """)
    List<Object[]> deliveryStats(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = "SELECT nextval('comm_message_number_seq')", nativeQuery = true)
    Long nextMessageSequence();
}
