package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Category;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Category c WHERE c.id = :id")
    Optional<Category> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT COALESCE(MAX(c.sortOrder), 0) FROM Category c WHERE c.parent IS NULL")
    Integer findMaxRootSortOrder();

    @Query("SELECT COALESCE(MAX(c.sortOrder), 0) FROM Category c WHERE c.parent.id = :parentId")
    Integer findMaxChildSortOrder(@Param("parentId") Long parentId);

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Category> findByParentIsNullAndActiveTrueOrderBySortOrderAscNameAscIdAsc();

    List<Category> findByParentIdAndActiveTrueOrderBySortOrderAscNameAscIdAsc(Long parentId);

    List<Category> findByActiveTrueOrderBySortOrderAscNameAsc();
}
