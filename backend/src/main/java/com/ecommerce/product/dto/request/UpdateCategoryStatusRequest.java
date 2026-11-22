package com.ecommerce.product.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateCategoryStatusRequest(@NotNull Boolean active) {
}
