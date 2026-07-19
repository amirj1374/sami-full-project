package com.sami.app.licensing.repository;

import com.sami.app.licensing.domain.BillingCycle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillingCycleRepository extends JpaRepository<BillingCycle, Long> {

    List<BillingCycle> findAllByOrderByDisplayOrderAsc();

    Optional<BillingCycle> findByCode(String code);

    Optional<BillingCycle> findByIsDefaultTrue();
}
