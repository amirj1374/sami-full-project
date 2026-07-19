package com.sami.app.metadata.repository;

import com.sami.app.metadata.domain.MetaFieldValue;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface MetaFieldValueRepository extends JpaRepository<MetaFieldValue, Long> {

    @EntityGraph(attributePaths = {"field", "field.fieldType"})
    List<MetaFieldValue> findByModuleCodeAndEntityCodeAndRecordId(
            String moduleCode, String entityCode, Long recordId);

    Optional<MetaFieldValue> findByFieldIdAndRecordId(Long fieldId, Long recordId);

    /** Search: record ids whose custom text field matches (case-insensitive contains). */
    @Query("SELECT v.recordId FROM MetaFieldValue v WHERE v.field.id = :fieldId "
            + "AND LOWER(v.valueText) LIKE LOWER(CONCAT('%', :value, '%'))")
    List<Long> searchByText(Long fieldId, String value);

    /** Filter: record ids whose custom numeric field falls in a range. */
    @Query("SELECT v.recordId FROM MetaFieldValue v WHERE v.field.id = :fieldId "
            + "AND (:min IS NULL OR v.valueNumber >= :min) AND (:max IS NULL OR v.valueNumber <= :max)")
    List<Long> searchByNumberRange(Long fieldId, BigDecimal min, BigDecimal max);

    void deleteByModuleCodeAndEntityCodeAndRecordId(String moduleCode, String entityCode, Long recordId);
}
