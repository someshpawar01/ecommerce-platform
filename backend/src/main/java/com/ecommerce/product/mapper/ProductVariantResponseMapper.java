package com.ecommerce.product.mapper;

import com.ecommerce.product.dto.response.ProductVariantResponse;
import com.ecommerce.product.dto.response.VariantAttributeResponse;
import com.ecommerce.product.entity.Attribute;
import com.ecommerce.product.entity.AttributeValue;
import com.ecommerce.product.entity.ProductVariant;
import com.ecommerce.product.entity.VariantAttributeValue;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProductVariantResponseMapper {

    public ProductVariantResponse toResponse(ProductVariant variant, List<VariantAttributeValue> assignments) {
        List<VariantAttributeResponse> attributes = assignments.stream()
            .map(assignment -> {
                Attribute attribute = assignment.getAttribute();
                AttributeValue value = assignment.getAttributeValue();

                return new VariantAttributeResponse(
                    attribute.getId(), attribute.getCode(), attribute.getName(),
                    value.getId(), value.getValue());
            })
            .toList();

        return new ProductVariantResponse(
            variant.getId(),
            variant.getProduct().getId(),
            variant.getSku(),
            variant.getVariantName(),
            variant.getPrice(),
            variant.getCompareAtPrice(),
            variant.getCurrencyCode(),
            variant.getActive(),
            variant.getWeightGrams(),
            variant.getVersion(),
            attributes
        );
    }
}
