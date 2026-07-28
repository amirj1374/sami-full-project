package com.sami.app.licensing.repository;

import com.sami.app.licensing.domain.Tenant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long>, JpaSpecificationExecutor<Tenant> {

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = {"status"})
    java.util.List<Tenant> findAllBy();

    @EntityGraph(attributePaths = {"status"})
    Optional<Tenant> findByCode(String code);

    @EntityGraph(attributePaths = {"status"})
    Optional<Tenant> findWithStatusById(Long id);
}
