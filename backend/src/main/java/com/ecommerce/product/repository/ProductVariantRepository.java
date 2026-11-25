package com.ecommerce.product.repository;

import com.ecommerce.product.entity.ProductVariant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    List<ProductVariant> findByProductIdOrderByIdAsc(Long productId);

    Optional<ProductVariant> findByIdAndProductId(Long id, Long productId);
}
