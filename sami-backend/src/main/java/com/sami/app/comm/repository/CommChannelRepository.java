package com.sami.app.comm.repository;

import com.sami.app.comm.domain.CommChannel;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CommChannelRepository extends JpaRepository<CommChannel, Long> {

    // Every read that builds a response or makes a routing decision needs the
    // whole configuration star; eager graphs here prevent the V18 lazy-init
    // failure class and the N+1 the sweep would otherwise cause.

    @EntityGraph(attributePaths = {"channelType", "provider", "status", "deliveryPolicy"})
    Optional<CommChannel> findByCode(String code);

    @EntityGraph(attributePaths = {"channelType", "provider", "status", "deliveryPolicy"})
    Optional<CommChannel> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"channelType", "provider", "status", "deliveryPolicy"})
    List<CommChannel> findByChannelTypeIdOrderByPriorityDesc(Long channelTypeId);

    @EntityGraph(attributePaths = {"channelType", "provider", "status", "deliveryPolicy"})
    List<CommChannel> findAllByOrderByPriorityDesc();
}
