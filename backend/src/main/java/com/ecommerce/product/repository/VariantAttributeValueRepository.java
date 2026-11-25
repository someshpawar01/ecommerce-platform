package com.ecommerce.product.repository;

import com.ecommerce.product.entity.VariantAttributeValue;
import com.ecommerce.product.entity.id.VariantAttributeValueId;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VariantAttributeValueRepository
    extends JpaRepository<VariantAttributeValue, VariantAttributeValueId> {

    @Query("""
            SELECT vav
            FROM VariantAttributeValue vav
            JOIN FETCH vav.attributeValue av
            JOIN FETCH vav.attribute a
            WHERE vav.variant.id IN :variantIds
            ORDER BY vav.variant.id, a.name, a.id
            """)
    List<VariantAttributeValue> findAllByVariantIdsWithDetails(
        @Param("variantIds") Collection<Long> variantIds);
}
