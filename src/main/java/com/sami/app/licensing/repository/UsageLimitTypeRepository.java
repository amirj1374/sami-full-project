package com.sami.app.licensing.repository;

import com.sami.app.licensing.domain.UsageLimitType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsageLimitTypeRepository extends JpaRepository<UsageLimitType, Long> {

    List<UsageLimitType> findAllByOrderByDisplayOrderAsc();

    Optional<UsageLimitType> findByCode(String code);
}
