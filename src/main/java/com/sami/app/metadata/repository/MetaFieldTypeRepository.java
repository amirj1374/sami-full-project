package com.sami.app.metadata.repository;

import com.sami.app.metadata.domain.MetaFieldType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MetaFieldTypeRepository extends JpaRepository<MetaFieldType, Long> {

    List<MetaFieldType> findAllByOrderByDisplayOrderAsc();

    Optional<MetaFieldType> findByCode(String code);
}
