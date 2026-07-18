package com.sami.app.crm.repository;

import com.sami.app.crm.domain.CustomerSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data-access for the configurable {@link CustomerSource} lookup. */
public interface CustomerSourceRepository extends JpaRepository<CustomerSource, Long> {

    List<CustomerSource> findAllByOrderByDisplayOrderAsc();

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
