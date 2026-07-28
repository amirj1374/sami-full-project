package com.sami.app.metadata.repository;

import com.sami.app.metadata.domain.MetaField;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MetaFieldRepository extends JpaRepository<MetaField, Long> {

    boolean existsByEntityIdAndCode(Long entityId, String code);

    @EntityGraph(attributePaths = {"entity", "fieldType"})
    Optional<MetaField> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"entity", "fieldType"})
    List<MetaField> findAllBy();

    /** Active fields for one extensible entity, in display order. */
    @EntityGraph(attributePaths = {"entity", "fieldType"})
    @Query("SELECT f FROM MetaField f JOIN f.entity e "
            + "WHERE e.moduleCode = :moduleCode AND e.entityCode = :entityCode AND f.active = true "
            + "ORDER BY f.displayOrder ASC")
    List<MetaField> findActiveFor(String moduleCode, String entityCode);

    @EntityGraph(attributePaths = {"entity", "fieldType"})
    @Query("SELECT f FROM MetaField f JOIN f.entity e "
            + "WHERE e.moduleCode = :moduleCode AND e.entityCode = :entityCode AND f.code = :code")
    Optional<MetaField> findByTarget(String moduleCode, String entityCode, String code);
}
