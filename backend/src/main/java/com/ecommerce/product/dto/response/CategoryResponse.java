package com.ecommerce.product.dto.response;

public record CategoryResponse(
    Long id,
    Long parentId,
    String name,
    String slug,
    String description,
    Integer sortOrder,
    Boolean active
) {
}
