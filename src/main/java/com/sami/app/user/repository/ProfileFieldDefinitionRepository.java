package com.sami.app.user.repository;

import com.sami.app.user.domain.ProfileFieldDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data-access for {@link ProfileFieldDefinition}. */
public interface ProfileFieldDefinitionRepository extends JpaRepository<ProfileFieldDefinition, Long> {

    List<ProfileFieldDefinition> findAllByOrderByDisplayOrderAsc();

    List<ProfileFieldDefinition> findByActiveTrueOrderByDisplayOrderAsc();

    boolean existsByFieldKeyIgnoreCase(String fieldKey);
}
