package com.ecommerce.product.dto.response;

import com.ecommerce.product.enums.ProductStatusEnum;
import java.time.LocalDateTime;
import java.util.List;

public record ProductResponse(
    Long id,
    String name,
    String slug,
    Long brandId,
    String brandName,
    String shortDescription,
    String description,
    ProductStatusEnum status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime publishedAt,
    Long version,
    List<ProductCategoryResponse> categories
) {
}
