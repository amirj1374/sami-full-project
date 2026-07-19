package com.sami.app.metadata.repository;

import com.sami.app.metadata.domain.MetaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MetaEntityRepository extends JpaRepository<MetaEntity, Long> {

    List<MetaEntity> findAllByOrderByDisplayOrderAsc();

    Optional<MetaEntity> findByModuleCodeAndEntityCode(String moduleCode, String entityCode);
}
