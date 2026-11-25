package com.ecommerce.product.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateProductVariantRequest(
    @Size(max = 100) String sku,
    @Size(max = 255) String variantName,
    @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
    @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal compareAtPrice,
    @Positive Integer weightGrams
) {
}
