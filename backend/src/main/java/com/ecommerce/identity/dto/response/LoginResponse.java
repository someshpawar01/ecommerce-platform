package com.ecommerce.identity.dto.response;

import java.util.List;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    UserInfo user
) {

    public record UserInfo(
        Long id,
        String email,
        List<String> roles
    ) {
    }
}
