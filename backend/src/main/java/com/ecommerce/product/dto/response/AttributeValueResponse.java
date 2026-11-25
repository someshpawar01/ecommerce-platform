package com.ecommerce.product.dto.response;

public record AttributeValueResponse(
    Long id,
    Long attributeId,
    String value,
    Integer displayOrder
) {
}
