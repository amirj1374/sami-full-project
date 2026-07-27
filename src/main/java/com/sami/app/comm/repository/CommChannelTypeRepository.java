package com.sami.app.comm.repository;

import com.sami.app.comm.domain.CommChannelType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CommChannelTypeRepository extends JpaRepository<CommChannelType, Long> {
    Optional<CommChannelType> findByCode(String code);
    Optional<CommChannelType> findFirstByIsDefaultTrue();
    List<CommChannelType> findByIsActiveTrueOrderByDisplayOrderAsc();
}
