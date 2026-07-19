package com.sami.app.metadata.repository;

import com.sami.app.metadata.domain.MetaFieldTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MetaFieldTranslationRepository extends JpaRepository<MetaFieldTranslation, Long> {

    List<MetaFieldTranslation> findByFieldId(Long fieldId);

    Optional<MetaFieldTranslation> findByFieldIdAndLocale(Long fieldId, String locale);
}
