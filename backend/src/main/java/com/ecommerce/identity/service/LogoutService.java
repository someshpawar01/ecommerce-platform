package com.ecommerce.identity.service;

public interface LogoutService {

    void logout(String rawRefreshToken);

    void logoutAll(Long userId);
}
