package com.sami.app.product.repository;

import com.sami.app.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * Data-access for {@link Product}.
 *
 * <p>{@link JpaSpecificationExecutor} enables dynamic, composable filtering
 * (see {@code ProductSpecifications}) without hand-writing query permutations.
 */
public interface ProductRepository
        extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);
}
