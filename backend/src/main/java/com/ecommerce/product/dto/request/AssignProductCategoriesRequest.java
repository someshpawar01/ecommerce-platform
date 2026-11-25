package com.ecommerce.product.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AssignProductCategoriesRequest(
    @NotNull List<@NotNull Long> categoryIds,
    Long primaryCategoryId
) {
}
