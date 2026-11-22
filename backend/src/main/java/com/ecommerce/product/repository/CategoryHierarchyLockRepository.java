package com.ecommerce.product.repository;

import com.ecommerce.product.entity.CategoryHierarchyLock;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryHierarchyLockRepository extends JpaRepository<CategoryHierarchyLock, Short> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM CategoryHierarchyLock l WHERE l.id = :id")
    Optional<CategoryHierarchyLock> findByIdForUpdate(@Param("id") Byte id);
}
