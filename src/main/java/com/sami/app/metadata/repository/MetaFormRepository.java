package com.sami.app.metadata.repository;

import com.sami.app.metadata.domain.MetaForm;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MetaFormRepository extends JpaRepository<MetaForm, Long> {

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = {"entity"})
    Optional<MetaForm> findByCode(String code);

    @EntityGraph(attributePaths = {"entity"})
    List<MetaForm> findAllBy();
}
