package com.ecommerce.product.dto.response;

public record VariantAttributeResponse(
    Long attributeId,
    String attributeCode,
    String attributeName,
    Long attributeValueId,
    String value
) {
}
