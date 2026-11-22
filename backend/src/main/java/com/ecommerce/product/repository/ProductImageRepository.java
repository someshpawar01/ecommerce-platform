package com.ecommerce.product.repository;

import com.ecommerce.product.entity.ProductImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdOrderBySortOrderAscIdAsc(Long productId);

    List<ProductImage> findByVariantIdOrderBySortOrderAscIdAsc(Long variantId);
}
