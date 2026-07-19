package com.sami.app.scheduling.repository;

import com.sami.app.scheduling.domain.ReminderChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReminderChannelRepository extends JpaRepository<ReminderChannel, Long> {
    Optional<ReminderChannel> findByCode(String code);
    List<ReminderChannel> findByIsActiveTrueOrderByDisplayOrderAsc();
    List<ReminderChannel> findAllByOrderByDisplayOrderAsc();
}
