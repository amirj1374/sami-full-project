package com.sami.app.comm.repository;

import com.sami.app.comm.domain.CommChannelStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CommChannelStatusRepository extends JpaRepository<CommChannelStatus, Long> {
    Optional<CommChannelStatus> findByCode(String code);
    Optional<CommChannelStatus> findFirstByIsDefaultTrue();
    List<CommChannelStatus> findAllByOrderByDisplayOrderAsc();
}
