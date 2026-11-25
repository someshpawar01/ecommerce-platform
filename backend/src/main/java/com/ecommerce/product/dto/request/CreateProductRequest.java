package com.ecommerce.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProductRequest(
    @NotBlank @Size(max = 255) String name,
    Long brandId,
    @Size(max = 500) String shortDescription,
    String description
) {
}
