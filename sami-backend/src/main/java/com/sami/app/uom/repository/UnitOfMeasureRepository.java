package com.sami.app.uom.repository;
import com.sami.app.uom.domain.UnitOfMeasure; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface UnitOfMeasureRepository extends JpaRepository<UnitOfMeasure,Long>{List<UnitOfMeasure> findAllByTenantIdOrderByCode(Long t); Optional<UnitOfMeasure> findByTenantIdAndId(Long t,Long id); boolean existsByTenantIdAndCode(Long t,String code);}
