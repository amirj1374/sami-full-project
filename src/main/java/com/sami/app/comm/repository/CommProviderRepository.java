package com.sami.app.comm.repository;

import com.sami.app.comm.domain.CommProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CommProviderRepository extends JpaRepository<CommProvider, Long> {
    Optional<CommProvider> findByCode(String code);
    List<CommProvider> findAllByOrderByDisplayOrderAsc();
    List<CommProvider> findByIsActiveTrueOrderByDisplayOrderAsc();
}
