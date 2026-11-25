package com.ecommerce.product.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ProductVariantResponse(
    Long id,
    Long productId,
    String sku,
    String variantName,
    BigDecimal price,
    BigDecimal compareAtPrice,
    String currencyCode,
    Boolean active,
    Integer weightGrams,
    Long version,
    List<VariantAttributeResponse> attributes
) {
}
