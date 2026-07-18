package com.sami.app.crm.repository;

import com.sami.app.crm.domain.CustomerTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data-access for the dynamic {@link CustomerTag} lookup. */
public interface CustomerTagRepository extends JpaRepository<CustomerTag, Long> {

    List<CustomerTag> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
