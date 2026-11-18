package com.ecommerce.identity.dto.response;

public record RefreshTokenResponse(
    String accessToken,
    String tokenType,
    long expiresIn
) {
}
