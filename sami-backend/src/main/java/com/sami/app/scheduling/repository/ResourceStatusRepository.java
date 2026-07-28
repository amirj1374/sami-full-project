package com.sami.app.scheduling.repository;

import com.sami.app.scheduling.domain.ResourceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ResourceStatusRepository extends JpaRepository<ResourceStatus, Long> {
    Optional<ResourceStatus> findByCode(String code);
    Optional<ResourceStatus> findFirstByIsDefaultTrue();
    List<ResourceStatus> findAllByOrderByDisplayOrderAsc();
}
