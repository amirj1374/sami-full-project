package com.sami.app.authz.repository;

import com.sami.app.authz.domain.AppModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data-access for {@link AppModule}. */
public interface AppModuleRepository extends JpaRepository<AppModule, Long> {

    boolean existsByCodeIgnoreCase(String code);

    /** Menu source: only enabled modules, in display order. */
    List<AppModule> findByEnabledTrueOrderByDisplayOrderAsc();

    /** Matrix source: every module (including disabled), in display order. */
    List<AppModule> findAllByOrderByDisplayOrderAsc();
}
