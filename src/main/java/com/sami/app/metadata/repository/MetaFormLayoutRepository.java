package com.sami.app.metadata.repository;

import com.sami.app.metadata.domain.MetaFormLayout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MetaFormLayoutRepository extends JpaRepository<MetaFormLayout, Long> {

    List<MetaFormLayout> findByFormVersionIdOrderByPriorityAsc(Long formVersionId);
}
