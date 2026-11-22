package com.ecommerce.product.dto.response;

public record BrandResponse(
    Long id,
    String name,
    String slug,
    String description,
    Boolean active
) {
}
