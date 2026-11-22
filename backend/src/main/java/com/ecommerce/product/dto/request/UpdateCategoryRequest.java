package com.ecommerce.product.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
    @Size(max = 150) String name,

    @Size(max = 1000) String description,

    @Min(0) Integer sortOrder
) {
}
