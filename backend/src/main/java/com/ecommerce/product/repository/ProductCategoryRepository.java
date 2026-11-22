package com.ecommerce.product.repository;

import com.ecommerce.product.entity.ProductCategory;
import com.ecommerce.product.entity.id.ProductCategoryId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, ProductCategoryId> {

    List<ProductCategory> findByIdProductIdOrderByIdCategoryIdAsc(Long productId);

    Optional<ProductCategory> findByIdProductIdAndPrimaryTrue(Long productId);
}
