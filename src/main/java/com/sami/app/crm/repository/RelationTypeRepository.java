package com.sami.app.crm.repository;

import com.sami.app.crm.domain.RelationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data-access for the configurable {@link RelationType} lookup. */
public interface RelationTypeRepository extends JpaRepository<RelationType, Long> {

    List<RelationType> findAllByOrderByDisplayOrderAsc();

    boolean existsByCodeIgnoreCase(String code);
}
