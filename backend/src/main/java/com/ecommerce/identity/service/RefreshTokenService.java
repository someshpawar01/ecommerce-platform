package com.ecommerce.identity.service;

import com.ecommerce.identity.dto.response.RefreshTokenResponse;

public interface RefreshTokenService {

    RefreshTokenResult refresh(String rawRefreshToken);

    //by default it is public static and if imported then can directly use w/o
    //interface reference.
    record RefreshTokenResult(
        RefreshTokenResponse response,
        String refreshToken,
        long refreshCookieMaxAgeSeconds
    ) {
    }
}
