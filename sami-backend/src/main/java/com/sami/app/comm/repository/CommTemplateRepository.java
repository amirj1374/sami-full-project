package com.sami.app.comm.repository;

import com.sami.app.comm.domain.CommTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CommTemplateRepository extends JpaRepository<CommTemplate, Long> {
    Optional<CommTemplate> findByCodeAndLanguageAndIsActiveTrue(String code, String language);
    List<CommTemplate> findByCodeOrderByRevisionDesc(String code);
    List<CommTemplate> findByIsActiveTrueOrderByDisplayOrderAsc();
}
