package com.ecommerce.identity.dto.projection;

public record LoginCredentialsProjection(
    Long id,
    String passwordHash
) {
}
