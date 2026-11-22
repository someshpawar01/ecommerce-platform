package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Attribute;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttributeRepository extends JpaRepository<Attribute, Long> {

    Optional<Attribute> findByCode(String code);

    boolean existsByCode(String code);
}
