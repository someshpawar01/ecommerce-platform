package com.ecommerce.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAttributeValueRequest(
    @NotBlank @Size(max = 150) String value
) {
}
