package com.ecommerce.product.repository;

import com.ecommerce.product.entity.VariantAttributeValue;
import com.ecommerce.product.entity.id.VariantAttributeValueId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VariantAttributeValueRepository
    extends JpaRepository<VariantAttributeValue, VariantAttributeValueId> {

    List<VariantAttributeValue> findByIdVariantId(Long variantId);
}
