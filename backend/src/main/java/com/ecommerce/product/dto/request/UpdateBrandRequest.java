package com.ecommerce.product.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateBrandRequest(
    @Size(max = 150) String name,
    @Size(max = 1000) String description
) {
}
