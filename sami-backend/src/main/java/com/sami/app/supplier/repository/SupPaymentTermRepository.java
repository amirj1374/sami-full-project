package com.sami.app.supplier.repository;

import com.sami.app.supplier.domain.SupPaymentTerm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data-access for the configurable {@link SupPaymentTerm} lookup. */
public interface SupPaymentTermRepository extends JpaRepository<SupPaymentTerm, Long> {

    List<SupPaymentTerm> findAllByOrderByDisplayOrderAsc();

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
