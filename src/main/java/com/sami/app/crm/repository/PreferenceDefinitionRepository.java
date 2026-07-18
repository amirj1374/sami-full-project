package com.sami.app.crm.repository;

import com.sami.app.crm.domain.PreferenceDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data-access for {@link PreferenceDefinition}. */
public interface PreferenceDefinitionRepository extends JpaRepository<PreferenceDefinition, Long> {

    List<PreferenceDefinition> findAllByOrderByDisplayOrderAsc();

    List<PreferenceDefinition> findByActiveTrueOrderByDisplayOrderAsc();

    boolean existsByPrefKeyIgnoreCase(String prefKey);
}
