package com.ecommerce.product.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReorderCategoriesRequest(
    Long parentId,
    @NotNull List<@NotNull Long> categoryIds
) {
}
