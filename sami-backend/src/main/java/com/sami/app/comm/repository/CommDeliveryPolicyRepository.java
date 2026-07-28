package com.sami.app.comm.repository;

import com.sami.app.comm.domain.CommDeliveryPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CommDeliveryPolicyRepository extends JpaRepository<CommDeliveryPolicy, Long> {
    Optional<CommDeliveryPolicy> findByCode(String code);
    Optional<CommDeliveryPolicy> findFirstByIsDefaultTrue();
    List<CommDeliveryPolicy> findAllByOrderByDisplayOrderAsc();
}
