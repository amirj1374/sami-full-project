package com.sami.app.purchasing.repository;

import com.sami.app.purchasing.domain.PurCancelReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data-access for the configurable {@link PurCancelReason} lookup. */
public interface PurCancelReasonRepository extends JpaRepository<PurCancelReason, Long> {

    List<PurCancelReason> findAllByOrderByDisplayOrderAsc();

    boolean existsByCodeIgnoreCase(String code);
}
