package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Attribute;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttributeRepository extends JpaRepository<Attribute, Long> {

    boolean existsByCode(String code);

    List<Attribute> findAllByOrderByNameAscIdAsc();
}
