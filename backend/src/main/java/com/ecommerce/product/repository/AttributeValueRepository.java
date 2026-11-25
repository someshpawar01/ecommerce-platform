package com.ecommerce.product.repository;

import com.ecommerce.product.entity.AttributeValue;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttributeValueRepository extends JpaRepository<AttributeValue, Long> {

    List<AttributeValue> findByAttributeIdOrderByDisplayOrderAscValueAscIdAsc(Long attributeId);

    boolean existsByAttributeIdAndValueIgnoreCase(Long attributeId, String value);

    boolean existsByAttributeIdAndValueIgnoreCaseAndIdNot(Long attributeId, String value, Long id);

    @Query(value = """
            SELECT COUNT(*)
            FROM variant_attribute_values
            WHERE attribute_value_id = :valueId
            """, nativeQuery = true)
    long countVariantReferences(@Param("valueId") Long valueId);
}
