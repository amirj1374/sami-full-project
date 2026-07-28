package com.sami.app.purchasing.repository;

import com.sami.app.purchasing.domain.PurWarehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data-access for the {@link PurWarehouse} reference lookup. */
public interface PurWarehouseRepository extends JpaRepository<PurWarehouse, Long> {

    List<PurWarehouse> findAllByOrderByDisplayOrderAsc();

    boolean existsByCodeIgnoreCase(String code);
}
