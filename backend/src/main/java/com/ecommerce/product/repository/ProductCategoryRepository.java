package com.ecommerce.product.repository;

import com.ecommerce.product.entity.ProductCategory;
import com.ecommerce.product.entity.id.ProductCategoryId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, ProductCategoryId> {

    List<ProductCategory> findByIdProductIdOrderByIdCategoryIdAsc(Long productId);

    Optional<ProductCategory> findByIdProductIdAndPrimaryTrue(Long productId);

    @Query("""
        SELECT pc
        FROM ProductCategory pc
        JOIN FETCH pc.category
        WHERE pc.product.id = :productId
        ORDER BY pc.category.name, pc.category.id
        """)
    List<ProductCategory> findAllByProductIdWithCategory(@Param("productId") Long productId);

    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query("DELETE FROM ProductCategory pc WHERE pc.product.id = :productId")
    int deleteAllByProductId(@Param("productId") Long productId);
}
