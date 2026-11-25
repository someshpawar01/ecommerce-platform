package com.ecommerce.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAttributeRequest(
    @NotBlank @Size(max = 100) String name
) {
}
