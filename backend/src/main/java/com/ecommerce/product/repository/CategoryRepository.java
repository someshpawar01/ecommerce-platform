package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Category;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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

    @Query("SELECT c FROM Category c WHERE c.parent IS NULL ORDER BY c.sortOrder, c.name, c.id")
    List<Category> findAllRootCategoriesForOrdering();

    @Query("SELECT c FROM Category c WHERE c.parent.id = :parentId ORDER BY c.sortOrder, c.name, c.id")
    List<Category> findAllChildrenForOrdering(@Param("parentId") Long parentId);
}
