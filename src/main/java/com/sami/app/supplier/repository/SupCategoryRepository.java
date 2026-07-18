package com.sami.app.supplier.repository;

import com.sami.app.supplier.domain.SupCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data-access for {@link SupCategory}. */
public interface SupCategoryRepository extends JpaRepository<SupCategory, Long> {

    List<SupCategory> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
