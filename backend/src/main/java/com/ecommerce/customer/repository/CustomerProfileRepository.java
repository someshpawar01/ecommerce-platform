package com.ecommerce.customer.repository;

import com.ecommerce.customer.entity.CustomerProfile;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cp FROM CustomerProfile cp WHERE cp.userId = :userId")
    Optional<CustomerProfile> findByUserIdForUpdate(@Param("userId") Long userId);
}
