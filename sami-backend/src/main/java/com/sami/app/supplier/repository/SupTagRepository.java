package com.sami.app.supplier.repository;

import com.sami.app.supplier.domain.SupTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data-access for {@link SupTag}. */
public interface SupTagRepository extends JpaRepository<SupTag, Long> {

    List<SupTag> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
