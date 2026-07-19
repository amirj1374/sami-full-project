package com.sami.app.scheduling.repository;

import com.sami.app.scheduling.domain.AppointmentType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AppointmentTypeRepository extends JpaRepository<AppointmentType, Long> {
    @EntityGraph(attributePaths = {"category"})
    Optional<AppointmentType> findByCode(String code);

    @EntityGraph(attributePaths = {"category"})
    Optional<AppointmentType> findByIdAndIsActiveTrue(Long id);

    @EntityGraph(attributePaths = {"category"})
    List<AppointmentType> findByIsActiveTrueOrderByDisplayOrderAsc();

    List<AppointmentType> findByIsActiveTrueAndAllowsSelfServiceTrueOrderByDisplayOrderAsc();
}
