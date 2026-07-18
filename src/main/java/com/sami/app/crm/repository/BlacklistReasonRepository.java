package com.sami.app.crm.repository;

import com.sami.app.crm.domain.BlacklistReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data-access for the configurable {@link BlacklistReason} lookup. */
public interface BlacklistReasonRepository extends JpaRepository<BlacklistReason, Long> {

    List<BlacklistReason> findAllByOrderByDisplayOrderAsc();

    boolean existsByCodeIgnoreCase(String code);
}
