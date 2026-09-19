package com.sami.app.uom.repository;
import com.sami.app.uom.domain.UomConversion; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface UomConversionRepository extends JpaRepository<UomConversion,Long>{List<UomConversion> findAllByTenantIdOrderById(Long t); Optional<UomConversion> findByTenantIdAndId(Long t,Long id); Optional<UomConversion> findByTenantIdAndFromUomIdAndToUomId(Long t,Long f,Long to);}
