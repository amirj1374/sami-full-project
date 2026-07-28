package com.sami.app.comm.repository;

import com.sami.app.comm.domain.CommRoutingRule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommRoutingRuleRepository extends JpaRepository<CommRoutingRule, Long> {

    @EntityGraph(attributePaths = {
            "preferredChannel", "preferredChannel.channelType", "preferredChannel.provider",
            "preferredChannel.status", "preferredChannel.deliveryPolicy",
            "fallbackChannel", "fallbackChannel.channelType", "fallbackChannel.provider",
            "fallbackChannel.status", "fallbackChannel.deliveryPolicy"})
    List<CommRoutingRule> findByIsActiveTrueOrderByPriorityDesc();

    @EntityGraph(attributePaths = {"preferredChannel", "fallbackChannel"})
    List<CommRoutingRule> findAllByOrderByPriorityDesc();
}
