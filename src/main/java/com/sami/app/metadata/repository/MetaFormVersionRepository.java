package com.sami.app.metadata.repository;

import com.sami.app.metadata.domain.MetaFormVersion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MetaFormVersionRepository extends JpaRepository<MetaFormVersion, Long> {

    @EntityGraph(attributePaths = {"form"})
    List<MetaFormVersion> findByFormIdOrderByVersionNoDesc(Long formId);

    @EntityGraph(attributePaths = {"form"})
    Optional<MetaFormVersion> findFirstByFormIdAndStatusOrderByVersionNoDesc(Long formId, String status);

    @EntityGraph(attributePaths = {"form"})
    Optional<MetaFormVersion> findWithFormById(Long id);

    Optional<MetaFormVersion> findFirstByFormIdOrderByVersionNoDesc(Long formId);
}
