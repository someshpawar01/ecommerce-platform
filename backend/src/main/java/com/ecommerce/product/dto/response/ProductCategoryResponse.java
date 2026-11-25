package com.ecommerce.product.dto.response;

public record ProductCategoryResponse(
    Long categoryId,
    String categoryName,
    String categorySlug,
    boolean primary
) {
}
