package com.ecommerce.product.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateBrandStatusRequest(@NotNull Boolean active) {
}
