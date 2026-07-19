package com.sami.app.scheduling.repository;

import com.sami.app.scheduling.domain.AppointmentTypeReminder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AppointmentTypeReminderRepository extends JpaRepository<AppointmentTypeReminder, Long> {
    @EntityGraph(attributePaths = {"channel"})
    List<AppointmentTypeReminder> findByAppointmentTypeIdAndIsActiveTrue(Long appointmentTypeId);
}
