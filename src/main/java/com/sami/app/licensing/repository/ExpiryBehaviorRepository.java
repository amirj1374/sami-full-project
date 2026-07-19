package com.sami.app.licensing.repository;

import com.sami.app.licensing.domain.ExpiryBehavior;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExpiryBehaviorRepository extends JpaRepository<ExpiryBehavior, Long> {

    List<ExpiryBehavior> findAllByOrderByDisplayOrderAsc();

    Optional<ExpiryBehavior> findByCode(String code);

    Optional<ExpiryBehavior> findByIsDefaultTrue();
}
