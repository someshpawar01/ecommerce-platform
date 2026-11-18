package com.ecommerce.identity.service;

import com.ecommerce.identity.dto.response.CurrentUserResponse;

public interface CurrentUserService {

    CurrentUserResponse getCurrentUser(Long userId);
}
