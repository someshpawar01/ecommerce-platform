package com.ecommerce.identity.service;

import com.ecommerce.identity.dto.request.LoginRequest;
import com.ecommerce.identity.dto.response.LoginResponse;

public interface AuthService {

    LoginResult login(LoginRequest request);

    record LoginResult(
        LoginResponse response,
        String refreshToken,
        long refreshCookieMaxAgeSeconds
    ) {
    }
}
