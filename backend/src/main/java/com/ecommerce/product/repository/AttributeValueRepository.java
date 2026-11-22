package com.ecommerce.product.repository;

import com.ecommerce.product.entity.AttributeValue;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttributeValueRepository extends JpaRepository<AttributeValue, Long> {

    List<AttributeValue> findByAttributeIdOrderByDisplayOrderAscIdAsc(Long attributeId);

    boolean existsByAttributeIdAndValue(Long attributeId, String value);
}
