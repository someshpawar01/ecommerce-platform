package com.ecommerce.customer.repository;

import com.ecommerce.customer.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {

    List<CustomerAddress> findByCustomerProfileUserIdOrderByCreatedAtDesc(Long customerUserId);

    Optional<CustomerAddress> findByIdAndCustomerProfileUserId(Long addressId, Long customerUserId);

    Optional<CustomerAddress> findByCustomerProfileUserIdAndIsDefaultTrue(Long customerUserId);

    boolean existsByCustomerProfileUserId(Long customerUserId);

    long countByCustomerProfileUserId(Long customerUserId);

    Optional<CustomerAddress> findFirstByCustomerProfileUserIdAndIdNotOrderByIdAsc(Long customerUserId, Long addressId);
}
